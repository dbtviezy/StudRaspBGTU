import { neon, type NeonQueryFunction } from "@neondatabase/serverless";
import { createHash } from "node:crypto";
import teachers from "@/data/teachers.json";

export class ApiError extends Error {
  constructor(public status: number, public code: string) {
    super(code);
  }
}

let client: NeonQueryFunction<false, false> | null = null;
let ready: Promise<void> | null = null;
let known: Set<string> | null = null;

export function sql(): NeonQueryFunction<false, false> {
  const url = process.env.DATABASE_URL;
  if (!url) throw new ApiError(503, "db_unavailable");
  client ??= neon(url);
  return client;
}

export function ensureSchema(): Promise<void> {
  ready ??= (async () => {
    const q = sql();
    await q`CREATE TABLE IF NOT EXISTS teacher_reviews (
      id bigserial PRIMARY KEY,
      teacher text NOT NULL,
      device_hash text NOT NULL,
      overall smallint NOT NULL CHECK (overall BETWEEN 1 AND 5),
      delivery smallint NOT NULL CHECK (delivery BETWEEN 1 AND 5),
      strictness smallint NOT NULL CHECK (strictness BETWEEN 1 AND 5),
      loyalty smallint NOT NULL CHECK (loyalty BETWEEN 1 AND 5),
      text varchar(300) NOT NULL DEFAULT '',
      created_at timestamptz NOT NULL DEFAULT now(),
      updated_at timestamptz NOT NULL DEFAULT now(),
      UNIQUE (teacher, device_hash)
    )`;
    await q`CREATE INDEX IF NOT EXISTS teacher_reviews_teacher_idx ON teacher_reviews (teacher)`;
    await q`CREATE TABLE IF NOT EXISTS rate_events (
      id bigserial PRIMARY KEY,
      device_hash text NOT NULL,
      created_at timestamptz NOT NULL DEFAULT now()
    )`;
    await q`CREATE INDEX IF NOT EXISTS rate_events_device_idx ON rate_events (device_hash, created_at)`;
  })().catch((error) => {
    ready = null;
    throw error;
  });
  return ready;
}

export function isKnownTeacher(name: string): boolean {
  known ??= new Set(teachers as string[]);
  return known.has(name);
}

export function deviceHash(deviceId: string): string {
  const salt = process.env.RATING_SALT;
  if (!salt) throw new ApiError(503, "not_configured");
  return createHash("sha256").update(deviceId + salt).digest("hex");
}

export function validDeviceId(value: unknown): value is string {
  return typeof value === "string" && /^[A-Za-z0-9-]{16,64}$/.test(value);
}

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

type Row = Record<string, unknown>;

export function toSummary(row: Row): Summary {
  return {
    teacher: String(row.teacher),
    count: Number(row.count),
    overall: Number(row.overall),
    delivery: Number(row.delivery),
    strictness: Number(row.strictness),
    loyalty: Number(row.loyalty),
  };
}

export function toReview(row: Row): Review {
  return {
    overall: Number(row.overall),
    delivery: Number(row.delivery),
    strictness: Number(row.strictness),
    loyalty: Number(row.loyalty),
    text: String(row.text ?? ""),
    updatedAt: new Date(row.updated_at as string | Date).toISOString(),
  };
}

export async function teacherSummary(teacher: string): Promise<Summary | null> {
  const rows = await sql()`
    SELECT teacher, count(*)::int AS count,
      round(avg(overall), 1)::float8 AS overall,
      round(avg(delivery), 1)::float8 AS delivery,
      round(avg(strictness), 1)::float8 AS strictness,
      round(avg(loyalty), 1)::float8 AS loyalty
    FROM teacher_reviews WHERE teacher = ${teacher} GROUP BY teacher`;
  return rows.length ? toSummary(rows[0]) : null;
}

export async function checkRateLimit(hash: string): Promise<void> {
  const q = sql();
  const rows = await q`
    SELECT count(*)::int AS n FROM rate_events
    WHERE device_hash = ${hash} AND created_at > now() - interval '1 hour'`;
  if (Number(rows[0]?.n ?? 0) >= 10) throw new ApiError(429, "rate_limited");
  await q`INSERT INTO rate_events (device_hash) VALUES (${hash})`;
  if (Math.random() < 0.05) await q`DELETE FROM rate_events WHERE created_at < now() - interval '1 day'`;
}
