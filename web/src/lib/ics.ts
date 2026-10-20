import { addDays, mondayOf, type ISODate, toMinutes } from "./dates";
import { type Filters, type Group, shownLessons } from "./schedule";

const escape = (text: string) => text.replace(/\\/g, "\\\\").replace(/;/g, "\\;").replace(/,/g, "\\,").replace(/\n/g, "\\n");

function fold(line: string): string {
  const chunks: string[] = [];
  for (let rest = line; rest.length; rest = rest.slice(73)) chunks.push(rest.slice(0, 73));
  return chunks.join("\r\n ");
}

const stamp = (date: ISODate, minutes: number) =>
  `${date.replace(/-/g, "")}T${String(Math.floor(minutes / 60)).padStart(2, "0")}${String(minutes % 60).padStart(2, "0")}00`;

function hash(text: string): string {
  let h = 5381;
  for (let i = 0; i < text.length; i++) h = ((h << 5) + h + text.charCodeAt(i)) >>> 0;
  return h.toString(36);
}

/** Builds an .ics for Mon–Sat of the week containing `anchor`; times are floating local time. */
export function weekIcs(group: Group, semesterStart: ISODate, anchor: ISODate, f: Filters, lessonWord: string): { text: string; count: number } {
  const monday = mondayOf(anchor);
  const now = new Date().toISOString().replace(/[-:]/g, "").replace(/\.\d+/, "");
  const lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//StudRaspBGTU//RU", "CALSCALE:GREGORIAN", "METHOD:PUBLISH"];
  let count = 0;
  for (let offset = 0; offset <= 5; offset++) {
    const date = addDays(monday, offset);
    for (const lesson of shownLessons(group, date, semesterStart, "auto", f)) {
      const start = toMinutes(lesson.start);
      if (start == null) continue;
      const end = toMinutes(lesson.end) ?? start + 95;
      const description = [lesson.type, lesson.teacher, group.id].filter(Boolean).join("\n");
      lines.push(
        "BEGIN:VEVENT",
        `UID:${date}-${hash(lesson.key)}@studrasp-bgtu`,
        `DTSTAMP:${now}`,
        `DTSTART:${stamp(date, start)}`,
        `DTEND:${stamp(date, end)}`,
        fold(`SUMMARY:${escape(lesson.subject || lessonWord)}`),
        ...(lesson.room ? [fold(`LOCATION:${escape(lesson.room)}`)] : []),
        fold(`DESCRIPTION:${escape(description)}`),
        "END:VEVENT",
      );
      count++;
    }
  }
  lines.push("END:VCALENDAR");
  return { text: lines.join("\r\n") + "\r\n", count };
}
