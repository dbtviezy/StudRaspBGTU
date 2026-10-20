"use client";

import { useEffect, useMemo, useState } from "react";
import { isoOf, type ISODate } from "@/lib/dates";
import { APP_VERSION, copyOf, type Copy } from "@/lib/i18n";
import { type Bundle, changeId, type Entry, type Filters, type Lesson, loadBundle } from "@/lib/schedule";
import { type Data, getState, resetData, setState, useStore } from "@/lib/store";
import { applyThemeAttr } from "@/lib/theme";
import { useNow } from "@/lib/useNow";
import { ShellContext, type Shell, type TodoPreset } from "./context";
import { Icon, type IconName } from "./Icons";
import Splash from "./Splash";
import { useToast } from "./ui";
import Onboarding from "./screens/Onboarding";
import GroupPicker from "./screens/GroupPicker";
import HomeScreen from "./screens/Home";
import ScheduleScreen from "./screens/Schedule";
import GroupsScreen from "./screens/Groups";
import CalendarScreen from "./screens/Calendar";
import ProfileScreen from "./screens/Profile";
import ChatScreen from "./screens/Chat";
import { forgetAccount, useAccount, useAccountRefresh } from "@/lib/account";
import { cachedChats, clearChatCache, useChatVersion, useOutboxPump } from "@/lib/chat";
import { disablePush } from "@/lib/push";
import LessonDialog from "./overlays/LessonDialog";
import TeacherDialog from "./overlays/TeacherDialog";
import TasksOverlay, { TodoEditor } from "./overlays/Tasks";
import FilesOverlay from "./overlays/Files";
import NotesOverlay from "./overlays/Notes";
import InstallHint from "./InstallHint";
import WhatsNew from "./overlays/WhatsNew";

function useServiceWorker() {
  useEffect(() => {
    if (process.env.NODE_ENV !== "production" || !("serviceWorker" in navigator)) return;
    navigator.serviceWorker.register("/sw.js", { scope: "/", updateViaCache: "none" }).catch(() => undefined);
  }, []);
}

