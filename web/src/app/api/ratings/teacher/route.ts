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
    const [summary, rows, mineRows] = await Promise.all([
      teacherSummary(name),
      q`SELECT overall, delivery, strictness, loyalty, text, updated_at
        FROM teacher_reviews WHERE teacher = ${name}
        ORDER BY updated_at DESC LIMIT 50`,
      validDeviceId(deviceId)
        ? q`SELECT overall, delivery, strictness, loyalty, text, updated_at
            FROM teacher_reviews WHERE teacher = ${name} AND device_hash = ${deviceHash(deviceId)}`
        : Promise.resolve([]),
    ]);
    return json({
      summary,
      reviews: rows.map(toReview),
      mine: mineRows.length ? toReview(mineRows[0]) : null,
    });
  } catch (error) {
    return fail(error);
  }
}
