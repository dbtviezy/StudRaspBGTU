import { neon, type NeonQueryFunction } from "@neondatabase/serverless";
import { createHash } from "node:crypto";
import teachers from "@/data/teachers.json";

export class ApiError extends Error {
  constructor(public status: number, public code: string, public extra: Record<string, unknown> = {}) {
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
    await q`ALTER TABLE teacher_reviews ADD COLUMN IF NOT EXISTS hidden boolean NOT NULL DEFAULT false`;
    await q`CREATE TABLE IF NOT EXISTS review_votes (
      review_id bigint NOT NULL REFERENCES teacher_reviews (id) ON DELETE CASCADE,
      device_hash text NOT NULL,
      value smallint NOT NULL CHECK (value IN (-1, 1)),
      created_at timestamptz NOT NULL DEFAULT now(),
      PRIMARY KEY (review_id, device_hash)
    )`;
    await q`CREATE TABLE IF NOT EXISTS reports (
      id bigserial PRIMARY KEY,
      kind text NOT NULL CHECK (kind IN ('review', 'message')),
      target text NOT NULL,
      snapshot varchar(1000) NOT NULL DEFAULT '',
      reason varchar(200) NOT NULL DEFAULT '',
      device_hash text NOT NULL,
      resolved boolean NOT NULL DEFAULT false,
      created_at timestamptz NOT NULL DEFAULT now(),
      UNIQUE (kind, target, device_hash)
    )`;
    await q`CREATE INDEX IF NOT EXISTS reports_open_idx ON reports (resolved, created_at)`;
    await q`CREATE TABLE IF NOT EXISTS users (
      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      login text UNIQUE,
      password_hash text,
      name varchar(40) NOT NULL,
      telegram_id bigint UNIQUE,
      google_sub text UNIQUE,
      created_at timestamptz NOT NULL DEFAULT now(),
      seen_at timestamptz NOT NULL DEFAULT now()
    )`;
    await q`CREATE TABLE IF NOT EXISTS sessions (
      token_hash text PRIMARY KEY,
      user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
      created_at timestamptz NOT NULL DEFAULT now(),
      expires_at timestamptz NOT NULL
    )`;
    await q`CREATE INDEX IF NOT EXISTS sessions_user_idx ON sessions (user_id)`;
    await q`CREATE TABLE IF NOT EXISTS auth_codes (
      code_hash text PRIMARY KEY,
      user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
      expires_at timestamptz NOT NULL
    )`;
    await q`CREATE TABLE IF NOT EXISTS bans (
      id bigserial PRIMARY KEY,
      user_id uuid REFERENCES users (id) ON DELETE CASCADE,
      device_hash text,
      reason varchar(200) NOT NULL DEFAULT '',
      created_at timestamptz NOT NULL DEFAULT now(),
      until timestamptz,
      CHECK (user_id IS NOT NULL OR device_hash IS NOT NULL)
    )`;
    await q`CREATE TABLE IF NOT EXISTS announcements (
      id bigserial PRIMARY KEY,
      text varchar(500) NOT NULL,
      created_at timestamptz NOT NULL DEFAULT now(),
      until timestamptz,
      active boolean NOT NULL DEFAULT true
    )`;
    await q`CREATE TABLE IF NOT EXISTS admin_log (
      id bigserial PRIMARY KEY,
      action text NOT NULL,
      target text NOT NULL DEFAULT '',
      created_at timestamptz NOT NULL DEFAULT now()
    )`;
    await q`ALTER TABLE admin_log ADD COLUMN IF NOT EXISTS detail varchar(300) NOT NULL DEFAULT ''`;
    await q`CREATE TABLE IF NOT EXISTS push_subscriptions (
      endpoint text PRIMARY KEY,
      user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
      p256dh text NOT NULL,
      auth text NOT NULL,
      hide_profanity boolean NOT NULL DEFAULT false,
      lang varchar(2) NOT NULL DEFAULT 'ru',
      created_at timestamptz NOT NULL DEFAULT now()
    )`;
    await q`ALTER TABLE push_subscriptions ADD COLUMN IF NOT EXISTS notify_profile varchar(16) NOT NULL DEFAULT 'normal'`;
    await q`ALTER TABLE push_subscriptions ADD COLUMN IF NOT EXISTS digest_on boolean NOT NULL DEFAULT false`;
    await q`ALTER TABLE push_subscriptions ADD COLUMN IF NOT EXISTS digest_hour int NOT NULL DEFAULT 21`;
    await q`ALTER TABLE push_subscriptions ADD COLUMN IF NOT EXISTS group_id varchar(40) NOT NULL DEFAULT ''`;
    await q`ALTER TABLE push_subscriptions ADD COLUMN IF NOT EXISTS time_zone varchar(64) NOT NULL DEFAULT 'Europe/Moscow'`;
    await q`CREATE INDEX IF NOT EXISTS push_subscriptions_user_idx ON push_subscriptions (user_id)`;
    // v1.2: reviews, votes and reports belong to accounts; `kept` marks reviews left anonymous on account deletion,
    // `unclaimed` marks old device-only reviews hidden after LEGACY_REVIEWS_UNTIL.
    await q`ALTER TABLE teacher_reviews ADD COLUMN IF NOT EXISTS user_id uuid REFERENCES users (id) ON DELETE CASCADE`;
    await q`ALTER TABLE teacher_reviews ADD COLUMN IF NOT EXISTS kept boolean NOT NULL DEFAULT false`;
    await q`ALTER TABLE teacher_reviews ADD COLUMN IF NOT EXISTS unclaimed boolean NOT NULL DEFAULT false`;
    await q`CREATE UNIQUE INDEX IF NOT EXISTS teacher_reviews_user_idx ON teacher_reviews (teacher, user_id) WHERE user_id IS NOT NULL`;
    await q`ALTER TABLE review_votes ADD COLUMN IF NOT EXISTS user_id uuid REFERENCES users (id) ON DELETE CASCADE`;
    await q`ALTER TABLE reports ADD COLUMN IF NOT EXISTS user_id uuid REFERENCES users (id) ON DELETE SET NULL`;
    await q`ALTER TABLE bans ADD COLUMN IF NOT EXISTS scope text NOT NULL DEFAULT 'all'`;
    await q`ALTER TABLE sessions ADD COLUMN IF NOT EXISTS id bigserial`;
    await q`ALTER TABLE sessions ADD COLUMN IF NOT EXISTS device varchar(80) NOT NULL DEFAULT ''`;
    await q`ALTER TABLE sessions ADD COLUMN IF NOT EXISTS used_at timestamptz NOT NULL DEFAULT now()`;
    await q`CREATE TABLE IF NOT EXISTS rep_requests (
      id bigserial PRIMARY KEY,
      user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
      group_id varchar(40) NOT NULL,
      group_name varchar(80) NOT NULL DEFAULT '',
      comment varchar(300) NOT NULL DEFAULT '',
      status text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'revoked')),
      created_at timestamptz NOT NULL DEFAULT now(),
      decided_at timestamptz
    )`;
    await q`CREATE INDEX IF NOT EXISTS rep_requests_user_idx ON rep_requests (user_id, created_at)`;
    await q`CREATE SEQUENCE IF NOT EXISTS user_sync_rev`;
    await q`CREATE TABLE IF NOT EXISTS user_sync (
      user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
      section varchar(20) NOT NULL,
      item varchar(300) NOT NULL,
      value jsonb,
      updated_at bigint NOT NULL,
      rev bigint NOT NULL DEFAULT nextval('user_sync_rev'),
      PRIMARY KEY (user_id, section, item)
    )`;
    await q`CREATE INDEX IF NOT EXISTS user_sync_rev_idx ON user_sync (user_id, rev)`;
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
  id: number;
  likes: number;
  dislikes: number;
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
    id: Number(row.id),
    likes: Number(row.likes ?? 0),
    dislikes: Number(row.dislikes ?? 0),
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
    FROM teacher_reviews WHERE teacher = ${teacher} AND NOT hidden GROUP BY teacher`;
  return rows.length ? toSummary(rows[0]) : null;
}

/** `bucket` is a device hash or any other opaque key, e.g. `login:<hash>`. */
export async function checkRateLimit(bucket: string, limit = 10): Promise<void> {
  const q = sql();
  const rows = await q`
    SELECT count(*)::int AS n FROM rate_events
    WHERE device_hash = ${bucket} AND created_at > now() - interval '1 hour'`;
  if (Number(rows[0]?.n ?? 0) >= limit) throw new ApiError(429, "rate_limited");
  await q`INSERT INTO rate_events (device_hash) VALUES (${bucket})`;
  if (Math.random() < 0.05) await q`DELETE FROM rate_events WHERE created_at < now() - interval '1 day'`;
}

/** `all` locks the account out completely; the others block one kind of action. */
export const BAN_SCOPES = ["all", "reviews", "votes", "reports", "chat", "chat_create"] as const;
export type BanScope = (typeof BAN_SCOPES)[number];

export interface Restriction {
  scope: BanScope;
  until: string | null;
  reason: string;
}

function toRestriction(row: Row): Restriction {
  return {
    scope: String(row.scope) as BanScope,
    until: row.until ? new Date(row.until as string).toISOString() : null,
    reason: String(row.reason ?? ""),
  };
}

/** Active restrictions, one per scope (the longest wins). */
export async function activeRestrictions(deviceHashValue: string | null, userId: string | null): Promise<Restriction[]> {
  const rows = await sql()`
    SELECT DISTINCT ON (scope) scope, until, reason FROM bans
    WHERE (until IS NULL OR until > now())
      AND ((${deviceHashValue}::text IS NOT NULL AND device_hash = ${deviceHashValue})
        OR (${userId}::uuid IS NOT NULL AND user_id = ${userId}::uuid))
    ORDER BY scope, until DESC NULLS FIRST`;
  return rows.map(toRestriction);
}

/** Throws 403 `banned` (full ban) or `restricted` (this action only) with `scope`, `until` and `reason`. */
export async function assertAllowed(scope: BanScope, deviceHashValue: string | null, userId: string | null): Promise<void> {
  const found = (await activeRestrictions(deviceHashValue, userId))
    .filter((item) => item.scope === "all" || item.scope === scope)
    .sort((a, b) => (a.scope === "all" ? -1 : b.scope === "all" ? 1 : 0))[0];
  if (found) throw new ApiError(403, found.scope === "all" ? "banned" : "restricted", { ...found });
}

export async function assertNotBanned(deviceHashValue: string | null, userId: string | null = null): Promise<void> {
  await assertAllowed("all", deviceHashValue, userId);
}

const DAY_MS = 86400_000;

/** Reviews need a Telegram or Google sign-in and an account older than a day. */
export function assertCanReview(user: Row): void {
  if (user.telegram_id == null && user.google_sub == null) throw new ApiError(403, "social_required");
  const ready = new Date(user.created_at as string).getTime() + DAY_MS;
  if (ready > Date.now()) throw new ApiError(403, "account_too_new", { until: new Date(ready).toISOString() });
}

/** After this date old reviews that nobody signed in to claim are hidden. Unset = never. */
export function legacyUntil(): string | null {
  const value = process.env.LEGACY_REVIEWS_UNTIL?.trim();
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

let unclaimedCheckedAt = 0;

export async function hideUnclaimedReviews(): Promise<void> {
  const until = legacyUntil();
  if (!until || Date.parse(until) > Date.now() || Date.now() - unclaimedCheckedAt < 600_000) return;
  unclaimedCheckedAt = Date.now();
  await sql()`UPDATE teacher_reviews SET hidden = true, unclaimed = true
    WHERE user_id IS NULL AND NOT kept AND NOT hidden`;
}

export function cleanText(value: unknown, max: number): string {
  if (value == null) return "";
  if (typeof value !== "string") throw new ApiError(400, "bad_request");
  const text = value.replace(/\r\n?/g, "\n").trim().replace(/\n{3,}/g, "\n\n");
  if (Array.from(text).length > max) throw new ApiError(400, "text_too_long");
  return text;
}
