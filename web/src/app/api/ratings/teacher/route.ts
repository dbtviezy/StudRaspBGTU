import { deviceHash, ensureSchema, sql, teacherSummary, toReview, validDeviceId } from "@/lib/server/db";
import { fail, json, preflight } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

export async function GET(request: Request) {
  try {
    const name = (new URL(request.url).searchParams.get("name") ?? "").trim();
    if (!name) return json({ error: "unknown_teacher" }, 400);
    await ensureSchema();
    const q = sql();
    const deviceId = request.headers.get("x-device-id");
    const hash = validDeviceId(deviceId) ? deviceHash(deviceId) : null;
    const [summary, rows, mineRows] = await Promise.all([
      teacherSummary(name),
      q`SELECT r.id, r.overall, r.delivery, r.strictness, r.loyalty, r.text, r.updated_at,
          count(v.*) FILTER (WHERE v.value = 1)::int AS likes,
          count(v.*) FILTER (WHERE v.value = -1)::int AS dislikes,
          max(CASE WHEN v.device_hash = ${hash} THEN v.value END)::int AS my_vote,
          bool_or(r.device_hash = ${hash}) AS is_mine
        FROM teacher_reviews r
        LEFT JOIN review_votes v ON v.review_id = r.id
        WHERE r.teacher = ${name} AND NOT r.hidden
        GROUP BY r.id
        ORDER BY (count(v.*) FILTER (WHERE v.value = 1) - count(v.*) FILTER (WHERE v.value = -1)) DESC, r.updated_at DESC
        LIMIT 50`,
      hash
        ? q`SELECT r.id, r.overall, r.delivery, r.strictness, r.loyalty, r.text, r.updated_at,
              (SELECT count(*) FROM review_votes v WHERE v.review_id = r.id AND v.value = 1)::int AS likes,
              (SELECT count(*) FROM review_votes v WHERE v.review_id = r.id AND v.value = -1)::int AS dislikes
            FROM teacher_reviews r WHERE r.teacher = ${name} AND r.device_hash = ${hash}`
        : Promise.resolve([]),
    ]);
    return json({
      summary,
      reviews: rows
        .filter((row) => !row.is_mine)
        .map((row) => ({ ...toReview(row), myVote: Number(row.my_vote ?? 0) })),
      mine: mineRows.length ? toReview(mineRows[0]) : null,
    });
  } catch (error) {
    return fail(error);
  }
}
