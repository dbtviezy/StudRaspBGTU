"use client";

import { useEffect, useMemo, useState } from "react";
import { useAccount } from "@/lib/account";
import { cachedChats, cachedGroupTasks, refreshGroupTasks, useChatVersion } from "@/lib/chat";
import type { Copy } from "@/lib/i18n";
import type { Lesson } from "@/lib/schedule";
import { actions, isOverdue, newId, sortedTodos, type TodoItem } from "@/lib/store";
import { type TodoPreset, useShell } from "../context";
import { Icon } from "../Icons";
import { Dialog, Overlay, OverlayHeader } from "../ui";
import { deadlineText } from "../screens/Home";
import { GroupTaskRow } from "./ChatTools";

export const lessonLabelOf = (s: Copy, lesson: Lesson) => [s.dayName(lesson.dayName), lesson.start, lesson.subject].filter(Boolean).join(" · ");

function TaskRow({ item }: { item: TodoItem }) {
  const { s, now, editTodo } = useShell();
  const overdue = isOverdue(item, now);
  return (
    <div className="task-row card" style={{ padding: "10px 14px" }}>
      <input type="checkbox" checked={item.done} aria-label={item.title} onChange={(e) => actions.upsertTodo({ ...item, done: e.target.checked })} />
      <button className="grow" style={{ textAlign: "left" }} onClick={() => editTodo({ initial: item })}>
        <div className={item.done ? "done-text" : "semibold"} style={{ overflowWrap: "anywhere" }}>{item.title}</div>
        <div className={`xsmall mt2 ${overdue ? "red" : "muted"}`}>{overdue ? `${s.overdue} · ${deadlineText(s, item)}` : deadlineText(s, item)}</div>
        {item.lessonLabel && <div className="blue xsmall mt2 ellipsis">{item.lessonLabel}</div>}
      </button>
    </div>
  );
}

export default function TasksOverlay({ onClose }: { onClose: () => void }) {
  const { s, data, editTodo, today } = useShell();
  const { user } = useAccount();
  useChatVersion();
  const all = sortedTodos(data.todos);
  const open = all.filter((t) => !t.done);
  const done = all.filter((t) => t.done);
  const chats = user ? cachedChats() : [];
  const group = user ? cachedGroupTasks().filter((t) => !t.dueDate || t.dueDate >= today) : [];

  useEffect(() => {
    if (user) refreshGroupTasks().catch(() => undefined);
  }, [user]);

  return (
    <Overlay onClose={onClose}>
      <OverlayHeader title={s.allTasks} onClose={onClose} closeLabel={s.close} />
      <div className="muted small">{s.tasksHint}</div>
      <button className="btn compact mt12" onClick={() => editTodo({})}>{s.addTask}</button>
      {all.length === 0 && <div className="muted mt16" style={{ textAlign: "center" }}>{s.tasksEmpty}</div>}
      {open.length > 0 && <h3 className="h3 mt16">{s.openTasks}</h3>}
      <div className="stack tight mt8">
        {open.map((item) => <TaskRow key={item.id} item={item} />)}
      </div>
      {done.length > 0 && <h3 className="h3 mt16">{s.completed}</h3>}
      <div className="stack tight mt8">
        {done.map((item) => <TaskRow key={item.id} item={item} />)}
      </div>
      {group.length > 0 && <h3 className="h3 mt16">{s.groupTasks}</h3>}
      {group.map((task) => (
        <GroupTaskRow key={task.id} task={task} chatTitle={chats.find((c) => c.id === task.chatId)?.title} />
      ))}
    </Overlay>
  );
}

export function LessonChooser({ onPick, onClose }: { onPick: (lesson: Lesson) => void; onClose: () => void }) {
  const { s, group } = useShell();
  const [query, setQuery] = useState("");
  const lessons = useMemo(() => {
    const seen = new Set<string>();
    return [...group.lessons]
      .sort((a, b) => a.day - b.day || a.start.localeCompare(b.start))
      .filter((l) => (seen.has(l.key) ? false : (seen.add(l.key), true)));
  }, [group]);
  const q = query.trim().toLowerCase();
  const shown = q ? lessons.filter((l) => lessonLabelOf(s, l).toLowerCase().includes(q)) : lessons;
  return (
    <Dialog onClose={onClose}>
      <h2 className="h2">{s.chooseLesson}</h2>
      <label className="search mt8" style={{ display: "block" }}>
        <Icon name="search" />
        <input className="input" type="search" value={query} placeholder={s.subjectOrDay} onChange={(e) => setQuery(e.target.value)} autoComplete="off" />
      </label>
      <div className="mt8" style={{ maxHeight: "50dvh", overflowY: "auto" }}>
        {shown.map((lesson) => (
          <button key={lesson.key} style={{ display: "block", width: "100%", textAlign: "left", padding: "10px 4px", borderBottom: "1px solid var(--line)" }} onClick={() => onPick(lesson)}>
            <div className="small">{lessonLabelOf(s, lesson)}</div>
            <div className="muted xsmall">{[s.lessonType(lesson.type), s.roomName(lesson.room), s.weekBadge(lesson.week)].filter(Boolean).join(" · ")}</div>
          </button>
        ))}
        {shown.length === 0 && <div className="muted small" style={{ padding: 8 }}>{s.nothingFound}</div>}
      </div>
      <div className="dialog-actions">
        <button className="tbtn muted" onClick={onClose}>{s.cancel}</button>
      </div>
    </Dialog>
  );
}

