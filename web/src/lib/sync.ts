"use client";

import { useSyncExternalStore } from "react";
import { api, getAccount } from "./account";
import { getState, PREFIX, setState, type Data, type TodoItem } from "./store";

/**
 * Account sync of user data. The wire format (sections, keys, values) must match data/Sync.kt on Android:
 * fav: name → true · profile: group | friend | name → string · notes: "date|lessonKey" → text ·
 * subjectNotes: subject → text · todos: id → todo · absences: "date|lessonKey" → subject · limits: subject → number ·
 * hidden: lessonKey → true · dayChanges: "date|lessonKey" → { cancelled, room } · settings: key → value.
 * A null value deletes the item. The newest change of an item wins on the server.
 */

type Section = "fav" | "profile" | "notes" | "subjectNotes" | "todos" | "absences" | "limits" | "hidden" | "dayChanges" | "settings";
interface Item {
  s: Section;
  k: string;
  v: unknown;
  t?: number;
}

/** Settings this platform has; others (Android reminders) are kept on the server untouched. */
const OWN_SETTINGS = new Set(["dark", "systemTheme", "lang", "hideProfanity", "notifyProfile", "digest", "digestHour"]);
const DATA_SECTIONS: Section[] = ["fav", "notes", "subjectNotes", "todos", "absences", "limits", "hidden", "dayChanges"];
const SEP = "\u0001";
const STATE_KEY = `${PREFIX}sync`;

export type SyncChoice = "merge" | "account" | "device";

interface Saved {
  userId: string;
  cursor: number;
  /** JSON of every item as last agreed with the server. */
  snapshot: Record<string, string>;
  at: string | null;
}

interface Status {
  busy: boolean;
  asking: boolean;
  at: string | null;
  failed: boolean;
}

let status: Status = { busy: false, asking: false, at: null, failed: false };
const listeners = new Set<() => void>();
let resolveAsk: ((choice: SyncChoice) => void) | null = null;
let running: Promise<void> | null = null;
let again = false;
let applying = false;

function setStatus(patch: Partial<Status>) {
  status = { ...status, ...patch };
  listeners.forEach((listener) => listener());
}

export function useSyncStatus(): Status {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => status,
    () => status,
  );
}

function loadSaved(userId: string): Saved | null {
  try {
    const saved = JSON.parse(localStorage.getItem(STATE_KEY) ?? "null") as Saved | null;
    return saved?.userId === userId ? saved : null;
  } catch {
    return null;
  }
}

function save(saved: Saved | null) {
  try {
    if (saved) localStorage.setItem(STATE_KEY, JSON.stringify(saved));
    else localStorage.removeItem(STATE_KEY);
  } catch {}
}

/** Forgets what was synced, e.g. after signing out, so the next sign-in starts fresh. */
export function forgetSync() {
  save(null);
  setStatus({ at: null, failed: false });
}

