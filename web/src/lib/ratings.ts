"use client";

import { useEffect, useState } from "react";
import { getAccount } from "./account";
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
  id: number;
  likes: number;
  dislikes: number;
  /** 1, -1 or 0 — this device's vote; absent on the device's own review. */
  myVote?: number;
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
  /** `claimable`: left from this device without an account; `hidden`: hidden for that reason or by a moderator. */
  mine: (Review & { claimable?: boolean; hidden?: boolean }) | null;
  legacyUntil?: string | null;
}

export function summaryFor(summaries: Map<string, Summary>, rawTeacher: string): Summary | undefined {
  return splitTeachers(rawTeacher)
    .map((name) => summaries.get(name))
    .find((summary) => summary && summary.count > 0);
}

export class RatingError extends Error {
  constructor(public code: string, public extra: Record<string, unknown> = {}) {
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

async function call<T>(input: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const token = getAccount().token;
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let response: Response;
  try {
    response = await fetch(input, { ...init, headers });
  } catch {
    throw new RatingError("offline");
  }
  const body = (await response.json().catch(() => null)) as (T & { error?: string }) | null;
  if (!response.ok || !body) {
    const { error, ...extra } = (body ?? {}) as Record<string, unknown>;
    throw new RatingError(typeof error === "string" ? error : "server_error", extra);
  }
  return body;
}

/** Old reviews left from this device without an account, and when they get hidden. */
export async function claimableReviews(): Promise<{ count: number; until: string | null }> {
  return call("/api/ratings/claim", { headers: { "X-Device-Id": deviceId() }, cache: "no-store" });
}

/** Moves this device's old reviews, votes and reports to the signed-in account. */
export async function claimDeviceReviews(): Promise<number> {
  const body = await call<{ claimed: number }>("/api/ratings/claim", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ deviceId: deviceId() }),
  });
  return body.claimed;
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

export async function voteReview(reviewId: number, value: number): Promise<{ likes: number; dislikes: number; myVote: number }> {
  return call("/api/ratings/vote", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ deviceId: deviceId(), reviewId, value }),
  });
}

export async function reportReview(reviewId: number, reason: string): Promise<void> {
  await call("/api/reports", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ deviceId: deviceId(), kind: "review", target: reviewId, reason }),
  });
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
