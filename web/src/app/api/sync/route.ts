import { ApiError, ensureSchema, sql } from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/** Must match the sections in web/src/lib/sync.ts and data/Sync.kt. */
const SECTIONS = new Set([
  "fav", "profile", "notes", "subjectNotes", "todos", "absences", "limits", "hidden", "dayChanges", "settings",
]);
const MAX_CHANGES = 2000;
const MAX_ITEMS = 20000;
const MAX_VALUE = 4000;

export function OPTIONS() {
  return preflight();
}

interface Change {
  s: string;
  k: string;
  v: unknown;
  t: number;
}

function changesOf(value: unknown): Change[] {
  if (value == null) return [];
  if (!Array.isArray(value) || value.length > MAX_CHANGES) throw new ApiError(400, "bad_request");
  const unique = new Map<string, Change>();
  for (const raw of value) {
    const item = raw as Record<string, unknown>;
    const s = item?.s;
    const k = item?.k;
    const t = Number(item?.t);
    if (typeof s !== "string" || !SECTIONS.has(s) || typeof k !== "string" || !k || k.length > 300 || !Number.isSafeInteger(t)) {
      throw new ApiError(400, "bad_request");
    }
    const v = item.v ?? null;
    if (v !== null && JSON.stringify(v).length > MAX_VALUE) throw new ApiError(413, "too_large");
    unique.set(`${s}\u0001${k}`, { s, k, v, t });
  }
  return [...unique.values()];
}

/**
 * Body `{ since, changes: [{ s: section, k: key, v: value | null (deleted), t: ms }] }`.
 * Newer `t` wins per item. Returns every item changed after `since` (including the caller's own) and the new cursor.
 */
export const POST = handle(async (request: Request) => {
  const user = await requireUser(request);
  const body = await readBody(request);
  const since = Number(body.since ?? 0);
  if (!Number.isSafeInteger(since) || since < 0) throw new ApiError(400, "bad_request");
  const changes = changesOf(body.changes);
  const userId = String(user.id);
  await ensureSchema();
  const q = sql();
  if (changes.length) {
    const [{ n }] = await q`SELECT count(*)::int AS n FROM user_sync WHERE user_id = ${userId}::uuid`;
    if (Number(n) + changes.length > MAX_ITEMS) throw new ApiError(413, "too_large");
    await q`
      INSERT INTO user_sync (user_id, section, item, value, updated_at)
      SELECT ${userId}::uuid, c.s, c.k, c.v, c.t
      FROM jsonb_to_recordset(${JSON.stringify(changes)}::jsonb) AS c(s text, k text, v jsonb, t bigint)
      ON CONFLICT (user_id, section, item) DO UPDATE
        SET value = EXCLUDED.value, updated_at = EXCLUDED.updated_at, rev = nextval('user_sync_rev')
        WHERE user_sync.updated_at <= EXCLUDED.updated_at`;
  }
  const rows = await q`
    SELECT section, item, value, updated_at, rev FROM user_sync
    WHERE user_id = ${userId}::uuid AND rev > ${since} ORDER BY rev`;
  if (Math.random() < 0.02) {
    await q`DELETE FROM user_sync WHERE value IS NULL AND updated_at < ${Date.now() - 90 * 86400_000}`;
  }
  const cursor = rows.reduce((max, row) => Math.max(max, Number(row.rev)), since);
  return json({
    items: rows.map((row) => ({ s: String(row.section), k: String(row.item), v: row.value ?? null, t: Number(row.updated_at) })),
    cursor,
  });
});
