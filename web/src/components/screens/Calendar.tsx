"use client";

import { addMonths, daysInMonth, dow, parts } from "@/lib/dates";
import { holidayOn, shownLessons } from "@/lib/schedule";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { LessonCard } from "../LessonCard";

export default function CalendarScreen() {
  const { bundle, s, group, filters, filter, selectedDate: date, setSelectedDate, today, openLesson } = useShell();
  const { y, m } = parts(date);
  const first = `${y}-${String(m).padStart(2, "0")}-01`;
  const cells: (string | null)[] = [
    ...Array.from({ length: dow(first) - 1 }, () => null),
    ...Array.from({ length: daysInMonth(y, m) }, (_, i) => `${first.slice(0, 8)}${String(i + 1).padStart(2, "0")}`),
  ];
  const holiday = holidayOn(date);
  const lessons = shownLessons(group, date, bundle.semesterStart, filter, filters);

  return (
    <div className="stack" style={{ gap: 0 }}>
      <h1 className="h1">{s.calendar}</h1>
      <div className="card mt8">
        <div className="row">
          <button className="icon-btn" aria-label={s.previousMonth} onClick={() => setSelectedDate(addMonths(date, -1))}>
            <Icon name="chevronLeft" />
          </button>
          <div className="grow semibold" style={{ fontSize: 16 }}>{s.formatMonth(date)}</div>
          <button className="icon-btn" aria-label={s.nextMonth} onClick={() => setSelectedDate(addMonths(date, 1))}>
            <Icon name="chevronRight" />
          </button>
        </div>
        <div className="cal-grid" style={{ paddingBottom: 6 }}>
          {s.dayShort.map((label) => (
            <div key={label} className="muted xsmall" style={{ textAlign: "center", fontWeight: 500 }}>{label}</div>
          ))}
        </div>
        <div className="cal-grid">
          {cells.map((cell, index) => {
            if (!cell) return <div key={`pad${index}`} className="cal-cell" />;
            const off = holidayOn(cell) != null;
            const busy = !off && shownLessons(group, cell, bundle.semesterStart, filter, filters).length > 0;
            const cls = ["cal-day", cell === date ? "on" : "", cell === today ? "today" : "", off ? "off" : ""].filter(Boolean).join(" ");
            return (
              <button key={cell} className="cal-cell" onClick={() => setSelectedDate(cell)}>
                <span className={cls}>{Number(cell.slice(8))}</span>
                <span className={`cal-dot${busy ? " busy" : ""}`} />
              </button>
            );
          })}
        </div>
      </div>
      <h3 className="h3" style={{ marginTop: 16, marginBottom: 8 }}>{s.formatLongDateCap(date)}</h3>
      {holiday ? (
        <>
          <div className="semibold">{s.code === "en" ? holiday.nameEn : holiday.nameRu}</div>
          <div className="muted mt4">{s.holidayQuiet}</div>
        </>
      ) : lessons.length === 0 ? (
        <div className="muted">{s.noLessonsThatDay}</div>
      ) : (
        <div className="stack tight">
          {lessons.map((lesson) => (
            <LessonCard key={lesson.key} lesson={lesson} date={date} onClick={() => openLesson(lesson, date)} />
          ))}
        </div>
      )}
    </div>
  );
}
