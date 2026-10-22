"use client";

import { useSyncExternalStore } from "react";
import type { DayChange } from "./schedule";

export interface Absence {
  date: string;
  key: string;
  subject: string;
}

export interface TodoItem {
  id: string;
  title: string;
  deadlineDate: string | null;
  deadlineTime: string | null;
  lessonKey: string | null;
  lessonLabel: string | null;
  done: boolean;
  remind: string;
}

export interface Data {
  lang: "ru" | "en" | null;
  dark: boolean;
  onboarded: boolean;
  group: string | null;
  name: string;
  friend: string | null;
  /** Keyed by `${date}|${lessonKey}`, same as the Android backup. */
  notes: Record<string, string>;
  subjectNotes: Record<string, string>;
  hidden: string[];
  absences: Absence[];
  dayChanges: DayChange[];
  absenceLimits: Record<string, number>;
  favoriteTeachers: string[];
  todos: TodoItem[];
  fileTags: Record<string, string[]>;
  systemTheme: boolean;
  hideProfanity: boolean;
  chatPush: boolean;
  /** normal | quiet | session | off — see lib/notify.ts */
  notifyProfile: "normal" | "quiet" | "session" | "off";
  digestOn: boolean;
  /** Local hour 18..23 for the evening digest. */
  digestHour: number;
  /** Last app version whose "What's new" screen was shown. */
  seenVersion: string | null;
  /** The one-time "sign in" suggestion was shown. */
  loginOffered: boolean;
}

const DEFAULTS: Data = {
  lang: null,
  dark: false,
  onboarded: false,
  group: null,
  name: "",
  friend: null,
  notes: {},
  subjectNotes: {},
  hidden: [],
  absences: [],
  dayChanges: [],
  absenceLimits: {},
  favoriteTeachers: [],
  todos: [],
  fileTags: {},
  systemTheme: false,
  hideProfanity: false,
  chatPush: false,
  notifyProfile: "normal",
  digestOn: false,
  digestHour: 21,
  seenVersion: null,
  loginOffered: false,
};

export const PREFIX = "studrasp.";
const KEYS = Object.keys(DEFAULTS) as (keyof Data)[];
const KEPT_ON_RESET: (keyof Data)[] = ["lang", "dark", "onboarded", "systemTheme", "seenVersion"];

let state: Data = DEFAULTS;
let loaded = false;
const listeners = new Set<() => void>();

function load() {
  if (loaded || typeof window === "undefined") return;
  loaded = true;
  const next = { ...DEFAULTS } as Record<keyof Data, unknown>;
  for (const key of KEYS) {
    try {
      const raw = localStorage.getItem(PREFIX + key);
      if (raw != null) next[key] = JSON.parse(raw);
    } catch {}
  }
  state = next as Data;
}

