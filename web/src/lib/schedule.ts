import { addDays, daysBetween, dow, type ISODate, minutesOf, mondayOf, toMinutes } from "./dates";

export interface Lesson {
  day: number;
  dayName: string;
  time: string;
  start: string;
  end: string;
  subject: string;
  type: string;
  teacher: string;
  room: string;
  week: string;
  /** Key of the official lesson; stays the same when a day override changes the room. */
  key: string;
}

export interface Group {
  id: string;
  level: string;
  year: number | null;
  lessons: Lesson[];
}

export interface Faculty {
  name: string;
  short: string;
  groups: Group[];
}

export interface Entry {
  faculty: Faculty;
  group: Group;
}

export interface Bundle {
  source: string;
  periodLabel: string;
  fetchedAt: string;
  semesterStart: ISODate;
  faculties: Faculty[];
  groups: Entry[];
  find(id: string | null | undefined): Entry | null;
}

export interface DayChange {
  date: ISODate;
  key: string;
  cancelled: boolean;
  room: string;
}

export interface Holiday {
  nameRu: string;
  nameEn: string;
}

export interface Filters {
  hidden: Set<string>;
  changes: Map<string, DayChange> | null;
}

export const lessonKey = (l: Omit<Lesson, "key">) =>
  `${l.day}|${l.time}|${l.subject}|${l.teacher}|${l.room}|${l.week}`;

const str = (value: unknown, fallback = "") => (value == null ? fallback : String(value));

export function parseBundle(raw: unknown): Bundle {
  const root = raw as Record<string, unknown>;
  const faculties: Faculty[] = (root.faculties as Record<string, unknown>[]).map((faculty) => ({
    name: str(faculty.name),
    short: str(faculty.short),
    groups: (faculty.groups as Record<string, unknown>[]).map((group) => ({
      id: str(group.id),
      level: str(group.level),
      year: typeof group.year === "number" ? group.year : null,
      lessons: ((group.lessons as Record<string, unknown>[]) ?? []).map((item) => {
        const lesson = {
          day: Number(item.day) || 0,
          dayName: str(item.dayName),
          time: str(item.time),
          start: str(item.start),
          end: str(item.end),
          subject: str(item.subject),
          type: str(item.type),
          teacher: str(item.teacher),
          room: str(item.room),
          week: str(item.week, "all") || "all",
        };
        return { ...lesson, key: lessonKey(lesson) };
      }),
    })),
  }));
  const groups = faculties.flatMap((faculty) => faculty.groups.map((group) => ({ faculty, group })));
  const index = new Map(groups.map((entry) => [entry.group.id, entry]));
  const start = /^\d{4}-\d{2}-\d{2}$/.test(str(root.semesterStart)) ? str(root.semesterStart) : "2026-09-01";
  return {
    source: str(root.source),
    periodLabel: str(root.periodLabel),
    fetchedAt: str(root.fetchedAt),
    semesterStart: start,
    faculties,
    groups,
    find: (id) => (id ? index.get(id) ?? null : null),
  };
}

const HOLIDAYS: Map<ISODate, Holiday> = (() => {
  const map = new Map<ISODate, Holiday>();
  map.set("2026-11-04", { nameRu: "День народного единства", nameEn: "Unity Day" });
  for (let day = "2026-12-29"; day <= "2027-01-11"; day = addDays(day, 1)) {
    map.set(day, { nameRu: "Каникулы", nameEn: "Holidays" });
  }
  return map;
})();

export const holidayOn = (date: ISODate): Holiday | null => HOLIDAYS.get(date) ?? null;

export interface WeekInfo {
  number: number;
  even: boolean;
  kind: "num" | "den";
}

export function weekInfo(date: ISODate, semesterStart: ISODate): WeekInfo {
  const number = Math.trunc(daysBetween(mondayOf(semesterStart), mondayOf(date)) / 7) + 1;
  const even = number % 2 === 0;
  return { number, even, kind: even ? "den" : "num" };
}

export const lessonVisible = (lesson: Lesson, kind: string | null) =>
  kind == null || lesson.week === "all" || lesson.week === kind;

export function activeKind(date: ISODate, semesterStart: ISODate, filter: string): string | null {
  if (filter === "all") return null;
  if (filter === "num" || filter === "den") return filter;
  return weekInfo(date, semesterStart).kind;
}