export default function App() {
  const data = useStore();
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [failed, setFailed] = useState(false);
  const [splash, setSplash] = useState(true);
  const [attempt, setAttempt] = useState(0);
  const [picking, setPicking] = useState(() => !getState().group);
  const [pickingFriend, setPickingFriend] = useState(false);
  const s = copyOf(data.lang);

  useServiceWorker();

  useEffect(() => {
    let alive = true;
    const started = Date.now();
    loadBundle()
      .then((loaded) => alive && setBundle(loaded))
      .catch(() => alive && setFailed(true))
      .finally(() => {
        const wait = Math.max(0, 800 - (Date.now() - started));
        window.setTimeout(() => alive && setSplash(false), wait);
      });
    return () => {
      alive = false;
    };
  }, [attempt]);

  useEffect(() => applyThemeAttr(data.dark), [data.dark]);
  useEffect(() => {
    if (!data.systemTheme) return;
    const query = window.matchMedia("(prefers-color-scheme: dark)");
    const sync = () => setState({ dark: query.matches });
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, [data.systemTheme]);
  useEffect(() => {
    document.documentElement.lang = data.lang ?? "ru";
    document.title = s.appName;
  }, [data.lang, s.appName]);

  const setLanguage = (code: "ru" | "en") => setState({ lang: code });

  if (splash) return <Splash />;
  if (failed || !bundle) {
    return (
      <div className="fullscreen" style={{ justifyContent: "center", padding: 28 }}>
        <h1 className="h1" style={{ fontSize: 24 }}>{s.loadFailedTitle}</h1>
        <p className="muted" style={{ fontSize: 15 }}>{s.loadFailedText}</p>
        <button
          className="btn mt16"
          onClick={() => {
            setFailed(false);
            setSplash(true);
            setAttempt((n) => n + 1);
          }}
        >
          {s.retry}
        </button>
      </div>
    );
  }
  if (!data.onboarded) {
    return (
      <Onboarding
        s={s}
        lang={data.lang}
        dark={data.dark}
        onLanguage={setLanguage}
        onStart={() => setState({ onboarded: true, lang: data.lang ?? "ru", seenVersion: APP_VERSION })}
      />
    );
  }
  const found = bundle.find(data.group);
  if (pickingFriend && found) {
    return (
      <GroupPicker
        s={s}
        bundle={bundle}
        initialName=""
        selectedId={data.friend}
        askName={false}
        onBack={() => setPickingFriend(false)}
        onConfirm={(id) => {
          setState({ friend: id });
          setPickingFriend(false);
        }}
      />
    );
  }
  if (picking || !found) {
    return (
      <GroupPicker
        s={s}
        bundle={bundle}
        initialName={data.name}
        selectedId={data.group}
        onBack={found ? () => setPicking(false) : null}
        onConfirm={(id, name) => {
          setState({ group: id, name: name.trim() });
          setPicking(false);
        }}
      />
    );
  }
  return (
    <MainShell
      bundle={bundle}
      entry={found}
      s={s}
      data={data}
      setLanguage={setLanguage}
      pickGroup={() => setPicking(true)}
      pickFriend={() => setPickingFriend(true)}
      onReset={() => {
        disablePush();
        resetData();
        forgetAccount();
        clearChatCache();
        setPickingFriend(false);
        setPicking(true);
      }}
    />
  );
}

interface MainProps {
  bundle: Bundle;
  entry: Entry;
  s: Copy;
  data: Data;
  setLanguage: (code: "ru" | "en") => void;
  pickGroup: () => void;
  pickFriend: () => void;
  onReset: () => void;
}

const TABS: { icon: IconName; label: (s: Copy) => string }[] = [
  { icon: "home", label: (s) => s.tabHome },
  { icon: "viewDay", label: (s) => s.tabSchedule },
  { icon: "groups", label: (s) => s.tabGroups },
  { icon: "calendar", label: (s) => s.tabCalendar },
  { icon: "chat", label: (s) => s.tabChat },
  { icon: "person", label: (s) => s.tabProfile },
];

function MainShell({ bundle, entry, s, data, setLanguage, pickGroup, pickFriend, onReset }: MainProps) {
  const now = useNow();
  const today = isoOf(now);
  const [tab, setTabState] = useState(() => (new URLSearchParams(window.location.search).has("chat") ? 4 : 0));
  const [back, setBack] = useState(false);
  // null follows "today", so the selection rolls over at midnight on its own.
  const [pickedDate, setPickedDate] = useState<ISODate | null>(null);
  const [filter, setFilter] = useState("auto");
  const [opened, setOpened] = useState<{ lesson: Lesson; date: ISODate } | null>(null);
  const [teacher, setTeacher] = useState<string | null>(null);
  const [tasks, setTasks] = useState(false);
  const [files, setFiles] = useState(false);
  const [reminders, setReminders] = useState(false);
  const [todo, setTodo] = useState<TodoPreset | null>(null);
  const [toastText, toast] = useToast();
  const selectedDate = pickedDate ?? today;
  const { user } = useAccount();
  useAccountRefresh();
  useOutboxPump(user != null);
  useChatVersion();
  const unread = user != null && cachedChats().some((chat) => chat.unread > 0 || !chat.accepted);

  const filters = useMemo<Filters>(
    () => ({
      hidden: new Set(data.hidden),
      changes: new Map(data.dayChanges.map((c) => [changeId(c.date, c.key), c])),
    }),
    [data.hidden, data.dayChanges],
  );

  const setTab = (next: number) => {
    setBack(next < tab);
    setTabState(next);
    window.scrollTo(0, 0);
  };

  const shell: Shell = {
    bundle,
    s,
    data,
    now,
    today,
    faculty: entry.faculty,
    group: entry.group,
    filters,
    selectedDate,
    setSelectedDate: (date) => setPickedDate(date === today ? null : date),
    filter,
    setFilter,
    setTab,
    openLesson: (lesson, date) => {
      setOpened({ lesson, date });
      setPickedDate(date === today ? null : date);
    },
    openTeacher: setTeacher,
    openTasks: () => setTasks(true),
    openFiles: () => setFiles(true),
    openReminders: () => setReminders(true),
    editTodo: setTodo,
    toast,
    setLanguage,
    pickGroup,
    pickFriend,
    onReset,
  };

  const screens = [HomeScreen, ScheduleScreen, GroupsScreen, CalendarScreen, ChatScreen, ProfileScreen];
  const Screen = screens[tab];

  return (
    <ShellContext.Provider value={shell}>
      <div className="app">
        <div key={tab} className={`screen${back ? " back" : ""}`}>
          <Screen />
        </div>
      </div>
      <nav className="tabbar">
        <div className="tabbar-inner">
          {TABS.map((item, index) => (
            <button key={item.icon} className={`tab${index === tab ? " on" : ""}`} onClick={() => setTab(index)} aria-current={index === tab ? "page" : undefined}>
              <span className="well">
                <Icon name={item.icon} size={22} />
                {item.icon === "chat" && unread && <span className="dot" />}
              </span>
              <span className="label">{item.label(s)}</span>
            </button>
          ))}
        </div>
      </nav>
      {reminders && <NotesOverlay onClose={() => setReminders(false)} />}
      {files && <FilesOverlay onClose={() => setFiles(false)} />}
      {tasks && <TasksOverlay onClose={() => setTasks(false)} />}
      {teacher && <TeacherDialog name={teacher} onClose={() => setTeacher(null)} />}
      {opened && <LessonDialog key={`${opened.date}|${opened.lesson.key}`} lesson={opened.lesson} date={opened.date} onClose={() => setOpened(null)} />}
      {todo && <TodoEditor preset={todo} onClose={() => setTodo(null)} />}
      <InstallHint s={s} />
      {data.seenVersion !== APP_VERSION && <WhatsNew s={s} onClose={() => setState({ seenVersion: APP_VERSION })} />}
      {toastText && <div className="toast" role="status">{toastText}</div>}
    </ShellContext.Provider>
  );
}