function persist(key: keyof Data, value: unknown) {
  try {
    if (value === DEFAULTS[key]) localStorage.removeItem(PREFIX + key);
    else localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {}
}

export function getState(): Data {
  load();
  return state;
}

export function setState(patch: Partial<Data> | ((d: Data) => Partial<Data>)) {
  const current = getState();
  const changes = typeof patch === "function" ? patch(current) : patch;
  state = { ...current, ...changes };
  for (const key of Object.keys(changes) as (keyof Data)[]) persist(key, state[key]);
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Runs `listener` after every data change; returns the unsubscribe function. */
export function onDataChange(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function useStore(): Data {
  return useSyncExternalStore(subscribe, getState, () => DEFAULTS);
}

export function resetData() {
  const patch: Partial<Data> = {};
  for (const key of KEYS) {
    if (!KEPT_ON_RESET.includes(key)) (patch as Record<string, unknown>)[key] = DEFAULTS[key];
  }
  setState(patch);
}

const noteId = (date: string, key: string) => `${date}|${key}`;

export const actions = {
  setNote(date: string, key: string, value: string) {
    setState((d) => {
      const notes = { ...d.notes };
      if (value.trim()) notes[noteId(date, key)] = value;
      else delete notes[noteId(date, key)];
      return { notes };
    });
  },
  setSubjectNote(subject: string, value: string) {
    setState((d) => {
      const subjectNotes = { ...d.subjectNotes };
      if (value.trim()) subjectNotes[subject] = value;
      else delete subjectNotes[subject];
      return { subjectNotes };
    });
  },
  setHidden(key: string, hidden: boolean) {
    setState((d) => ({ hidden: hidden ? [...new Set([...d.hidden, key])] : d.hidden.filter((k) => k !== key) }));
  },
  toggleAbsence(date: string, key: string, subject: string) {
    setState((d) => {
      const has = d.absences.some((a) => a.date === date && a.key === key);
      const rest = d.absences.filter((a) => !(a.date === date && a.key === key));
      return { absences: has ? rest : [...rest, { date, key, subject }] };
    });
  },
  setDayChange(date: string, key: string, cancelled: boolean, room: string | null) {
    setState((d) => ({
      dayChanges: [
        ...d.dayChanges.filter((c) => !(c.date === date && c.key === key)),
        { date, key, cancelled, room: room?.trim() ?? "" },
      ],
    }));
  },
  clearDayChange(date: string, key: string) {
    setState((d) => ({ dayChanges: d.dayChanges.filter((c) => !(c.date === date && c.key === key)) }));
  },
  setLimit(subject: string, value: number | null) {
    setState((d) => {
      const absenceLimits = { ...d.absenceLimits };
      if (value == null || value < 0) delete absenceLimits[subject];
      else absenceLimits[subject] = value;
      return { absenceLimits };
    });
  },
  setFavorite(name: string, favorite: boolean) {
    const key = name.trim();
    if (!key) return;
    setState((d) => ({
      favoriteTeachers: favorite ? [...new Set([...d.favoriteTeachers, key])] : d.favoriteTeachers.filter((n) => n !== key),
    }));
  },
  upsertTodo(item: TodoItem) {
    setState((d) => ({ todos: [...d.todos.filter((t) => t.id !== item.id), item] }));
  },
  removeTodo(id: string) {
    setState((d) => ({ todos: d.todos.filter((t) => t.id !== id) }));
  },
  linkFile(name: string, key: string, attached: boolean) {
    setState((d) => {
      const current = new Set(d.fileTags[name] ?? []);
      if (attached) current.add(key);
      else current.delete(key);
      return { fileTags: { ...d.fileTags, [name]: [...current] } };
    });
  },
  forgetFile(name: string) {
    setState((d) => {
      const fileTags = { ...d.fileTags };
      delete fileTags[name];
      return { fileTags };
    });
  },
};

export function noteOf(data: Data, date: string, key: string): string {
  return data.notes[noteId(date, key)] ?? "";
}

export function deadlineAt(item: TodoItem): Date | null {
  if (!item.deadlineDate || !/^\d{4}-\d{2}-\d{2}$/.test(item.deadlineDate)) return null;
  const [y, m, d] = item.deadlineDate.split("-").map(Number);
  const time = /^(\d{2}):(\d{2})$/.exec(item.deadlineTime ?? "");
  return time ? new Date(y, m - 1, d, Number(time[1]), Number(time[2])) : new Date(y, m - 1, d, 23, 59);
}

export function isOverdue(item: TodoItem, now: Date): boolean {
  if (item.done) return false;
  const at = deadlineAt(item);
  return at != null && at < now;
}

export function sortedTodos(items: TodoItem[]): TodoItem[] {
  const cmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
  return [...items].sort(
    (a, b) =>
      Number(a.done) - Number(b.done) ||
      Number(a.deadlineDate == null) - Number(b.deadlineDate == null) ||
      cmp(a.deadlineDate ?? "9999-99-99", b.deadlineDate ?? "9999-99-99") ||
      cmp(a.deadlineTime ?? "99:99", b.deadlineTime ?? "99:99") ||
      cmp(a.title, b.title),
  );
}

export function newId(): string {
  const c: Crypto = globalThis.crypto;
  if (typeof c.randomUUID === "function") return c.randomUUID();
  const bytes = c.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((b) => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
