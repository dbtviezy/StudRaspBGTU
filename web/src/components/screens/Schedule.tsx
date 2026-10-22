"use client";

import { useState } from "react";
import { download } from "@/lib/backup";
import { addDays, dow, mondayOf, type ISODate } from "@/lib/dates";
import { weekIcs } from "@/lib/ics";
import {
  applyChange,
  changeId,
  dayRows,
  holidayOn,
  type Lesson,
  lessonsOfKind,
  lessonsOn,
  shownLessons,
  weekInfo,
} from "@/lib/schedule";
import { actions } from "@/lib/store";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { GapRow, ScheduleLesson } from "../LessonCard";
import { Alert, Appear, Chip } from "../ui";

const shareBody = (lessons: Lesson[]) =>
  lessons.length === 0 ? "—" : lessons.map((l) => [l.time || l.start, l.subject, l.room].filter(Boolean).join(" · ")).join("\n");

export async function shareText(text: string, onCopied: () => void) {
  if (navigator.share) {
    try {
      await navigator.share({ text });
      return;
    } catch (error) {
      if ((error as DOMException)?.name === "AbortError") return;
    }
  }
  try {
    await navigator.clipboard.writeText(text);
    onCopied();
  } catch {}
}

function WeekColumn({ title, lessons, date }: { title: string; lessons: Lesson[]; date: ISODate }) {
  const { s } = useShell();
  return (
    <div className="stack grow" style={{ gap: 8, flex: 1 }}>
      <div className="blue semibold small">{title}</div>
      {lessons.length === 0 ? <div className="muted xsmall">{s.noLessons}</div> : lessons.map((l) => <ScheduleLesson key={l.key} lesson={l} date={date} />)}
    </div>
  );
}

