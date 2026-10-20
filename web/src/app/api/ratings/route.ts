import {
  ApiError,
  assertNotBanned,
  checkRateLimit,
  cleanText,
  deviceHash,
  ensureSchema,
  isKnownTeacher,
  sql,
  teacherSummary,
  toSummary,
  validDeviceId,
} from "@/lib/server/db";
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
    await ensureSchema();
    await assertNotBanned(hash);
    await checkRateLimit(hash);
    const [overall, delivery, strictness, loyalty] = scores;
    await sql()`
      INSERT INTO teacher_reviews (teacher, device_hash, overall, delivery, strictness, loyalty, text)
      VALUES (${teacher}, ${hash}, ${overall}, ${delivery}, ${strictness}, ${loyalty}, ${text})
      ON CONFLICT (teacher, device_hash) DO UPDATE SET
        overall = EXCLUDED.overall,
        delivery = EXCLUDED.delivery,
        strictness = EXCLUDED.strictness,
        loyalty = EXCLUDED.loyalty,
        text = EXCLUDED.text,
        updated_at = now()`;
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
    await ensureSchema();
    await checkRateLimit(hash);
    await sql()`DELETE FROM teacher_reviews WHERE teacher = ${teacher} AND device_hash = ${hash}`;
    return json({ ok: true, summary: await teacherSummary(teacher) });
  } catch (error) {
    return fail(error);
  }
}
