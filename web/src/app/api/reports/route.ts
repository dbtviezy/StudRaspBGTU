import { ApiError, assertAllowed, checkRateLimit, cleanText, deviceHash, ensureSchema, sql, validDeviceId } from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";
import { chatModerationEnabled, supabaseAdmin } from "@/lib/server/supabase";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/**
 * Body: `{ deviceId, kind: "review", target: reviewId, reason }` or
 * `{ deviceId, kind: "message", target: messageId, reason }`. Both need a signed-in user.
 */
export const POST = handle(async (request: Request) => {
  const body = await readBody(request);
  if (!validDeviceId(body.deviceId)) throw new ApiError(400, "invalid_device");
  const kind = body.kind;
  const target = Number(body.target);
  if ((kind !== "review" && kind !== "message") || !Number.isSafeInteger(target) || target <= 0) throw new ApiError(400, "bad_request");
  const reason = cleanText(body.reason, 200);
  const hash = deviceHash(body.deviceId);
  const user = await requireUser(request);
  const userId = String(user.id);
  await ensureSchema();
  await assertAllowed("reports", hash, userId);
  await checkRateLimit(`report:${hash}`, 20);
  let snapshot = "";
  if (kind === "review") {
    const rows = await sql()`SELECT teacher, text FROM teacher_reviews WHERE id = ${target}`;
    if (!rows.length) throw new ApiError(404, "not_found");
    snapshot = `${rows[0].teacher}: ${rows[0].text}`.slice(0, 1000);
  } else {
    if (chatModerationEnabled()) {
      const rows = await supabaseAdmin<{ author: string; body: string }[]>(`messages?id=eq.${target}&select=author,body`);
      if (!rows?.length) throw new ApiError(404, "not_found");
      snapshot = `${rows[0].author}: ${rows[0].body}`.slice(0, 1000);
    }
  }
  await sql()`
    INSERT INTO reports (kind, target, snapshot, reason, device_hash, user_id)
    VALUES (${kind}, ${String(target)}, ${snapshot}, ${reason}, ${hash}, ${userId}::uuid)
    ON CONFLICT (kind, target, device_hash) DO UPDATE SET reason = EXCLUDED.reason, user_id = EXCLUDED.user_id,
      resolved = false, created_at = now()`;
  return json({ ok: true });
});
