"use client";

import { useRef, useState } from "react";
import type { ISODate } from "@/lib/dates";
import { addFile, openFile } from "@/lib/idb";
import { summaryFor, useSummaries } from "@/lib/ratings";
import { applyChange, changeId, type Lesson, pairNumber } from "@/lib/schedule";
import { actions, noteOf, sortedTodos } from "@/lib/store";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { accent } from "../LessonCard";
import { Detail, Sheet, Stars } from "../ui";
import { deadlineText } from "../screens/Home";

export default function LessonDialog({ lesson: opened, date, onClose }: { lesson: Lesson; date: ISODate; onClose: () => void }) {
  const { s, data, group, filters, openTeacher, editTodo, toast } = useShell();
  const official = group.lessons.find((l) => l.key === opened.key) ?? opened;
  const change = filters.changes?.get(changeId(date, official.key)) ?? null;
  const lesson = applyChange(filters.changes, date, official) ?? official;
  const key = official.key;
  const [room, setRoom] = useState(change?.room ?? "");
  const [copying, setCopying] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const summaries = useSummaries();
  const teacher = lesson.teacher.trim();
  const summary = teacher ? summaryFor(summaries, teacher) : undefined;
  const favorite = data.favoriteTeachers.includes(teacher);
  const hidden = filters.hidden.has(key);
  const missed = data.absences.some((a) => a.date === date && a.key === key);
  const pair = pairNumber(lesson.start);
  const todos = sortedTodos(data.todos).filter((t) => t.lessonKey === key);
  const files = Object.entries(data.fileTags)
    .filter(([, keys]) => keys.includes(key))
    .map(([name]) => name)
    .sort();

  const attach = async (list: FileList | null) => {
    if (!list?.length) return;
    setCopying(true);
    const names: string[] = [];
    try {
      for (const file of Array.from(list)) {
        const name = await addFile(file);
        actions.linkFile(name, key, true);
        names.push(name);
      }
      toast(s.filesCopied(names.join(", ")));
    } catch {
      toast(s.copyFailed);
    } finally {
      setCopying(false);
    }
  };

  return (
    <Sheet
      onClose={onClose}
      foot={
        <button className="btn compact" onClick={onClose}>
          {s.close}
        </button>
      }
    >
      <div style={{ fontSize: 12, fontWeight: 600, color: accent(lesson.type) }}>{s.weekBadge(lesson.week)}</div>
      <h2 className="h2 mt4" style={{ fontSize: 22, lineHeight: 1.25 }}>{lesson.subject || s.lessonFallback}</h2>

      <div className="mt12">
        <Detail label={s.time} value={[lesson.start, lesson.end].filter(Boolean).join("–") || "—"} />
        {pair != null && <Detail label={s.pair} value={s.pairLabel(pair)} />}
        {lesson.type && <Detail label={s.type} value={s.lessonType(lesson.type)} />}
        {lesson.room && <Detail label={s.room} value={lesson.room} />}
        {teacher && (
          <div className="detail">
            <div className="label">{s.teacher}</div>
            <div className="row" style={{ gap: 4 }}>
              <button
                className="grow"
                style={{ textAlign: "left", color: "var(--blue)", fontWeight: 500, fontSize: 15 }}
                onClick={() => {
                  onClose();
                  openTeacher(teacher);
                }}
              >
                {lesson.teacher}
                {summary && summary.count > 0 && (
                  <span className="row xsmall mt2" style={{ gap: 6, color: "var(--ink)" }}>
                    <Stars value={summary.overall} size={12} />
                    {summary.overall.toFixed(1)}
                    <span className="muted">· {s.ratingsCount(summary.count)}</span>
                  </span>
                )}
              </button>
              <button
                className="icon-btn"
                style={{ color: favorite ? "var(--blue)" : "var(--muted)" }}
                aria-label={favorite ? s.removeFavorite : s.addFavorite}
                onClick={() => actions.setFavorite(teacher, !favorite)}
              >
                <Icon name={favorite ? "star" : "starBorder"} />
              </button>
            </div>
          </div>
        )}
        <Detail label={s.day} value={s.formatLongDateCap(date)} />
      </div>

      <div className="row mt12" style={{ gap: 8, flexWrap: "wrap" }}>
        <button className={`chip${hidden ? " on" : ""}`} onClick={() => actions.setHidden(key, !hidden)}>
          {hidden ? s.attendLesson : s.hideLesson}
        </button>
        <button className={`chip${missed ? " on" : ""}`} onClick={() => actions.toggleAbsence(date, key, lesson.subject)}>
          {missed ? s.clearAbsence : s.wasAbsent}
        </button>
      </div>

      <h3 className="h3 mt16">{s.tasksForLesson}</h3>
      {todos.length === 0 && <div className="muted small mt4">{s.noTasksForLesson}</div>}
      {todos.map((item) => (
        <div key={item.id} className="task-row">
          <input
            type="checkbox"
            checked={item.done}
            aria-label={item.title}
            onChange={(e) => actions.upsertTodo({ ...item, done: e.target.checked })}
          />
          <button className="grow" style={{ textAlign: "left" }} onClick={() => editTodo({ initial: item })}>
            <div className={item.done ? "done-text" : undefined}>{item.title}</div>
            <div className="muted xsmall">{deadlineText(s, item)}</div>
          </button>
        </div>
      ))}
      <button className="tbtn flush" onClick={() => editTodo({ lesson: official })}>{s.addTask}</button>

      <h3 className="h3 mt12">{s.files}</h3>
      {files.length === 0 && <div className="muted small mt4">{s.nothingAttached}</div>}
      {files.map((name) => (
        <div key={name} className="row mt4">
          <button className="grow ellipsis blue small" style={{ textAlign: "left", padding: "6px 0" }} onClick={() => openFile(name)}>
            {name}
          </button>
          <button className="tbtn muted" onClick={() => actions.linkFile(name, key, false)}>{s.removeLesson}</button>
        </div>
      ))}
      <button className="tbtn flush" disabled={copying} onClick={() => fileInput.current?.click()}>
        {copying ? s.copying : s.attachFile}
      </button>
      <input
        ref={fileInput}
        type="file"
        multiple
        hidden
        onChange={(e) => {
          const list = e.target.files;
          attach(list).finally(() => {
            e.target.value = "";
          });
        }}
      />

      <label className="field mt12">
        <span>{s.subjectNote}</span>
        <textarea
          className="input"
          rows={2}
          value={data.subjectNotes[lesson.subject] ?? ""}
          placeholder={s.subjectNoteHint}
          onChange={(e) => actions.setSubjectNote(lesson.subject, e.target.value)}
        />
      </label>

      <label className="field mt12">
        <span>{s.note}</span>
        <textarea
          className="input"
          rows={2}
          value={noteOf(data, date, key)}
          placeholder={s.noteHint}
          onChange={(e) => actions.setNote(date, key, e.target.value)}
        />
      </label>

      <div className="card mt16" style={{ background: "var(--bg)", boxShadow: "none" }}>
        <h3 className="h3">{s.todayChange}</h3>
        <div className="muted xsmall mt2">{s.todayOnly}</div>
        <label className="field mt8">
          <span>{s.todayRoom}</span>
          <input className="input" value={room} maxLength={40} placeholder={s.roomExample} onChange={(e) => setRoom(e.target.value)} />
        </label>
        <div className="row mt4" style={{ marginLeft: -8, flexWrap: "wrap", gap: 0 }}>
          <button
            className="tbtn"
            disabled={!room.trim()}
            onClick={() => actions.setDayChange(date, key, false, room)}
          >
            {s.saveRoom}
          </button>
          {change && (
            <button
              className="tbtn"
              onClick={() => {
                actions.clearDayChange(date, key);
                setRoom("");
              }}
            >
              {s.officialAgain}
            </button>
          )}
          <button
            className="tbtn danger"
            onClick={() => {
              actions.setDayChange(date, key, true, null);
              onClose();
            }}
          >
            {s.cancelToday}
          </button>
        </div>
      </div>
    </Sheet>
  );
}
