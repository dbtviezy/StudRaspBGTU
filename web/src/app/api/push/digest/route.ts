import { timingSafeEqual } from "node:crypto";
import { addDays, isoOf } from "@/lib/dates";
import { ApiError, ensureSchema, sql } from "@/lib/server/db";
import { handle, json } from "@/lib/server/http";
import { pushKeys, sendPush } from "@/lib/server/push";
import { holidayOn, lessonsOn, parseBundle } from "@/lib/schedule";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export const maxDuration = 60;

function secretOk(request: Request): boolean {
  const expected = process.env.CRON_SECRET?.trim() || process.env.PUSH_WEBHOOK_SECRET?.trim();
  const bearer = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "").trim() ?? "";
  const header = request.headers.get("x-push-secret")?.trim() ?? "";
  const given = bearer || header;
  if (!expected || given.length !== expected.length) return false;
  return timingSafeEqual(Buffer.from(given), Buffer.from(expected));
}

function hourInZone(timeZone: string): number {
  try {
    const parts = new Intl.DateTimeFormat("en-GB", {
      timeZone: timeZone || "Europe/Moscow",
      hour: "numeric",
      hour12: false,
    }).formatToParts(new Date());
    return Number(parts.find((p) => p.type === "hour")?.value ?? -1);
  } catch {
    return -1;
  }
}

function tomorrowInZone(timeZone: string): string {
  try {
    const parts = new Intl.DateTimeFormat("en-CA", {
      timeZone: timeZone || "Europe/Moscow",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).formatToParts(new Date());
    const y = parts.find((p) => p.type === "year")?.value;
    const m = parts.find((p) => p.type === "month")?.value;
    const d = parts.find((p) => p.type === "day")?.value;
    if (!y || !m || !d) return isoOf(new Date());
    return addDays(`${y}-${m}-${d}`, 1);
  } catch {
    return addDays(isoOf(new Date()), 1);
  }
}

function digestText(lang: string, count: number, time: string, room: string): { title: string; body: string } {
  const where = room.trim() ? `${time}, ${room.trim()}` : time;
  if (lang === "en") {
    const word = count === 1 ? "class" : "classes";
    return { title: "Tomorrow's schedule", body: `Tomorrow: ${count} ${word}. First at ${where}.` };
  }
  const word =
    count % 100 >= 11 && count % 100 <= 14
      ? "пар"
      : count % 10 === 1
        ? "пара"
        : count % 10 >= 2 && count % 10 <= 4
          ? "пары"
          : "пар";
  return { title: "Завтрашнее расписание", body: `Завтра ${count} ${word}. Первая в ${where}.` };
}

/** Hourly cron: evening digest for subscriptions whose local hour matches digest_hour. */
export const GET = handle(async (request: Request) => {
  if (!secretOk(request)) throw new ApiError(401, "unauthorized");
  const keys = pushKeys();
  if (!keys) return json({ sent: 0, skipped: "push_not_configured" });

  let bundle;
  try {
    const response = await fetch(new URL("/schedule.json", request.url), { next: { revalidate: 3600 } });
    if (!response.ok) throw new Error(String(response.status));
    bundle = parseBundle(await response.json());
  } catch {
    return json({ sent: 0, skipped: "schedule_unavailable" });
  }

  await ensureSchema();
  const rows = await sql()`
    SELECT endpoint, p256dh, auth, lang, digest_hour, group_id, time_zone
    FROM push_subscriptions
    WHERE digest_on = true AND notify_profile = 'normal' AND group_id <> ''`;

  let sent = 0;
  for (const row of rows) {
    const tz = String(row.time_zone || "Europe/Moscow");
    const hour = hourInZone(tz);
    if (hour !== Number(row.digest_hour)) continue;
    const groupId = String(row.group_id || "");
    const found = bundle.find(groupId);
    if (!found) continue;
    const tomorrow = tomorrowInZone(tz);
    if (holidayOn(tomorrow)) continue;
    const lessons = lessonsOn(found.group, tomorrow, bundle.semesterStart, "auto");
    if (!lessons.length) continue;
    const first = lessons[0];
    const time = first.time?.trim() || first.start;
    const { title, body } = digestText(String(row.lang || "ru"), lessons.length, time, first.room || "");
    const alive = await sendPush(
      keys,
      { endpoint: String(row.endpoint), p256dh: String(row.p256dh), auth: String(row.auth) },
      { title, body, url: "/" },
    );
    if (!alive) {
      await sql()`DELETE FROM push_subscriptions WHERE endpoint = ${String(row.endpoint)}`;
    } else {
      sent += 1;
    }
  }
  return json({ sent });
});

export const POST = GET;
