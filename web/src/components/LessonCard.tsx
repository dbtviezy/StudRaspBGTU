"use client";

import { useState } from "react";
import type { ISODate } from "@/lib/dates";
import { changeId, type Lesson, pairNumber, typeColorKey } from "@/lib/schedule";
import { actions, noteOf } from "@/lib/store";
import { useShell } from "./context";
import { Icon, type IconName } from "./Icons";
import { Centered, useLongPress } from "./ui";

export function accent(type: string): string {
  switch (typeColorKey(type)) {
    case "lab": return "var(--teal)";
    case "practice": return "var(--amber)";
    case "online": return "var(--violet)";
    case "lecture": return "var(--blue)";
    default: return "var(--other)";
  }
}

export function LessonCard({
  lesson,
  date,
  caption,
  onClick,
  onLongPress,
}: {
  lesson: Lesson;
  date: ISODate;
  caption?: string;
  onClick: () => void;
  onLongPress?: () => void;
}) {
  const { s, data } = useShell();
  const color = accent(lesson.type);
  const press = useLongPress(onLongPress ?? (() => undefined), onClick);
  const missed = data.absences.some((a) => a.date === date && a.key === lesson.key);
  const note = noteOf(data, date, lesson.key);
  const subjectNote = data.subjectNotes[lesson.subject] ?? "";
  const extra = [note, subjectNote].filter((t) => t.trim()).join("\n");
  const hasFile = Object.values(data.fileTags).some((keys) => keys.includes(lesson.key));
  const hasTask = data.todos.some((t) => t.lessonKey === lesson.key && !t.done);
  const meta = [s.lessonType(lesson.type), s.roomName(lesson.room)].filter(Boolean).join(" · ");
  const pair = pairNumber(lesson.start);
  return (
    <button type="button" className={`card lesson${missed ? " missed" : ""}`} {...press}>
      {missed && <span className="badge-missed">{s.missedBadge}</span>}
      <div className="times">
        <div className="bold" style={{ fontSize: 14 }}>{lesson.start || "—"}</div>
        <div className="muted xsmall">{lesson.end}</div>
        {pair != null && <div className="blue" style={{ fontSize: 11, fontWeight: 500 }}>{s.pairLabel(pair)}</div>}
      </div>
      <div className="bar" style={{ background: color }} />
      <div className="grow" style={{ paddingRight: missed ? 52 : 0 }}>
        {caption && <div className="muted" style={{ fontSize: 11 }}>{caption}</div>}
        <div className="semibold" style={{ fontSize: 15 }}>{lesson.subject}</div>
        {meta && <div className="muted xsmall">{meta}</div>}
        {lesson.teacher && <div className="xsmall" style={{ color: "var(--ink-75)" }}>{lesson.teacher}</div>}
        <div className="mt2" style={{ fontSize: 11, fontWeight: 500, color }}>{s.weekBadge(lesson.week)}</div>
        {extra && (
          <div className="blue xsmall mt4 pre" style={{ lineHeight: "16px", display: "-webkit-box", WebkitLineClamp: 4, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
            {extra}
          </div>
        )}
        {hasFile && <div className="blue xsmall mt2">{s.hasFile}</div>}
        {hasTask && <div className="blue xsmall mt2">{s.hasTask}</div>}
      </div>
    </button>
  );
}

export function ScheduleLesson({ lesson, date, caption }: { lesson: Lesson; date: ISODate; caption?: string }) {
  const { openLesson } = useShell();
  const [menu, setMenu] = useState(false);
  return (
    <>
      <LessonCard lesson={lesson} date={date} caption={caption} onClick={() => openLesson(lesson, date)} onLongPress={() => setMenu(true)} />
      {menu && <QuickMenu lesson={lesson} date={date} onClose={() => setMenu(false)} />}
    </>
  );
}

function QuickAction({ icon, label, danger, onClick }: { icon: IconName; label: string; danger?: boolean; onClick: () => void }) {
  return (
    <button className={`quick-action${danger ? " danger" : ""}`} onClick={onClick}>
      <span className="well">
        <Icon name={icon} size={20} />
      </span>
      {label}
    </button>
  );
}

function QuickMenu({ lesson, date, onClose }: { lesson: Lesson; date: ISODate; onClose: () => void }) {
  const { s, data, filters } = useShell();
  const hidden = filters.hidden.has(lesson.key);
  const missed = data.absences.some((a) => a.date === date && a.key === lesson.key);
  const changed = filters.changes?.has(changeId(date, lesson.key)) ?? false;
  const run = (body: () => void) => () => {
    body();
    onClose();
  };
  return (
    <Centered onClose={onClose}>
      <div className="row" style={{ padding: "0 8px", gap: 12 }}>
        <div style={{ width: 3, height: 36, borderRadius: 2, background: accent(lesson.type) }} />
        <div className="grow">
          <div className="semibold" style={{ fontSize: 16 }}>{lesson.subject || s.lessonFallback}</div>
          <div className="muted xsmall mt2">{[lesson.start, lesson.end].filter(Boolean).join("–")}</div>
        </div>
      </div>
      <div className="mt8">
        <QuickAction icon={hidden ? "visibility" : "visibilityOff"} label={hidden ? s.attendLesson : s.hideLesson} onClick={run(() => actions.setHidden(lesson.key, !hidden))} />
        <QuickAction icon="eventBusy" label={missed ? s.clearAbsence : s.wasAbsent} onClick={run(() => actions.toggleAbsence(date, lesson.key, lesson.subject))} />
        {changed && <QuickAction icon="undo" label={s.officialAgain} onClick={run(() => actions.clearDayChange(date, lesson.key))} />}
        <QuickAction icon="close" danger label={s.goneToday} onClick={run(() => actions.setDayChange(date, lesson.key, true, null))} />
      </div>
    </Centered>
  );
}

export function GapRow({ minutes, onCreate }: { minutes: number; onCreate: () => void }) {
  const { s } = useShell();
  const long = minutes >= 60;
  const text = `${s.liveWindow} ${s.span(minutes)}`;
  if (!long) return <div className="gap-row">{text}</div>;
  return (
    <button className="gap-row long" onClick={onCreate}>
      {text}
    </button>
  );
}
