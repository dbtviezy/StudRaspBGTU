import { ApiError, assertAllowed, checkRateLimit, deviceHash, ensureSchema, sql, validDeviceId } from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** Body: `{ deviceId, reviewId, value }`, value 1 = like, -1 = dislike, 0 = remove the vote. One vote per account and device. */
export const POST = handle(async (request: Request) => {
  const body = await readBody(request);
  if (!validDeviceId(body.deviceId)) throw new ApiError(400, "invalid_device");
  const reviewId = Number(body.reviewId);
  const value = Number(body.value);
  if (!Number.isSafeInteger(reviewId) || reviewId <= 0 || ![1, -1, 0].includes(value)) throw new ApiError(400, "bad_request");
  const hash = deviceHash(body.deviceId);
  const user = await requireUser(request);
  const userId = String(user.id);
  await ensureSchema();
  await assertAllowed("votes", hash, userId);
  await checkRateLimit(`vote:${hash}`, 60);
  const q = sql();
  const review = await q`SELECT device_hash, user_id FROM teacher_reviews WHERE id = ${reviewId} AND NOT hidden`;
  if (!review.length) throw new ApiError(404, "not_found");
  if (review[0].device_hash === hash || String(review[0].user_id ?? "") === userId) throw new ApiError(400, "own_review");
  await q`DELETE FROM review_votes WHERE review_id = ${reviewId} AND user_id = ${userId}::uuid AND device_hash <> ${hash}`;
  if (value === 0) {
    await q`DELETE FROM review_votes WHERE review_id = ${reviewId} AND device_hash = ${hash}`;
  } else {
    await q`INSERT INTO review_votes (review_id, device_hash, user_id, value) VALUES (${reviewId}, ${hash}, ${userId}::uuid, ${value})
      ON CONFLICT (review_id, device_hash) DO UPDATE SET value = EXCLUDED.value, user_id = EXCLUDED.user_id, created_at = now()`;
  }
  const rows = await q`
    SELECT count(*) FILTER (WHERE value = 1)::int AS likes, count(*) FILTER (WHERE value = -1)::int AS dislikes
    FROM review_votes WHERE review_id = ${reviewId}`;
  return json({ likes: Number(rows[0].likes), dislikes: Number(rows[0].dislikes), myVote: value });
});