const byStart = (a: Lesson, b: Lesson) => {
  const sa = a.start || "99:99";
  const sb = b.start || "99:99";
  if (sa !== sb) return sa < sb ? -1 : 1;
  return a.week < b.week ? -1 : a.week > b.week ? 1 : 0;
};

export function lessonsOn(group: Group, date: ISODate, semesterStart: ISODate, filter: string): Lesson[] {
  const kind = activeKind(date, semesterStart, filter);
  const day = dow(date);
  return group.lessons.filter((l) => l.day === day && lessonVisible(l, kind)).sort(byStart);
}

export function lessonsOfKind(group: Group, day: number, kind: string, hidden: Set<string>): Lesson[] {
  return group.lessons
    .filter((l) => l.day === day && !hidden.has(l.key) && (l.week === "all" || l.week === kind))
    .sort(byStart);
}

export const changeId = (date: ISODate, key: string) => `${date}|${key}`;

export function applyChange(changes: Map<string, DayChange> | null, date: ISODate, lesson: Lesson): Lesson | null {
  const change = changes?.get(changeId(date, lesson.key));
  if (!change) return lesson;
  if (change.cancelled) return null;
  const room = change.room.trim();
  return room ? { ...lesson, room } : lesson;
}

export function shownLessons(group: Group, date: ISODate, semesterStart: ISODate, filter: string, f: Filters): Lesson[] {
  if (holidayOn(date)) return [];
  return lessonsOn(group, date, semesterStart, filter)
    .filter((l) => !f.hidden.has(l.key))
    .map((l) => applyChange(f.changes, date, l))
    .filter((l): l is Lesson => l != null);
}

export function pairNumber(start: string): number | null {
  switch (start) {
    case "08:00": return 1;
    case "09:45": return 2;
    case "11:30": return 3;
    case "13:20": return 4;
    case "15:05": return 5;
    case "16:50": return 6;
    case "18:35": return 7;
    case "20:00":
    case "20:10": return 8;
    default: return null;
  }
}

export function typeColorKey(type: string): "lab" | "practice" | "online" | "lecture" | "other" {
  const t = type.toLowerCase();
  if (t.includes("лаб")) return "lab";
  if (t.includes("практ")) return "practice";
  if (t.includes("электрон")) return "online";
  if (t.includes("лек")) return "lecture";
  return "other";
}

export type LiveStatus =
  | { kind: "off"; holiday: Holiday }
  | { kind: "inClass"; lesson: Lesson; minutesLeft: number }
  | { kind: "soon"; lesson: Lesson; minutesUntil: number }
  | { kind: "window"; minutes: number; next: Lesson }
  | { kind: "done"; next: { day: ISODate; lesson: Lesson } | null }
  | { kind: "none" };

export function liveStatus(group: Group, today: ISODate, now: Date, semesterStart: ISODate, f: Filters): LiveStatus {
  const holiday = holidayOn(today);
  if (holiday) return { kind: "off", holiday };
  const nowM = minutesOf(now);
  const lessons = shownLessons(group, today, semesterStart, "auto", f);
  const current = lessons.find((l) => {
    const s = toMinutes(l.start);
    const e = toMinutes(l.end);
    return s != null && e != null && nowM >= s && nowM < e;
  });
  if (current) {
    const end = toMinutes(current.end)!;
    return { kind: "inClass", lesson: current, minutesLeft: Math.max(1, Math.floor(end - nowM)) };
  }
  const next = lessons.find((l) => {
    const s = toMinutes(l.start);
    return s != null && s > nowM;
  });
  if (next) {
    const start = toMinutes(next.start)!;
    const until = Math.max(0, Math.floor(start - nowM));
    const previous = [...lessons].reverse().find((l) => {
      const e = toMinutes(l.end);
      return e != null && e <= nowM;
    });
    const previousEnd = previous ? toMinutes(previous.end) : null;
    if (previousEnd != null && start - previousEnd >= 40) return { kind: "window", minutes: start - previousEnd, next };
    return { kind: "soon", lesson: next, minutesUntil: until };
  }
  const tomorrow = addDays(today, 1);
  const first = holidayOn(tomorrow) ? undefined : shownLessons(group, tomorrow, semesterStart, "auto", f)[0];
  const upcoming = first ? { day: tomorrow, lesson: first } : null;
  if (lessons.length === 0 && !upcoming) return { kind: "none" };
  return { kind: "done", next: upcoming };
}

