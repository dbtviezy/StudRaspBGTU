"use client";

import { createContext, useContext } from "react";
import type { ISODate } from "@/lib/dates";
import type { Copy } from "@/lib/i18n";
import type { Bundle, Faculty, Filters, Group, Lesson } from "@/lib/schedule";
import type { Data, TodoItem } from "@/lib/store";

export interface TodoPreset {
  initial?: TodoItem | null;
  lesson?: Lesson | null;
  date?: string | null;
  time?: string | null;
}

export interface Shell {
  bundle: Bundle;
  s: Copy;
  data: Data;
  now: Date;
  today: ISODate;
  faculty: Faculty;
  group: Group;
  filters: Filters;
  selectedDate: ISODate;
  setSelectedDate: (date: ISODate) => void;
  filter: string;
  setFilter: (filter: string) => void;
  setTab: (tab: number) => void;
  openLesson: (lesson: Lesson, date: ISODate) => void;
  openTeacher: (name: string) => void;
  openTasks: () => void;
  openFiles: () => void;
  openReminders: () => void;
  editTodo: (preset: TodoPreset) => void;
  toast: (text: string) => void;
  setLanguage: (code: "ru" | "en") => void;
  pickGroup: () => void;
  pickFriend: () => void;
  onReset: () => void;
}

export const ShellContext = createContext<Shell | null>(null);

export function useShell(): Shell {
  const shell = useContext(ShellContext);
  if (!shell) throw new Error("ShellContext missing");
  return shell;
}
