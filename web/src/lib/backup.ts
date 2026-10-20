import type { DayChange } from "./schedule";
import { type Absence, type Data, getState, newId, setState, type TodoItem } from "./store";

/** Same JSON layout as the Android Backup.kt (version 2), so copies move between platforms. */
export function exportBackup(fileNames: string[]): string {
  const d = getState();
  const root = {
    version: 2,
    name: d.name.trim(),
    group: d.group ?? "",
    friend: d.friend ?? "",
    notes: Object.fromEntries(Object.entries(d.notes).filter(([, v]) => v.trim()).map(([k, v]) => [k, v.trim()])),
    subjectNotes: Object.fromEntries(Object.entries(d.subjectNotes).filter(([, v]) => v.trim()).map(([k, v]) => [k, v.trim()])),
    hidden: d.hidden,
    absences: d.absences.map((a) => ({ date: a.date, key: a.key, subject: a.subject })),
    dayChanges: d.dayChanges.map((c) => ({ date: c.date, key: c.key, cancelled: c.cancelled, room: c.room ?? "" })),
    absenceLimits: d.absenceLimits,
    favoriteTeachers: d.favoriteTeachers,
    todos: d.todos.map((t) => ({
      id: t.id,
      title: t.title,
      deadlineDate: t.deadlineDate ?? "",
      deadlineTime: t.deadlineTime ?? "",
      lessonKey: t.lessonKey ?? "",
      lessonLabel: t.lessonLabel ?? "",
      done: t.done,
      remind: t.remind,
    })),
    files: fileNames,
  };
  return JSON.stringify(root, null, 2);
}

type Obj = Record<string, unknown>;
const s = (value: unknown) => (typeof value === "string" ? value : value == null ? "" : String(value));
const obj = (value: unknown): Obj => (value && typeof value === "object" && !Array.isArray(value) ? (value as Obj) : {});
const arr = (value: unknown): unknown[] => (Array.isArray(value) ? value : []);
const strMap = (value: unknown) =>
  Object.fromEntries(Object.entries(obj(value)).map(([k, v]) => [k, s(v)]).filter(([k, v]) => k.trim() && v.trim()));

export function importBackup(text: string): { name: string; group: string } {
  const root = JSON.parse(text) as unknown;
  if (!root || typeof root !== "object" || Array.isArray(root)) throw new Error("bad backup");
  const r = root as Obj;
  const name = s(r.name).trim();
  const group = s(r.group);
  const patch: Partial<Data> = {
    name,
    friend: s(r.friend).trim() || null,
    notes: strMap(r.notes),
    subjectNotes: strMap(r.subjectNotes),
    hidden: arr(r.hidden).map(s).filter(Boolean),
    absences: arr(r.absences)
      .map(obj)
      .map((item): Absence => ({ date: s(item.date), key: s(item.key), subject: s(item.subject) }))
      .filter((a) => a.date && a.key),
    dayChanges: arr(r.dayChanges)
      .map(obj)
      .map((item): DayChange => ({ date: s(item.date), key: s(item.key), cancelled: item.cancelled === true, room: s(item.room) }))
      .filter((c) => c.date && c.key),
    absenceLimits: Object.fromEntries(
      Object.entries(obj(r.absenceLimits))
        .map(([k, v]) => [k, Math.trunc(Number(v)) || 0] as const)
        .filter(([, v]) => v >= 0),
    ),
    todos: arr(r.todos)
      .map(obj)
      .filter((item) => s(item.title).trim())
      .map(
        (item): TodoItem => ({
          id: s(item.id) || newId(),
          title: s(item.title),
          deadlineDate: s(item.deadlineDate) || null,
          deadlineTime: s(item.deadlineTime) || null,
          lessonKey: s(item.lessonKey) || null,
          lessonLabel: s(item.lessonLabel) || null,
          done: item.done === true,
          remind: s(item.remind),
        }),
      ),
  };
  if (group.trim()) patch.group = group;
  if ("favoriteTeachers" in r) patch.favoriteTeachers = [...new Set(arr(r.favoriteTeachers).map((n) => s(n).trim()).filter(Boolean))];
  setState(patch);
  return { name, group };
}

export function download(name: string, content: BlobPart, type: string) {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 30_000);
}
