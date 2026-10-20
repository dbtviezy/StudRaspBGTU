"use client";

import { useState } from "react";
import { addDays } from "@/lib/dates";
import type { Copy } from "@/lib/i18n";
import {
  dayRows,
  liveStatus,
  type LiveStatus,
  nextTeacherLessons,
  sharedFree,
  shownLessons,
  type TeacherNext,
  upcomingLessons,
  weekInfo,
} from "@/lib/schedule";
import { isOverdue, sortedTodos, type TodoItem } from "@/lib/store";
import { originOf, switchTheme } from "@/lib/theme";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { GapRow, ScheduleLesson } from "../LessonCard";
import { Announcements } from "../Announcements";
import { Appear, LanguageMark, Mark } from "../ui";

export function liveLine(s: Copy, status: LiveStatus): string {
  const join = (parts: string[]) => parts.filter(Boolean).join(" · ");
  switch (status.kind) {
    case "off":
      return s.code === "en" ? status.holiday.nameEn : status.holiday.nameRu;
    case "inClass":
      return join([s.liveInClass, status.lesson.subject, s.roomName(status.lesson.room), s.leftPhrase(status.minutesLeft)]);
    case "soon":
      return join([`${s.liveSoon} ${s.untilPhrase(status.minutesUntil)}`, status.lesson.subject, s.roomName(status.lesson.room)]);
    case "window":
      return join([`${s.liveWindow} ${s.span(status.minutes)}`, status.next.subject, status.next.start]);
    case "done":
      return status.next ? `${s.liveDone}. ${s.liveNext} ${status.next.lesson.start} · ${status.next.lesson.subject}` : s.liveDone;
    default:
      return s.liveNone;
  }
}

export function teacherFollowLine(s: Copy, today: string, item: TeacherNext): string {
  const place = [item.lesson.subject, item.groupId, s.roomName(item.lesson.room)].filter(Boolean).join(" · ");
  return `${s.whenLabel(item.day, today)} · ${item.lesson.start} · ${place}`;
}

export function deadlineText(s: Copy, item: TodoItem): string {
  if (!item.deadlineDate || !/^\d{4}-\d{2}-\d{2}$/.test(item.deadlineDate)) return s.noDeadline;
  const base = s.formatLongDateCap(item.deadlineDate);
  return item.deadlineTime ? `${base} · ${item.deadlineTime}` : base;
}

function TaskPreview({ item }: { item: TodoItem }) {
  const { s } = useShell();
  return (
    <>
      <div className="ellipsis mt6" style={{ fontSize: 14 }}>{item.title}</div>
      <div className="muted xsmall">{deadlineText(s, item)}</div>
    </>
  );
}

