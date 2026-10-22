import { ApiError, sql } from "@/lib/server/db";
import { requireUser, sessionHash } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** Devices signed in to this account: `{ items: [{ id, device, createdAt, usedAt, current }] }`. */
export const GET = handle(async (request: Request) => {
  const user = await requireUser(request);
  const rows = await sql()`
    SELECT id, device, created_at, used_at, token_hash = ${sessionHash(request)} AS current
    FROM sessions WHERE user_id = ${String(user.id)}::uuid AND expires_at > now()
    ORDER BY used_at DESC LIMIT 50`;
  return json({
    items: rows.map((row) => ({
      id: Number(row.id),
      device: String(row.device ?? ""),
      createdAt: new Date(row.created_at as string).toISOString(),
      usedAt: new Date(row.used_at as string).toISOString(),
      current: Boolean(row.current),
    })),
  });
});

/** Body `{ id }`: signs that device out. */
export const DELETE = handle(async (request: Request) => {
  const user = await requireUser(request);
  const body = await readBody(request);
  const id = Number(body.id);
  if (!Number.isSafeInteger(id) || id <= 0) throw new ApiError(400, "bad_request");
  await sql()`DELETE FROM sessions WHERE id = ${id} AND user_id = ${String(user.id)}::uuid`;
  return json({ ok: true });
});

/** Same as DELETE for clients that cannot send a body with DELETE. */
export const POST = DELETE;
