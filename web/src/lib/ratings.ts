"use client";

import { useEffect, useState } from "react";
import { splitTeachers } from "./schedule";
import { newId, PREFIX } from "./store";

export interface Summary {
  teacher: string;
  count: number;
  overall: number;
  delivery: number;
  strictness: number;
  loyalty: number;
}

export interface Review {
  overall: number;
  delivery: number;
  strictness: number;
  loyalty: number;
  text: string;
  updatedAt: string;
}

export interface TeacherRatings {
  summary: Summary | null;
  reviews: Review[];
  mine: Review | null;
}

export function summaryFor(summaries: Map<string, Summary>, rawTeacher: string): Summary | undefined {
  return splitTeachers(rawTeacher)
    .map((name) => summaries.get(name))
    .find((summary) => summary && summary.count > 0);
}

export class RatingError extends Error {
  constructor(public code: string) {
    super(code);
  }
}

const CACHE_KEY = `${PREFIX}ratingsCache`;

export function deviceId(): string {
  const key = `${PREFIX}deviceId`;
  let id = localStorage.getItem(key);
  if (!id) {
    id = newId();
    localStorage.setItem(key, id);
  }
  return id;
}

async function call<T>(input: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(input, init);
  } catch {
    throw new RatingError("offline");
  }
  const body = (await response.json().catch(() => null)) as (T & { error?: string }) | null;
  if (!response.ok || !body) throw new RatingError(body?.error ?? "server_error");
  return body;
}

let summaries: Map<string, Summary> | null = null;
let loading: Promise<void> | null = null;
const listeners = new Set<() => void>();

function publish(items: Summary[]) {
  summaries = new Map(items.map((item) => [item.teacher, item]));
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(items));
  } catch {}
  listeners.forEach((listener) => listener());
}

export function refreshSummaries(): Promise<void> {
  loading ??= call<{ items: Summary[] }>("/api/ratings")
    .then((body) => publish(body.items))
    .catch(() => undefined)
    .finally(() => {
      loading = null;
    });
  return loading;
}

function updateSummary(teacher: string, summary: Summary | null) {
  const items = [...(summaries?.values() ?? [])].filter((item) => item.teacher !== teacher);
  publish(summary ? [...items, summary] : items);
}

export function useSummaries(): Map<string, Summary> {
  const [, setTick] = useState(0);
  useEffect(() => {
    const listener = () => setTick((t) => t + 1);
    listeners.add(listener);
    if (!summaries) {
      try {
        const cached = JSON.parse(localStorage.getItem(CACHE_KEY) ?? "[]") as Summary[];
        summaries = new Map(cached.map((item) => [item.teacher, item]));
        listener();
      } catch {}
      refreshSummaries();
    }
    return () => {
      listeners.delete(listener);
    };
  }, []);
  return summaries ?? new Map();
}

export async function fetchTeacher(name: string): Promise<TeacherRatings> {
  const result = await call<TeacherRatings>(`/api/ratings/teacher?name=${encodeURIComponent(name.trim())}`, {
    headers: { "X-Device-Id": deviceId() },
    cache: "no-store",
  });
  updateSummary(name.trim(), result.summary);
  return result;
}

export interface Scores {
  overall: number;
  delivery: number;
  strictness: number;
  loyalty: number;
}

export async function saveReview(teacher: string, scores: Scores, text: string): Promise<Summary | null> {
  const body = await call<{ ok: true; summary: Summary | null }>("/api/ratings", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ deviceId: deviceId(), teacher: teacher.trim(), ...scores, text }),
  });
  updateSummary(teacher.trim(), body.summary);
  return body.summary;
}

export async function deleteReview(teacher: string): Promise<Summary | null> {
  const body = await call<{ ok: true; summary: Summary | null }>("/api/ratings", {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ deviceId: deviceId(), teacher: teacher.trim() }),
  });
  updateSummary(teacher.trim(), body.summary);
  return body.summary;
}
