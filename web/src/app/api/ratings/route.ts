import {
  ApiError,
  assertAllowed,
  assertCanReview,
  checkRateLimit,
  cleanText,
  deviceHash,
  ensureSchema,
  hideUnclaimedReviews,
  isKnownTeacher,
  sql,
  teacherSummary,
  toSummary,
  validDeviceId,
} from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { fail, json, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const SCORES = ["overall", "delivery", "strictness", "loyalty"] as const;

export function OPTIONS() {
  return preflight();
}

export async function GET() {
  try {
    await ensureSchema();
    await hideUnclaimedReviews();
    const rows = await sql()`
      SELECT teacher, count(*)::int AS count,
        round(avg(overall), 1)::float8 AS overall,
        round(avg(delivery), 1)::float8 AS delivery,
        round(avg(strictness), 1)::float8 AS strictness,
        round(avg(loyalty), 1)::float8 AS loyalty
      FROM teacher_reviews WHERE NOT hidden GROUP BY teacher ORDER BY teacher`;
    return json({ items: rows.map(toSummary) }, 200, {
      "Cache-Control": "public, s-maxage=60, stale-while-revalidate=300",
    });
  } catch (error) {
    return fail(error);
  }
}

function teacherOf(body: Record<string, unknown>): string {
  const teacher = typeof body.teacher === "string" ? body.teacher.trim() : "";
  if (!teacher || !isKnownTeacher(teacher)) throw new ApiError(400, "unknown_teacher");
  return teacher;
}

function deviceOf(body: Record<string, unknown>): string {
  if (!validDeviceId(body.deviceId)) throw new ApiError(400, "invalid_device");
  return body.deviceId;
}

/**
 * One review per account and per device for each teacher. An old device-only review on this device is taken over
 * by the account; a review left from this device by another account blocks a new one (`device_used`).
 */
export async function POST(request: Request) {
  try {
    const body = await readBody(request);
    const deviceId = deviceOf(body);
    const scores = SCORES.map((key) => {
      const value = body[key];
      if (typeof value !== "number" || !Number.isInteger(value) || value < 1 || value > 5) {
        throw new ApiError(400, "invalid_score");
      }
      return value;
    });
    const text = cleanText(body.text, 300);
    const teacher = teacherOf(body);
    const hash = deviceHash(deviceId);
    const user = await requireUser(request);
    const userId = String(user.id);
    assertCanReview(user);
    await assertAllowed("reviews", hash, userId);
    await checkRateLimit(hash);
    const [overall, delivery, strictness, loyalty] = scores;
    const q = sql();
    const own = await q`SELECT id FROM teacher_reviews WHERE teacher = ${teacher} AND user_id = ${userId}::uuid`;
    let id = own.length ? Number(own[0].id) : null;
    if (id == null) {
      const byDevice = await q`SELECT id, user_id FROM teacher_reviews WHERE teacher = ${teacher} AND device_hash = ${hash}`;
      if (byDevice.length && byDevice[0].user_id != null) throw new ApiError(409, "device_used");
      id = byDevice.length ? Number(byDevice[0].id) : null;
    }
    if (id == null) {
      await q`
        INSERT INTO teacher_reviews (teacher, device_hash, user_id, overall, delivery, strictness, loyalty, text)
        VALUES (${teacher}, ${hash}, ${userId}::uuid, ${overall}, ${delivery}, ${strictness}, ${loyalty}, ${text})`;
    } else {
      await q`
        UPDATE teacher_reviews SET user_id = ${userId}::uuid, overall = ${overall}, delivery = ${delivery},
          strictness = ${strictness}, loyalty = ${loyalty}, text = ${text}, updated_at = now(),
          hidden = hidden AND NOT unclaimed, unclaimed = false, kept = false
        WHERE id = ${id}`;
    }
    return json({ ok: true, summary: await teacherSummary(teacher) });
  } catch (error) {
    return fail(error);
  }
}

export async function DELETE(request: Request) {
  try {
    const body = await readBody(request);
    const deviceId = deviceOf(body);
    const teacher = teacherOf(body);
    const hash = deviceHash(deviceId);
    const user = await requireUser(request);
    await checkRateLimit(hash);
    await sql()`DELETE FROM teacher_reviews WHERE teacher = ${teacher}
      AND (user_id = ${String(user.id)}::uuid OR (user_id IS NULL AND NOT kept AND device_hash = ${hash}))`;
    return json({ ok: true, summary: await teacherSummary(teacher) });
  } catch (error) {
    return fail(error);
  }
}
