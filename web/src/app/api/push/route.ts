import { ApiError, ensureSchema, sql } from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";
import { pushKeys } from "@/lib/server/push";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const MAX_DEVICES = 10;

export function OPTIONS() {
  return preflight();
}

export const GET = handle(async () => json({ publicKey: pushKeys()?.publicKey ?? null }));

function cleanEndpoint(value: unknown): string {
  const endpoint = typeof value === "string" ? value.trim() : "";
  if (!/^https:\/\/\S+$/.test(endpoint) || endpoint.length > 1000) throw new ApiError(400, "bad_request");
  return endpoint;
}

function cleanKey(value: unknown): string {
  const key = typeof value === "string" ? value.trim() : "";
  if (!/^[A-Za-z0-9_\-=]{8,200}$/.test(key)) throw new ApiError(400, "bad_request");
  return key;
}

/** Body: `{ endpoint, keys: { p256dh, auth }, hideProfanity, lang }` from PushSubscription.toJSON(). */
export const POST = handle(async (request: Request) => {
  if (!pushKeys()) throw new ApiError(503, "push_not_configured");
  const user = await requireUser(request);
  const body = await readBody(request);
  const keys = (body.keys ?? {}) as Record<string, unknown>;
  const endpoint = cleanEndpoint(body.endpoint);
  const p256dh = cleanKey(keys.p256dh);
  const auth = cleanKey(keys.auth);
  const hide = body.hideProfanity === true;
  const lang = body.lang === "en" ? "en" : "ru";
  const profile =
    body.notifyProfile === "quiet" || body.notifyProfile === "session" || body.notifyProfile === "off" || body.notifyProfile === "normal"
      ? body.notifyProfile
      : "normal";
  const digestOn = body.digestOn === true && profile === "normal";
  const digestHour = Math.min(23, Math.max(18, Math.round(Number(body.digestHour) || 21)));
  const groupId = typeof body.groupId === "string" ? body.groupId.trim().slice(0, 40) : "";
  const timeZone = typeof body.timeZone === "string" && body.timeZone.length <= 64 ? body.timeZone.trim() : "Europe/Moscow";
  const userId = String(user.id);
  await ensureSchema();
  const q = sql();
  await q`
    INSERT INTO push_subscriptions (endpoint, user_id, p256dh, auth, hide_profanity, lang, notify_profile, digest_on, digest_hour, group_id, time_zone)
    VALUES (${endpoint}, ${userId}::uuid, ${p256dh}, ${auth}, ${hide}, ${lang}, ${profile}, ${digestOn}, ${digestHour}, ${groupId}, ${timeZone})
    ON CONFLICT (endpoint) DO UPDATE SET user_id = EXCLUDED.user_id, p256dh = EXCLUDED.p256dh, auth = EXCLUDED.auth,
      hide_profanity = EXCLUDED.hide_profanity, lang = EXCLUDED.lang, notify_profile = EXCLUDED.notify_profile,
      digest_on = EXCLUDED.digest_on, digest_hour = EXCLUDED.digest_hour, group_id = EXCLUDED.group_id, time_zone = EXCLUDED.time_zone`;
  await q`
    DELETE FROM push_subscriptions WHERE user_id = ${userId}::uuid AND endpoint NOT IN (
      SELECT endpoint FROM push_subscriptions WHERE user_id = ${userId}::uuid ORDER BY created_at DESC LIMIT ${MAX_DEVICES}
    )`;
  return json({ ok: true });
});

/** Body: `{ endpoint }`. Works without a session so sign-out can clean up first. */
export const DELETE = handle(async (request: Request) => {
  const body = await readBody(request);
  await ensureSchema();
  await sql()`DELETE FROM push_subscriptions WHERE endpoint = ${cleanEndpoint(body.endpoint)}`;
  return json({ ok: true });
});
