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

const MOSCOW = [
  "BEGIN:VTIMEZONE",
  "TZID:Europe/Moscow",
  "BEGIN:STANDARD",
  "DTSTART:19700101T000000",
  "TZOFFSETFROM:+0300",
  "TZOFFSETTO:+0300",
  "TZNAME:MSK",
  "END:STANDARD",
  "END:VTIMEZONE",
];

interface IcsOptions {
  /** Calendar name and refresh hints for subscriptions; times are then pinned to Moscow. */
  subscription?: { name: string };
}

/** Builds an .ics for `days` days starting at `from`. */
export function rangeIcs(
  group: Group,
  semesterStart: ISODate,
  from: ISODate,
  days: number,
  f: Filters,
  lessonWord: string,
  options: IcsOptions = {},
): { text: string; count: number } {
  const now = new Date().toISOString().replace(/[-:]/g, "").replace(/\.\d+/, "");
  const lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//StudRaspBGTU//RU", "CALSCALE:GREGORIAN", "METHOD:PUBLISH"];
  const sub = options.subscription;
  if (sub) {
    lines.push(
      fold(`X-WR-CALNAME:${escape(sub.name)}`),
      "X-WR-TIMEZONE:Europe/Moscow",
      "REFRESH-INTERVAL;VALUE=DURATION:PT12H",
      "X-PUBLISHED-TTL:PT12H",
      ...MOSCOW,
    );
  }
  const zone = sub ? ";TZID=Europe/Moscow" : "";
  let count = 0;
  for (let offset = 0; offset < days; offset++) {
    const date = addDays(from, offset);
    for (const lesson of shownLessons(group, date, semesterStart, "auto", f)) {
      const start = toMinutes(lesson.start);
      if (start == null) continue;
      const end = toMinutes(lesson.end) ?? start + 95;
      const description = [lesson.type, lesson.teacher, group.id].filter(Boolean).join("\n");
      lines.push(
        "BEGIN:VEVENT",
        `UID:${date}-${hash(lesson.key)}@studrasp-bgtu`,
        `DTSTAMP:${now}`,
        `DTSTART${zone}:${stamp(date, start)}`,
        `DTEND${zone}:${stamp(date, end)}`,
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

/** Builds an .ics for Mon–Sat of the week containing `anchor`; times are floating local time. */
export function weekIcs(group: Group, semesterStart: ISODate, anchor: ISODate, f: Filters, lessonWord: string): { text: string; count: number } {
  return rangeIcs(group, semesterStart, mondayOf(anchor), 6, f, lessonWord);
}

/** Public address of the self-updating calendar for a group. */
export function subscriptionUrl(origin: string, groupId: string): string {
  return `${origin}/api/ics/${encodeURIComponent(groupId)}.ics`;
}