export function TodoEditor({ preset, onClose }: { preset: TodoPreset; onClose: () => void }) {
  const { s, today } = useShell();
  const initial = preset.initial ?? null;
  const [title, setTitle] = useState(initial?.title ?? "");
  const [date, setDate] = useState(initial?.deadlineDate ?? preset.date ?? "");
  const [time, setTime] = useState(initial?.deadlineTime ?? preset.time ?? "");
  const [lessonKey, setLessonKey] = useState(initial?.lessonKey ?? preset.lesson?.key ?? null);
  const [lessonLabel, setLessonLabel] = useState(initial?.lessonLabel ?? (preset.lesson ? lessonLabelOf(s, preset.lesson) : null));
  const [choosing, setChoosing] = useState(false);
  const validTime = !time || /^\d{2}:\d{2}$/.test(time);

  const save = () => {
    if (!title.trim() || !validTime) return;
    actions.upsertTodo({
      id: initial?.id ?? newId(),
      title: title.trim(),
      deadlineDate: date || null,
      deadlineTime: date && time ? time : null,
      lessonKey,
      lessonLabel: lessonKey ? lessonLabel : null,
      done: initial?.done ?? false,
      remind: initial?.remind ?? "",
    });
    onClose();
  };

  return (
    <Dialog onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 22 }}>{initial ? s.editTask : s.newTask}</h2>
      <label className="field mt12">
        <span>{s.taskTitle}</span>
        <input className="input" value={title} maxLength={200} placeholder={s.taskTitleHint} onChange={(e) => setTitle(e.target.value)} autoFocus={!initial} />
      </label>
      <label className="field mt12">
        <span>{s.deadline}</span>
        <input className="input" type="date" value={date} min={initial ? undefined : today} onChange={(e) => setDate(e.target.value)} />
      </label>
      {date && (
        <>
          <button className="tbtn flush" onClick={() => setDate("")}>{s.clearDeadline}</button>
          <label className="field mt4">
            <span>{s.timeOptional}</span>
            <input className={`input${validTime ? "" : " error"}`} type="time" value={time} onChange={(e) => setTime(e.target.value)} />
          </label>
          {!validTime && <div className="red xsmall mt2">{s.badTime}</div>}
        </>
      )}
      <div className="field mt12">
        <span>{s.attachLessonOptional}</span>
        <div className="row">
          <div className={`grow small ${lessonKey ? "" : "muted"}`}>{lessonKey ? lessonLabel : s.notAttached}</div>
          {lessonKey ? (
            <button
              className="tbtn muted"
              onClick={() => {
                setLessonKey(null);
                setLessonLabel(null);
              }}
            >
              {s.removeLesson}
            </button>
          ) : (
            <button className="tbtn" onClick={() => setChoosing(true)}>{s.chooseLesson}</button>
          )}
        </div>
      </div>
      <div className="dialog-actions">
        {initial && (
          <button
            className="tbtn danger"
            style={{ marginRight: "auto" }}
            onClick={() => {
              actions.removeTodo(initial.id);
              onClose();
            }}
          >
            {s.delete}
          </button>
        )}
        <button className="tbtn muted" onClick={onClose}>{s.cancel}</button>
        <button className="tbtn" disabled={!title.trim() || !validTime} onClick={save}>{s.save}</button>
      </div>
      {choosing && (
        <LessonChooser
          onClose={() => setChoosing(false)}
          onPick={(lesson) => {
            setLessonKey(lesson.key);
            setLessonLabel(lessonLabelOf(s, lesson));
            setChoosing(false);
          }}
        />
      )}
    </Dialog>
  );
}