/** JSON with sorted object keys, so equal values from the server and from this device compare equal. */
function stable(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(stable).join(",")}]`;
  if (value && typeof value === "object") {
    const record = value as Record<string, unknown>;
    return `{${Object.keys(record).sort().map((key) => `${JSON.stringify(key)}:${stable(record[key])}`).join(",")}}`;
  }
  return JSON.stringify(value ?? null);
}

const keyOf = (s: string, k: string) => `${s}${SEP}${k}`;

function todoValue(item: TodoItem) {
  return {
    title: item.title,
    deadlineDate: item.deadlineDate,
    deadlineTime: item.deadlineTime,
    lessonKey: item.lessonKey,
    lessonLabel: item.lessonLabel,
    done: item.done,
    remind: item.remind,
  };
}

function collect(d: Data): Map<string, string> {
  const out = new Map<string, string>();
  const put = (s: Section, k: string, v: unknown) => {
    if (k) out.set(keyOf(s, k), stable(v));
  };
  d.favoriteTeachers.forEach((name) => put("fav", name, true));
  if (d.group) put("profile", "group", d.group);
  if (d.friend) put("profile", "friend", d.friend);
  if (d.name) put("profile", "name", d.name);
  Object.entries(d.notes).forEach(([k, v]) => v.trim() && put("notes", k, v));
  Object.entries(d.subjectNotes).forEach(([k, v]) => v.trim() && put("subjectNotes", k, v));
  d.todos.forEach((item) => put("todos", item.id, todoValue(item)));
  d.absences.forEach((a) => put("absences", `${a.date}|${a.key}`, a.subject));
  Object.entries(d.absenceLimits).forEach(([k, v]) => put("limits", k, v));
  d.hidden.forEach((k) => put("hidden", k, true));
  d.dayChanges.forEach((c) => put("dayChanges", `${c.date}|${c.key}`, { cancelled: c.cancelled, room: c.room ?? "" }));
  if (!d.systemTheme) put("settings", "dark", d.dark);
  put("settings", "systemTheme", d.systemTheme);
  if (d.lang) put("settings", "lang", d.lang);
  put("settings", "hideProfanity", d.hideProfanity);
  put("settings", "notifyProfile", d.notifyProfile);
  put("settings", "digest", d.digestOn);
  put("settings", "digestHour", d.digestHour);
  return out;
}

const ownsKey = (key: string) => {
  const [s, k] = key.split(SEP);
  return s !== "settings" || OWN_SETTINGS.has(k);
};

function split(key: string): [Section, string] {
  const at = key.indexOf(SEP);
  return [key.slice(0, at) as Section, key.slice(at + 1)];
}

function splitDateKey(k: string): [string, string] {
  const at = k.indexOf("|");
  return [k.slice(0, at), k.slice(at + 1)];
}

/** Applies server items to local data. `replace` first clears the synced data sections. */
function apply(items: Item[], replace = false) {
  if (!items.length && !replace) return;
  applying = true;
  try {
    setState((d) => {
      const fav = new Set(replace ? [] : d.favoriteTeachers);
      const notes = replace ? {} : { ...d.notes };
      const subjectNotes = replace ? {} : { ...d.subjectNotes };
      const todos = new Map((replace ? [] : d.todos).map((t) => [t.id, t]));
      const absences = new Map((replace ? [] : d.absences).map((a) => [`${a.date}|${a.key}`, a]));
      const limits = replace ? {} : { ...d.absenceLimits };
      const hidden = new Set(replace ? [] : d.hidden);
      const changes = new Map((replace ? [] : d.dayChanges).map((c) => [`${c.date}|${c.key}`, c]));
      const patch: Partial<Data> = {};
      for (const { s, k, v } of items) {
        const gone = v == null;
        switch (s) {
          case "fav":
            if (gone) fav.delete(k);
            else fav.add(k);
            break;
          case "profile":
            if (k === "group") patch.group = gone ? null : String(v);
            else if (k === "friend") patch.friend = gone ? null : String(v);
            else if (k === "name") patch.name = gone ? "" : String(v);
            break;
          case "notes":
            if (gone) delete notes[k];
            else notes[k] = String(v);
            break;
          case "subjectNotes":
            if (gone) delete subjectNotes[k];
            else subjectNotes[k] = String(v);
            break;
          case "todos": {
            if (gone) {
              todos.delete(k);
              break;
            }
            const t = v as Partial<TodoItem>;
            todos.set(k, {
              id: k,
              title: String(t.title ?? ""),
              deadlineDate: t.deadlineDate ?? null,
              deadlineTime: t.deadlineTime ?? null,
              lessonKey: t.lessonKey ?? null,
              lessonLabel: t.lessonLabel ?? null,
              done: Boolean(t.done),
              remind: String(t.remind ?? ""),
            });
            break;
          }
          case "absences": {
            if (gone) {
              absences.delete(k);
              break;
            }
            const [date, key] = splitDateKey(k);
            absences.set(k, { date, key, subject: String(v) });
            break;
          }
          case "limits":
            if (gone) delete limits[k];
            else limits[k] = Number(v);
            break;
          case "hidden":
            if (gone) hidden.delete(k);
            else hidden.add(k);
            break;
          case "dayChanges": {
            if (gone) {
              changes.delete(k);
              break;
            }
            const [date, key] = splitDateKey(k);
            const c = v as { cancelled?: boolean; room?: string };
            changes.set(k, { date, key, cancelled: Boolean(c.cancelled), room: String(c.room ?? "") });
            break;
          }
          case "settings":
            if (gone) break;
            if (k === "dark") patch.dark = Boolean(v);
            else if (k === "systemTheme") patch.systemTheme = Boolean(v);
            else if (k === "lang" && (v === "ru" || v === "en")) patch.lang = v;
            else if (k === "hideProfanity") patch.hideProfanity = Boolean(v);
            else if (k === "notifyProfile" && (v === "normal" || v === "quiet" || v === "session" || v === "off")) {
              patch.notifyProfile = v;
              if (v !== "normal" && v !== "quiet") patch.chatPush = false;
            } else if (k === "digest") patch.digestOn = Boolean(v);
            else if (k === "digestHour" && typeof v === "number") patch.digestHour = Math.min(23, Math.max(18, Math.round(v)));
            break;
        }
      }
      return {
        ...patch,
        favoriteTeachers: [...fav],
        notes,
        subjectNotes,
        todos: [...todos.values()],
        absences: [...absences.values()],
        absenceLimits: limits,
        hidden: [...hidden],
        dayChanges: [...changes.values()],
      };
    });
  } finally {
    applying = false;
  }
}

interface Reply {
  items: Item[];
  cursor: number;
}

function post(since: number, changes: Item[]): Promise<Reply> {
  return api<Reply>("/api/sync", { method: "POST", body: JSON.stringify({ since, changes }) });
}

function snapshotOf(base: Record<string, string>, items: Item[]): Record<string, string> {
  const next = { ...base };
  for (const { s, k, v } of items) {
    if (v == null) delete next[keyOf(s, k)];
    else next[keyOf(s, k)] = stable(v);
  }
  return next;
}

const hasData = (keys: Iterable<string>) => [...keys].some((key) => DATA_SECTIONS.includes(split(key)[0]));

async function firstSync(userId: string) {
  const first = await post(0, []);
  const server = first.items.filter((item) => item.v != null);
  const serverKeys = new Map(server.map((item) => [keyOf(item.s, item.k), stable(item.v)]));
  const local = collect(getState());
  const differs = [...local].some(([key, value]) => DATA_SECTIONS.includes(split(key)[0]) && serverKeys.get(key) !== value)
    || [...serverKeys].some(([key, value]) => DATA_SECTIONS.includes(split(key)[0]) && local.get(key) !== value);
  let choice: SyncChoice = "merge";
  if (hasData(local.keys()) && hasData(serverKeys.keys()) && differs) {
    setStatus({ asking: true });
    choice = await new Promise<SyncChoice>((resolve) => {
      resolveAsk = resolve;
    });
    setStatus({ asking: false });
  }
  const now = Date.now();
  let changes: Item[] = [];
  if (choice === "account") {
    apply(server, true);
  } else {
    changes = [...local].map(([key, value]) => {
      const [s, k] = split(key);
      return { s, k, v: JSON.parse(value), t: now };
    });
    if (choice === "device") {
      for (const key of serverKeys.keys()) {
        if (!local.has(key) && ownsKey(key)) {
          const [s, k] = split(key);
          changes.push({ s, k, v: null, t: now });
        }
      }
    }
  }
  const reply = await post(first.cursor, changes);
  const fromServer = choice === "merge" ? [...server.filter((item) => !local.has(keyOf(item.s, item.k))), ...reply.items] : reply.items;
  apply(fromServer);
  const base = snapshotOf(Object.fromEntries(serverKeys), [...changes, ...reply.items]);
  const at = new Date().toISOString();
  save({ userId, cursor: reply.cursor, snapshot: base, at });
  setStatus({ at, failed: false });
}

async function regularSync(saved: Saved) {
  const current = collect(getState());
  const now = Date.now();
  const changes: Item[] = [];
  for (const [key, value] of current) {
    if (saved.snapshot[key] !== value) {
      const [s, k] = split(key);
      changes.push({ s, k, v: JSON.parse(value), t: now });
    }
  }
  for (const key of Object.keys(saved.snapshot)) {
    if (!current.has(key) && ownsKey(key)) {
      const [s, k] = split(key);
      changes.push({ s, k, v: null, t: now });
    }
  }
  const reply = await post(saved.cursor, changes);
  apply(reply.items.filter((item) => {
    const mine = changes.find((c) => c.s === item.s && c.k === item.k);
    return !mine || stable(mine.v) !== stable(item.v);
  }));
  const at = new Date().toISOString();
  save({ userId: saved.userId, cursor: reply.cursor, snapshot: snapshotOf(saved.snapshot, [...changes, ...reply.items]), at });
  setStatus({ at, failed: false });
}

async function runSync() {
  const { user, token } = getAccount();
  if (!user || !token) return;
  const saved = loadSaved(user.id);
  if (saved) await regularSync(saved);
  else await firstSync(user.id);
}

/** Syncs now (or right after the running sync). Errors are kept in the status, never thrown. */
export function syncNow(): Promise<void> {
  if (running) {
    again = true;
    return running;
  }
  setStatus({ busy: true });
  running = (async () => {
    try {
      do {
        again = false;
        await runSync();
      } while (again);
    } catch {
      setStatus({ failed: true });
    } finally {
      running = null;
      setStatus({ busy: false });
    }
  })();
  return running;
}

export function answerSyncQuestion(choice: SyncChoice) {
  resolveAsk?.(choice);
  resolveAsk = null;
}

let timer: ReturnType<typeof setTimeout> | null = null;

/** Called on every local data change; syncs a few seconds later. */
export function scheduleSync() {
  if (applying || !getAccount().user) return;
  if (timer) clearTimeout(timer);
  timer = setTimeout(() => {
    timer = null;
    void syncNow();
  }, 4000);
}

export function lastSyncAt(): string | null {
  const user = getAccount().user;
  return user ? loadSaved(user.id)?.at ?? null : null;
}
