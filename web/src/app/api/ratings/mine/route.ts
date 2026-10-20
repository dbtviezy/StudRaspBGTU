import { sql, toReview } from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { handle, json, preflight } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** The signed-in account's reviews: `{ items: [Review & { teacher, hidden }] }`. */
export const GET = handle(async (request: Request) => {
  const user = await requireUser(request);
  const rows = await sql()`
    SELECT r.id, r.teacher, r.overall, r.delivery, r.strictness, r.loyalty, r.text, r.updated_at, r.hidden,
      (SELECT count(*) FROM review_votes v WHERE v.review_id = r.id AND v.value = 1)::int AS likes,
      (SELECT count(*) FROM review_votes v WHERE v.review_id = r.id AND v.value = -1)::int AS dislikes
    FROM teacher_reviews r WHERE r.user_id = ${String(user.id)}::uuid
    ORDER BY r.updated_at DESC LIMIT 200`;
  return json({
    items: rows.map((row) => ({ ...toReview(row), teacher: String(row.teacher), hidden: Boolean(row.hidden) })),
  });
});