export function upcomingLessons(
  group: Group,
  today: ISODate,
  now: Date,
  semesterStart: ISODate,
  f: Filters,
  limit = 4,
): { day: ISODate; lesson: Lesson }[] {
  const result: { day: ISODate; lesson: Lesson }[] = [];
  const nowM = minutesOf(now);
  for (let offset = 0; offset <= 7; offset++) {
    const day = addDays(today, offset);
    if (holidayOn(day)) continue;
    for (const lesson of shownLessons(group, day, semesterStart, "auto", f)) {
      if (offset === 0) {
        const start = toMinutes(lesson.start);
        if (start != null && start < nowM - 15) continue;
      }
      result.push({ day, lesson });
      if (result.length >= limit) return result;
    }
  }
  return result;
}

export type DayRow = { kind: "class"; lesson: Lesson } | { kind: "gap"; minutes: number; startsAt: string };

export function dayRows(lessons: Lesson[]): DayRow[] {
  const rows: DayRow[] = [];
  lessons.forEach((lesson, index) => {
    rows.push({ kind: "class", lesson });
    const next = lessons[index + 1];
    if (!next) return;
    const from = toMinutes(lesson.end);
    const to = toMinutes(next.start);
    if (from == null || to == null) return;
    if (to - from >= 40) rows.push({ kind: "gap", minutes: to - from, startsAt: lesson.end });
  });
  return rows;
}

/** One person from the schedule; a lesson with two teachers counts for each of them. */
export interface TeacherCard {
  name: string;
  subjects: string[];
  groups: number;
  mine: boolean;
}

const cmp = (a: string | number, b: string | number) => (a < b ? -1 : a > b ? 1 : 0);

const directories = new WeakMap<Bundle, Map<string, { subjects: Set<string>; groups: Set<string> }>>();

function teacherDirectory(bundle: Bundle) {
  let directory = directories.get(bundle);
  if (directory) return directory;
  directory = new Map();
  for (const { group } of bundle.groups) {
    for (const lesson of group.lessons) {
      for (const name of splitTeachers(lesson.teacher)) {
        let entry = directory.get(name);
        if (!entry) directory.set(name, (entry = { subjects: new Set(), groups: new Set() }));
        if (lesson.subject) entry.subjects.add(lesson.subject);
        entry.groups.add(group.id);
      }
    }
  }
  directories.set(bundle, directory);
  return directory;
}

/** Teachers of `mine` first, then everyone else, each list alphabetical. An empty query lists only `mine`. */
export function searchTeachers(bundle: Bundle, query: string, mine: Group): TeacherCard[] {
  const text = query.trim().toLowerCase();
  const own = new Set(mine.lessons.flatMap((l) => splitTeachers(l.teacher)));
  const cards: TeacherCard[] = [];
  for (const [name, entry] of teacherDirectory(bundle)) {
    const isMine = own.has(name);
    if (text ? !name.toLowerCase().includes(text) : !isMine) continue;
    cards.push({ name, subjects: [...entry.subjects].sort(), groups: entry.groups.size, mine: isMine });
  }
  return cards.sort((a, b) => Number(b.mine) - Number(a.mine) || a.name.localeCompare(b.name, "ru")).slice(0, 80);
}

function teachesLesson(people: string[], raw: string, lesson: Lesson): boolean {
  if (lesson.teacher.trim().toLowerCase() === raw) return true;
  const names = splitTeachers(lesson.teacher);
  return people.some((person) => names.includes(person));
}

export interface TeacherNext {
  day: ISODate;
  groupId: string;
  facultyShort: string;
  lesson: Lesson;
}