export default function HomeScreen() {
  const { bundle, s, data, now, today, faculty, group, filters, setTab, openTasks, openReminders, openTeacher, editTodo, setLanguage } = useShell();
  const [tasksExpanded, setTasksExpanded] = useState(false);
  const tomorrow = addDays(today, 1);
  const info = weekInfo(today, bundle.semesterStart);
  const todayLessons = shownLessons(group, today, bundle.semesterStart, "auto", filters);
  const tomorrowLessons = shownLessons(group, tomorrow, bundle.semesterStart, "auto", filters);
  const status = liveStatus(group, today, now, bundle.semesterStart, filters);
  const upcoming = upcomingLessons(group, today, now, bundle.semesterStart, filters);
  const friend = data.friend ? bundle.find(data.friend)?.group ?? null : null;
  const shared = friend ? sharedFree(group, friend, today, now, bundle.semesterStart, filters) : [];
  const openTodos = sortedTodos(data.todos).filter((t) => !t.done);
  const favorites = [...data.favoriteTeachers].sort();
  const days = [...new Set(upcoming.map((u) => u.day))];
  const rows = days.flatMap((day) => dayRows(upcoming.filter((u) => u.day === day).map((u) => u.lesson)).map((row) => ({ day, row })));

  return (
    <div className="stack">
      <div className="row" style={{ gap: 4 }}>
        <Mark size={44} />
        <div className="grow" style={{ marginLeft: 6 }}>
          <div className="semibold ellipsis" style={{ fontSize: 15 }}>{s.appName}</div>
          <div className="muted xsmall ellipsis">{faculty.short}</div>
        </div>
        <LanguageMark lang={data.lang} onLanguage={setLanguage} />
        <button className="icon-btn" aria-label={s.theme} onClick={(e) => switchTheme(!data.dark, originOf(e))}>
          <Icon name={data.dark ? "lightMode" : "darkMode"} />
        </button>
        <button className="icon-btn" aria-label={s.reminders} onClick={openReminders}>
          <Icon name="notifications" />
        </button>
      </div>

      <Announcements s={s} />

      <Appear index={0}>
        <button className="hero" onClick={() => setTab(5)}>
          <div style={{ fontSize: 14, color: "rgba(255,255,255,.82)" }}>{s.greeting(now.getHours())}</div>
          <div className="semibold mt6" style={{ fontSize: 15, lineHeight: "20px" }}>{liveLine(s, status)}</div>
          <div className="bold" style={{ fontSize: 28, lineHeight: 1.2, overflowWrap: "anywhere" }}>{data.name.trim() || s.studentFallback}</div>
          <div className="mt6" style={{ fontSize: 14, color: "rgba(255,255,255,.92)" }}>{group.id}</div>
          <div className="mt2" style={{ fontSize: 13, color: "rgba(255,255,255,.8)" }}>{s.facultyName(faculty.name)}</div>
          <div style={{ fontSize: 13, color: "rgba(255,255,255,.9)", marginTop: 14 }}>{s.formatLongDateCap(today)}</div>
          <div className="row mt12" style={{ flexWrap: "wrap" }}>
            <span className="pill">{s.todayPill(todayLessons.length)}</span>
            <span className="pill">{s.evenWeekTitle(info.even)}</span>
          </div>
        </button>
      </Appear>

      <div className="row" style={{ gap: 12, alignItems: "stretch" }}>
        {[
          [s.today, todayLessons.length],
          [s.tomorrow, tomorrowLessons.length],
        ].map(([title, count]) => (
          <div key={title} className="card stat">
            <div className="muted small">{title}</div>
            <div className="value">{count}</div>
            <div className="blue small" style={{ fontWeight: 500 }}>{s.pairsWord(count as number)}</div>
          </div>
        ))}
      </div>

      {friend && (
        <div className="card">
          <h2 className="h2">{s.bothFreeTitle}</h2>
          {shared.length === 0 ? (
            <div className="muted small mt4">{s.noSharedWindow}</div>
          ) : (
            shared.map(({ day, time }) => (
              <div key={`${day}${time}`} className="blue mt6" style={{ fontSize: 14 }}>
                {s.bothFreeLine(day, today, time)}
              </div>
            ))
          )}
        </div>
      )}

      <div className="card" role="button" tabIndex={0} onClick={openTasks} style={{ cursor: "pointer" }}>
        <h2 className="h2">{s.tasks}</h2>
        <div className="small mt2" style={{ color: openTodos.some((t) => isOverdue(t, now)) ? "var(--red)" : "var(--muted)" }}>
          {openTodos.length === 0 ? s.tasksEmpty : s.openTasksLine(openTodos.length)}
        </div>
        {openTodos.slice(0, 2).map((item) => (
          <TaskPreview key={item.id} item={item} />
        ))}
        {tasksExpanded && openTodos.slice(2, 6).map((item) => <TaskPreview key={item.id} item={item} />)}
        {openTodos.length > 2 && (
          <button
            className={`hidden-toggle blue semibold${tasksExpanded ? " open" : ""}`}
            style={{ color: "var(--blue)", fontSize: 13, paddingLeft: 0 }}
            onClick={(e) => {
              e.stopPropagation();
              setTasksExpanded(!tasksExpanded);
            }}
          >
            {tasksExpanded ? s.collapseTasks : s.expandTasks}
            <Icon name="expandMore" size={18} />
          </button>
        )}
      </div>

      {favorites.length > 0 && (
        <div className="card">
          <h2 className="h2">{s.favoriteTeachers}</h2>
          {favorites.map((name) => {
            const next = nextTeacherLessons(bundle, name, today, now, 1)[0];
            return (
              <button key={name} className="row" style={{ width: "100%", textAlign: "left", padding: "8px 0 4px" }} onClick={() => openTeacher(name)}>
                <div className="grow">
                  <div className="ellipsis" style={{ fontSize: 14 }}>{name}</div>
                  <div className={`xsmall ellipsis mt2 ${next ? "blue" : "muted"}`}>{next ? teacherFollowLine(s, today, next) : s.noTeacherSoon}</div>
                </div>
                <span className="muted">
                  <Icon name="chevronRight" size={18} />
                </span>
              </button>
            );
          })}
        </div>
      )}

      <div style={{ marginTop: 4 }}>
        <h2 className="h2">{s.upcomingLessons}</h2>
        <div className="muted small">{s.studyWeekLine(info.number, info.even)}</div>
      </div>

      {upcoming.length === 0 ? (
        <div className="card">
          <div className="semibold">{s.freeWindow}</div>
          <div className="muted small mt4">{s.freeWindowText}</div>
        </div>
      ) : (
        rows.map(({ day, row }, index) =>
          row.kind === "class" ? (
            <Appear key={`${day}|${row.lesson.key}`} index={index + 1}>
              <ScheduleLesson lesson={row.lesson} date={day} caption={s.whenLabel(day, today)} />
            </Appear>
          ) : (
            <GapRow key={`gap-${index}-${day}-${row.startsAt}`} minutes={row.minutes} onCreate={() => editTodo({ date: day, time: row.startsAt })} />
          ),
        )
      )}
    </div>
  );
}