function HiddenSection({ date, skipped, cancelled }: { date: ISODate; skipped: Lesson[]; cancelled: Lesson[] }) {
  const { s } = useShell();
  const [open, setOpen] = useState(false);
  const total = skipped.length + cancelled.length;
  if (total === 0) return null;
  const row = (lesson: Lesson, restore: () => void) => (
    <div key={lesson.key} className="hidden-row">
      <div className="grow ellipsis">
        {[lesson.start, lesson.subject].filter(Boolean).join(" · ")}
      </div>
      <button className="tbtn" style={{ fontSize: 12, padding: "4px 6px" }} onClick={restore}>
        {s.restore}
      </button>
    </div>
  );
  return (
    <div style={{ marginTop: 4 }}>
      <button className={`hidden-toggle${open ? " open" : ""}`} onClick={() => setOpen(!open)} aria-expanded={open}>
        {s.hiddenSection(total)}
        <Icon name="expandMore" size={16} />
      </button>
      {open && (
        <div className="hidden-list appear">
          {skipped.length > 0 && (
            <>
              <div className="semibold" style={{ marginTop: 2 }}>{s.hideLesson}</div>
              {skipped.map((l) => row(l, () => actions.setHidden(l.key, false)))}
            </>
          )}
          {cancelled.length > 0 && (
            <>
              <div className="semibold" style={{ marginTop: 6 }}>{s.goneToday}</div>
              {cancelled.map((l) => row(l, () => actions.clearDayChange(date, l.key)))}
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default function ScheduleScreen() {
  const { bundle, s, faculty, group, filters, selectedDate: date, setSelectedDate, filter, setFilter, today, editTodo, toast } = useShell();
  const [exportMessage, setExportMessage] = useState<string | null>(null);
  const holiday = holidayOn(date);
  const monday = mondayOf(date);
  const info = weekInfo(date, bundle.semesterStart);
  const lessons = shownLessons(group, date, bundle.semesterStart, filter, filters);
  const day = dow(date);
  const ofKind = (kind: string) =>
    lessonsOfKind(group, day, kind, filters.hidden)
      .map((l) => applyChange(filters.changes, date, l))
      .filter((l): l is Lesson => l != null);

  const none = new Set<string>();
  const base = holiday
    ? []
    : filter === "both"
      ? [...new Map([...lessonsOfKind(group, day, "num", none), ...lessonsOfKind(group, day, "den", none)].map((l) => [l.key, l])).values()]
      : lessonsOn(group, date, bundle.semesterStart, filter);
  const skipped = base.filter((l) => filters.hidden.has(l.key));
  const cancelled = base.filter((l) => !filters.hidden.has(l.key) && filters.changes?.get(changeId(date, l.key))?.cancelled);

  const share = () => {
    const head = `${s.formatLongDateCap(date)}\n${group.id}`;
    let text: string;
    if (holiday) text = `${head}\n${s.code === "en" ? holiday.nameEn : holiday.nameRu}`;
    else if (filter === "both") text = `${head}\n${s.numeratorColumn}\n${shareBody(ofKind("num"))}\n${s.denominatorColumn}\n${shareBody(ofKind("den"))}`;
    else if (lessons.length === 0) text = `${head}\n${s.noLessonsThatDay}`;
    else text = `${head}\n${shareBody(lessons)}`;
    shareText(text, () => toast(s.copied));
  };

  const exportWeek = () => {
    const { text, count } = weekIcs(group, bundle.semesterStart, date, filters, s.lessonFallback);
    if (count === 0) {
      setExportMessage(s.exportEmpty);
      return;
    }
    download(`studrasp-${group.id}-${monday}.ics`, text, "text/calendar;charset=utf-8");
    setExportMessage(s.icsReady(count));
  };

  const rows = dayRows(lessons);

  return (
    <div className="stack tight">
      <div>
        <h1 className="h1">{s.schedule}</h1>
        <div className="muted small">{`${group.id} · ${s.facultyName(faculty.name)}`}</div>
        <div className="mt4" style={{ fontSize: 14 }}>{`${s.formatLongDateCap(date)} · ${s.evenWeekTitle(info.even)}`}</div>
        <div className="row" style={{ gap: 4, marginLeft: -8 }}>
          <button className="tbtn" onClick={share}>{s.shareDay}</button>
          <button className="tbtn" onClick={exportWeek}>{s.exportWeek}</button>
        </div>
        <div className="scroll-x">
          {Array.from({ length: 6 }, (_, offset) => {
            const d = addDays(monday, offset);
            return (
              <button key={d} className={`daychip${d === date ? " on" : ""}${d === today ? " today" : ""}`} onClick={() => setSelectedDate(d)}>
                <span className="dn">{s.dayShort[offset]}</span>
                <span className="dd">{Number(d.slice(8))}</span>
              </button>
            );
          })}
        </div>
        <div className="scroll-x mt8">
          <Chip on={filter === "auto"} onClick={() => setFilter("auto")}>{s.filterAuto}</Chip>
          <Chip on={filter === "all"} onClick={() => setFilter("all")}>{s.filterAll}</Chip>
          <Chip on={filter === "num"} onClick={() => setFilter("num")}>{s.numerator[0].toUpperCase() + s.numerator.slice(1)}</Chip>
          <Chip on={filter === "den"} onClick={() => setFilter("den")}>{s.denominator[0].toUpperCase() + s.denominator.slice(1)}</Chip>
          <Chip on={filter === "both"} onClick={() => setFilter("both")}>{s.bothWeeks}</Chip>
        </div>
      </div>

      {holiday ? (
        <div className="card">
          <div className="semibold">{s.code === "en" ? holiday.nameEn : holiday.nameRu}</div>
          <div className="muted small mt4">{s.holidayQuiet}</div>
        </div>
      ) : filter === "both" ? (
        <div className="row" style={{ alignItems: "flex-start", gap: 8 }}>
          <WeekColumn title={s.numeratorColumn} lessons={ofKind("num")} date={date} />
          <WeekColumn title={s.denominatorColumn} lessons={ofKind("den")} date={date} />
        </div>
      ) : lessons.length === 0 ? (
        <div style={{ padding: "28px 0", textAlign: "center" }}>
          <div className="semibold" style={{ fontSize: 18 }}>{s.noLessons}</div>
          <div className="muted small">{s.noLessonsHint}</div>
        </div>
      ) : (
        rows.map((row, index) =>
          row.kind === "class" ? (
            <Appear key={`${date}|${row.lesson.key}`} index={index}>
              <ScheduleLesson lesson={row.lesson} date={date} />
            </Appear>
          ) : (
            <GapRow key={`gap-${index}-${row.startsAt}`} minutes={row.minutes} onCreate={() => editTodo({ date, time: row.startsAt })} />
          ),
        )
      )}

      <HiddenSection key={date} date={date} skipped={skipped} cancelled={cancelled} />

      {exportMessage && (
        <Alert title={s.exportWeek} text={exportMessage} onClose={() => setExportMessage(null)}>
          <button className="tbtn" onClick={() => setExportMessage(null)}>{s.ok}</button>
        </Alert>
      )}
    </div>
  );
}
