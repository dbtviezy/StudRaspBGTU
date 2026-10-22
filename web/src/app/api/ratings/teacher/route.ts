import {
  deviceHash,
  ensureSchema,
  hideUnclaimedReviews,
  legacyUntil,
  sql,
  teacherSummary,
  toReview,
  validDeviceId,
} from "@/lib/server/db";
import { userFromRequest } from "@/lib/server/auth";
import { fail, json, preflight } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/**
 * `mine` is the signed-in account's review, or an old review left from this device without an account
 * (`claimable: true`; it is hidden after `legacyUntil` unless someone signs in).
 */
export async function GET(request: Request) {
  try {
    const name = (new URL(request.url).searchParams.get("name") ?? "").trim();
    if (!name) return json({ error: "unknown_teacher" }, 400);
    await ensureSchema();
    await hideUnclaimedReviews();
    const q = sql();
    const deviceId = request.headers.get("x-device-id");
    const hash = validDeviceId(deviceId) ? deviceHash(deviceId) : null;
    const user = await userFromRequest(request);
    const userId = user ? String(user.id) : null;
    const [summary, rows, mineRows] = await Promise.all([
      teacherSummary(name),
      q`SELECT r.id, r.overall, r.delivery, r.strictness, r.loyalty, r.text, r.updated_at,
          count(v.*) FILTER (WHERE v.value = 1)::int AS likes,
          count(v.*) FILTER (WHERE v.value = -1)::int AS dislikes,
          max(CASE WHEN (${userId}::uuid IS NOT NULL AND v.user_id = ${userId}::uuid)
            OR (${userId}::uuid IS NULL AND v.device_hash = ${hash}) THEN v.value END)::int AS my_vote,
          bool_or((${userId}::uuid IS NOT NULL AND r.user_id = ${userId}::uuid)
            OR (r.user_id IS NULL AND NOT r.kept AND r.device_hash = ${hash})) AS is_mine
        FROM teacher_reviews r
        LEFT JOIN review_votes v ON v.review_id = r.id
        WHERE r.teacher = ${name} AND NOT r.hidden
        GROUP BY r.id
        ORDER BY (count(v.*) FILTER (WHERE v.value = 1) - count(v.*) FILTER (WHERE v.value = -1)) DESC, r.updated_at DESC
        LIMIT 50`,
      q`SELECT r.id, r.overall, r.delivery, r.strictness, r.loyalty, r.text, r.updated_at, r.user_id IS NULL AS claimable,
          r.hidden,
          (SELECT count(*) FROM review_votes v WHERE v.review_id = r.id AND v.value = 1)::int AS likes,
          (SELECT count(*) FROM review_votes v WHERE v.review_id = r.id AND v.value = -1)::int AS dislikes
        FROM teacher_reviews r
        WHERE r.teacher = ${name}
          AND ((${userId}::uuid IS NOT NULL AND r.user_id = ${userId}::uuid)
            OR (r.user_id IS NULL AND NOT r.kept AND r.device_hash = ${hash}))
        ORDER BY (r.user_id IS NULL) LIMIT 1`,
    ]);
    const mine = mineRows[0];
    return json({
      summary,
      reviews: rows
        .filter((row) => !row.is_mine)
        .map((row) => ({ ...toReview(row), myVote: Number(row.my_vote ?? 0) })),
      mine: mine ? { ...toReview(mine), claimable: Boolean(mine.claimable), hidden: Boolean(mine.hidden) } : null,
      legacyUntil: legacyUntil(),
    });
  } catch (error) {
    return fail(error);
  }
}
