"use client";

import { upcomingLessons, weekInfo } from "@/lib/schedule";
import { useShell } from "../context";
import { LessonCard } from "../LessonCard";
import { Overlay, OverlayHeader } from "../ui";

export default function NotesOverlay({ onClose }: { onClose: () => void }) {
  const { bundle, s, group, today, now, filters, openLesson } = useShell();
  const info = weekInfo(today, bundle.semesterStart);
  const upcoming = upcomingLessons(group, today, now, bundle.semesterStart, filters, 8);
  return (
    <Overlay onClose={onClose}>
      <OverlayHeader title={s.reminders} onClose={onClose} closeLabel={s.close} />
      <div className="muted small">{s.remindersHint}</div>
      <div className="card mt12">
        <div className="semibold">{s.studyWeekLine(info.number, info.even)}</div>
        <div className="muted small mt4">{s.weekNow(info.number, info.even)}</div>
      </div>
      <div className="stack tight mt12">
        {upcoming.length === 0 && <div className="muted">{s.noUpcoming}</div>}
        {upcoming.map(({ day, lesson }) => (
          <LessonCard
            key={`${day}|${lesson.key}`}
            lesson={lesson}
            date={day}
            caption={s.whenLabel(day, today)}
            onClick={() => {
              onClose();
              openLesson(lesson, day);
            }}
          />
        ))}
      </div>
    </Overlay>
  );
}
