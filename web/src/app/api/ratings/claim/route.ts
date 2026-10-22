import { ApiError, deviceHash, ensureSchema, legacyUntil, sql, validDeviceId } from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** Header `X-Device-Id`: `{ count, until }`, old reviews left from this device that no account owns yet. */
export const GET = handle(async (request: Request) => {
  const deviceId = request.headers.get("x-device-id");
  if (!validDeviceId(deviceId)) throw new ApiError(400, "invalid_device");
  await ensureSchema();
  const rows = await sql()`SELECT count(*)::int AS n FROM teacher_reviews
    WHERE device_hash = ${deviceHash(deviceId)} AND user_id IS NULL AND NOT kept`;
  return json({ count: Number(rows[0]?.n ?? 0), until: legacyUntil() });
});

/**
 * Body `{ deviceId }`: moves this device's old reviews, votes and reports to the signed-in account.
 * If the account already reviewed the same teacher, the newer review stays.
 */
export const POST = handle(async (request: Request) => {
  const body = await readBody(request);
  if (!validDeviceId(body.deviceId)) throw new ApiError(400, "invalid_device");
  const user = await requireUser(request);
  const userId = String(user.id);
  const hash = deviceHash(body.deviceId);
  const q = sql();
  const legacy = await q`SELECT id, teacher, updated_at FROM teacher_reviews
    WHERE device_hash = ${hash} AND user_id IS NULL AND NOT kept`;
  let claimed = 0;
  for (const row of legacy) {
    const own = await q`SELECT id, updated_at FROM teacher_reviews WHERE teacher = ${String(row.teacher)} AND user_id = ${userId}::uuid`;
    if (own.length && new Date(own[0].updated_at as string) >= new Date(row.updated_at as string)) {
      await q`DELETE FROM teacher_reviews WHERE id = ${Number(row.id)}`;
      continue;
    }
    if (own.length) await q`DELETE FROM teacher_reviews WHERE id = ${Number(own[0].id)}`;
    await q`UPDATE teacher_reviews SET user_id = ${userId}::uuid, hidden = hidden AND NOT unclaimed, unclaimed = false
      WHERE id = ${Number(row.id)}`;
    claimed++;
  }
  await q`UPDATE review_votes v SET user_id = ${userId}::uuid
    WHERE v.device_hash = ${hash} AND v.user_id IS NULL
      AND NOT EXISTS (SELECT 1 FROM review_votes o WHERE o.review_id = v.review_id AND o.user_id = ${userId}::uuid)`;
  await q`UPDATE reports SET user_id = ${userId}::uuid WHERE device_hash = ${hash} AND user_id IS NULL`;
  return json({ claimed });
});