export function nextTeacherLessons(
  bundle: Bundle,
  teacher: string,
  today: ISODate,
  now: Date,
  limit = 3,
): TeacherNext[] {
  const name = teacher.trim().toLowerCase();
  if (!name) return [];
  const people = splitTeachers(teacher);
  const hits: TeacherNext[] = [];
  for (const { faculty, group } of bundle.groups) {
    for (const lesson of group.lessons) {
      if (teachesLesson(people, name, lesson)) hits.push({ day: "", groupId: group.id, facultyShort: faculty.short, lesson });
    }
  }
  if (!hits.length) return [];
  const nowM = minutesOf(now);
  const result: TeacherNext[] = [];
  for (let offset = 0; offset <= 13; offset++) {
    const day = addDays(today, offset);
    if (dow(day) === 7 || holidayOn(day)) continue;
    const kind = activeKind(day, bundle.semesterStart, "auto");
    const todays = hits
      .filter((h) => h.lesson.day === dow(day) && lessonVisible(h.lesson, kind))
      .sort((a, b) => cmp(a.lesson.start || "99:99", b.lesson.start || "99:99"));
    for (const hit of todays) {
      if (offset === 0) {
        const end = toMinutes(hit.lesson.end) ?? toMinutes(hit.lesson.start);
        if (end != null && end <= nowM) continue;
      }
      result.push({ ...hit, day });
      if (result.length >= limit) return result;
    }
  }
  return result;
}

const PAIR_STARTS = ["08:00", "09:45", "11:30", "13:20", "15:05", "16:50", "18:35", "20:00"];

export function sharedFree(
  mine: Group,
  friend: Group,
  today: ISODate,
  now: Date,
  semesterStart: ISODate,
  f: Filters,
): { day: ISODate; time: string }[] {
  const result: { day: ISODate; time: string }[] = [];
  const nowM = minutesOf(now);
  const none: Filters = { hidden: new Set(), changes: null };
  for (let offset = 0; offset <= 2; offset++) {
    const day = addDays(today, offset);
    if (dow(day) === 7 || holidayOn(day)) continue;
    const mineBusy = new Set(shownLessons(mine, day, semesterStart, "auto", f).map((l) => l.start));
    const friendBusy = new Set(shownLessons(friend, day, semesterStart, "auto", none).map((l) => l.start));
    for (const start of PAIR_STARTS) {
      if (offset === 0 && toMinutes(start)! <= nowM) continue;
      if (!mineBusy.has(start) && !friendBusy.has(start)) result.push({ day, time: start });
    }
    if (result.length >= 3) break;
  }
  return result.slice(0, 3);
}

const TEACHER_NAME = /([А-ЯЁA-Z][а-яёa-z-]+)((?:\s+[А-ЯЁA-Z]?\.){1,2})/g;

/** Must stay identical to splitTeachers() in Android StudentExtras.kt and scripts/teachers.mjs: ratings are keyed by these names. */
export function splitTeachers(raw: string): string[] {
  const found = [...raw.matchAll(TEACHER_NAME)].map((match) => {
    const initials = match[2].split(".").map((part) => part.trim()).filter(Boolean);
    return [match[1], ...initials.map((letter) => `${letter}.`)].join(" ");
  });
  const unique = [...new Set(found)];
  if (unique.length) return unique;
  const trimmed = raw.trim();
  return trimmed ? [trimmed] : [];
}

export function teacherHeading(raw: string): string {
  if (new RegExp(TEACHER_NAME.source).test(raw)) return splitTeachers(raw).join("\n");
  const people = raw.split(/[,;/]|\s+и\s+/);
  const formatted = people.map(formatTeacher).filter(Boolean).join("\n");
  return formatted || raw.trim();
}

function formatTeacher(raw: string): string {
  const words = raw.trim().split(/\s+/).filter((w) => /\p{L}/u.test(w));
  if (!words.length) return "";
  const surname = words[0].replace(/^\.+|\.+$/g, "");
  const letters = words.slice(1).flatMap((word) => {
    const core = [...word].filter((c) => /\p{L}/u.test(c));
    if (!core.length) return [];
    if (core.length > 2) return [core[0].toUpperCase()];
    return core.map((c) => c.toUpperCase());
  });
  return letters.length ? `${surname} ${letters.map((l) => `${l}.`).join(" ")}` : surname;
}

let cached: Promise<Bundle> | null = null;

export function loadBundle(): Promise<Bundle> {
  cached ??= fetch("/schedule.json")
    .then((response) => {
      if (!response.ok) throw new Error(`schedule ${response.status}`);
      return response.json();
    })
    .then(parseBundle)
    .catch((error) => {
      cached = null;
      throw error;
    });
  return cached;
}
