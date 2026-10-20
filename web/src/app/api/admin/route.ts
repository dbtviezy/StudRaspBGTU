import { ApiError, cleanText, ensureSchema, sql } from "@/lib/server/db";
import { requireAdmin } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";
import { chatModerationEnabled, supabaseAdmin, supabaseCount } from "@/lib/server/supabase";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type Row = Record<string, unknown>;

export function OPTIONS() {
  return preflight();
}

const iso = (value: unknown) => (value ? new Date(value as string).toISOString() : null);

function idOf(value: unknown): number {
  const id = Number(value);
  if (!Number.isSafeInteger(id) || id <= 0) throw new ApiError(400, "bad_request");
  return id;
}

function uuidOf(value: unknown): string {
  if (typeof value !== "string" || !/^[0-9a-f-]{36}$/i.test(value)) throw new ApiError(400, "bad_request");
  return value;
}

function daysOf(value: unknown): number | null {
  if (value == null || value === 0) return null;
  const days = Number(value);
  if (!Number.isFinite(days) || days < 0 || days > 3650) throw new ApiError(400, "bad_request");
  return days;
}

async function log(action: string, target: unknown, detail: unknown) {
  const text = typeof detail === "string" ? detail.trim().slice(0, 300) : "";
  await sql()`INSERT INTO admin_log (action, target, detail) VALUES (${action}, ${String(target ?? "")}, ${text})`;
}

async function stats() {
  const q = sql();
  const [row] = await q`SELECT
    (SELECT count(*) FROM users)::int AS users,
    (SELECT count(*) FROM users WHERE seen_at > now() - interval '7 days')::int AS active_users,
    (SELECT count(*) FROM teacher_reviews)::int AS reviews,
    (SELECT count(*) FROM teacher_reviews WHERE hidden)::int AS hidden_reviews,
    (SELECT count(*) FROM teacher_reviews WHERE created_at > now() - interval '7 days')::int AS new_reviews,
    (SELECT count(DISTINCT teacher) FROM teacher_reviews)::int AS rated_teachers,
    (SELECT count(*) FROM review_votes)::int AS votes,
    (SELECT count(DISTINCT kind || target) FROM reports WHERE NOT resolved)::int AS open_reports,
    (SELECT count(*) FROM bans WHERE until IS NULL OR until > now())::int AS active_bans,
    (SELECT count(*) FROM announcements WHERE active AND (until IS NULL OR until > now()))::int AS announcements`;
  const [chats, messages] = await Promise.all([supabaseCount("chats?select=id"), supabaseCount("messages?select=id")]);
  return {
    users: row.users,
    activeUsers: row.active_users,
    reviews: row.reviews,
    hiddenReviews: row.hidden_reviews,
    newReviews: row.new_reviews,
    ratedTeachers: row.rated_teachers,
    votes: row.votes,
    openReports: row.open_reports,
    activeBans: row.active_bans,
    announcements: row.announcements,
    chats,
    messages,
    chatEnabled: chatModerationEnabled(),
  };
}

async function reports() {
  const rows = await sql()`
    SELECT kind, target, count(*)::int AS count, max(created_at) AS last_at,
      (array_agg(snapshot ORDER BY created_at DESC))[1] AS snapshot,
      array_remove(array_agg(DISTINCT NULLIF(reason, '')), NULL) AS reasons,
      min(id) AS id
    FROM reports WHERE NOT resolved
    GROUP BY kind, target ORDER BY count(*) DESC, max(created_at) DESC LIMIT 100`;
  return rows.map((row) => ({
    id: Number(row.id),
    kind: String(row.kind),
    target: String(row.target),
    count: Number(row.count),
    lastAt: iso(row.last_at),
    snapshot: String(row.snapshot ?? ""),
    reasons: (row.reasons as string[] | null) ?? [],
  }));
}

async function reviews(url: URL) {
  const query = (url.searchParams.get("q") ?? "").trim();
  const pattern = `%${query}%`;
  const rows = await sql()`
    SELECT r.id, r.teacher, r.overall, r.text, r.hidden, r.updated_at,
      (SELECT count(*) FROM review_votes v WHERE v.review_id = r.id AND v.value = 1)::int AS likes,
      (SELECT count(*) FROM review_votes v WHERE v.review_id = r.id AND v.value = -1)::int AS dislikes,
      (SELECT count(*) FROM reports p WHERE p.kind = 'review' AND p.target = r.id::text AND NOT p.resolved)::int AS reports
    FROM teacher_reviews r
    WHERE ${query}::text = '' OR r.teacher ILIKE ${pattern} OR r.text ILIKE ${pattern}
    ORDER BY r.updated_at DESC LIMIT 100`;
  return rows.map((row) => ({
    id: Number(row.id),
    teacher: String(row.teacher),
    overall: Number(row.overall),
    text: String(row.text),
    hidden: Boolean(row.hidden),
    updatedAt: iso(row.updated_at),
    likes: Number(row.likes),
    dislikes: Number(row.dislikes),
    reports: Number(row.reports),
  }));
}

async function messages() {
  if (!chatModerationEnabled()) return [];
  const rows = await supabaseAdmin<Row[]>(
    "messages?select=id,chat_id,user_id,author,body,created_at,chats(kind,title,group_id)&order=id.desc&limit=100",
  );
  return (rows ?? []).map((row) => {
    const chat = (row.chats ?? {}) as Row;
    return {
      id: Number(row.id),
      chatId: String(row.chat_id),
      userId: String(row.user_id),
      author: String(row.author),
      body: String(row.body),
      createdAt: iso(row.created_at),
      chat: chat.kind === "dm" ? "dm" : String(chat.title || chat.group_id || ""),
    };
  });
}

async function bans() {
  const rows = await sql()`
    SELECT b.id, b.user_id, b.device_hash IS NOT NULL AS device, b.reason, b.created_at, b.until, u.name, u.login
    FROM bans b LEFT JOIN users u ON u.id = b.user_id
    WHERE b.until IS NULL OR b.until > now()
    ORDER BY b.created_at DESC LIMIT 200`;
  return rows.map((row) => ({
    id: Number(row.id),
    userId: row.user_id ? String(row.user_id) : null,
    device: Boolean(row.device),
    name: row.name ? String(row.name) : null,
    login: row.login ? String(row.login) : null,
    reason: String(row.reason),
    createdAt: iso(row.created_at),
    until: iso(row.until),
  }));
}

async function announcements() {
  const rows = await sql()`SELECT id, text, created_at, until, active FROM announcements ORDER BY created_at DESC LIMIT 30`;
  return rows.map((row) => ({
    id: Number(row.id),
    text: String(row.text),
    createdAt: iso(row.created_at),
    until: iso(row.until),
    active: Boolean(row.active) && (!row.until || new Date(row.until as string) > new Date()),
  }));
}

async function users(url: URL) {
  const query = (url.searchParams.get("q") ?? "").trim();
  const pattern = `%${query}%`;
  const rows = await sql()`
    SELECT u.id, u.name, u.login, u.telegram_id IS NOT NULL AS telegram, u.google_sub IS NOT NULL AS google,
      u.created_at, u.seen_at,
      EXISTS (SELECT 1 FROM bans b WHERE b.user_id = u.id AND (b.until IS NULL OR b.until > now())) AS banned
    FROM users u
    WHERE ${query}::text = '' OR u.name ILIKE ${pattern} OR u.login ILIKE ${pattern} OR u.id::text LIKE ${`${query.toLowerCase()}%`}
    ORDER BY u.seen_at DESC LIMIT 100`;
  return rows.map((row) => ({
    id: String(row.id),
    name: String(row.name),
    login: row.login ? String(row.login) : null,
    telegram: Boolean(row.telegram),
    google: Boolean(row.google),
    createdAt: iso(row.created_at),
    seenAt: iso(row.seen_at),
    banned: Boolean(row.banned),
  }));
}

async function moderationLog() {
  const q = sql();
  if (Math.random() < 0.05) await q`DELETE FROM admin_log WHERE created_at < now() - interval '180 days'`;
  const rows = await q`SELECT id, action, target, detail, created_at FROM admin_log ORDER BY id DESC LIMIT 200`;
  return rows.map((row) => ({
    id: Number(row.id),
    action: String(row.action),
    target: String(row.target ?? ""),
    detail: String(row.detail ?? ""),
    createdAt: iso(row.created_at),
  }));
}

export const GET = handle(async (request: Request) => {
  requireAdmin(request);
  await ensureSchema();
  const url = new URL(request.url);
  switch (url.searchParams.get("view")) {
    case "log": return json({ items: await moderationLog() });
    case "stats": return json(await stats());
    case "reports": return json({ items: await reports() });
    case "reviews": return json({ items: await reviews(url) });
    case "messages": return json({ items: await messages() });
    case "bans": return json({ items: await bans() });
    case "announcements": return json({ items: await announcements() });
    case "users": return json({ items: await users(url) });
    default: throw new ApiError(400, "bad_request");
  }
});

async function deleteMessage(id: number) {
  await supabaseAdmin(`messages?id=eq.${id}`, { method: "DELETE" });
}

async function banUser(userId: string, days: number | null, reason: string) {
  const q = sql();
  await q`INSERT INTO bans (user_id, reason, until)
    VALUES (${userId}::uuid, ${reason}, ${days == null ? null : new Date(Date.now() + days * 86400_000).toISOString()}::timestamptz)`;
  await q`DELETE FROM sessions WHERE user_id = ${userId}::uuid`;
  if (chatModerationEnabled()) {
    await supabaseAdmin("chat_bans?on_conflict=user_id", {
      method: "POST",
      headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
      body: JSON.stringify({ user_id: userId, reason, until: days == null ? null : new Date(Date.now() + days * 86400_000).toISOString() }),
    });
  }
}

async function resolveReports(kind: string, target: string) {
  await sql()`UPDATE reports SET resolved = true WHERE kind = ${kind} AND target = ${target}`;
}

export const POST = handle(async (request: Request) => {
  requireAdmin(request);
  await ensureSchema();
  const body = await readBody(request);
  const q = sql();
  const action = String(body.action ?? "");
  switch (action) {
    case "review.hide":
    case "review.unhide": {
      const id = idOf(body.id);
      await q`UPDATE teacher_reviews SET hidden = ${action === "review.hide"} WHERE id = ${id}`;
      if (action === "review.hide") await resolveReports("review", String(id));
      break;
    }
    case "review.delete": {
      const id = idOf(body.id);
      await q`DELETE FROM teacher_reviews WHERE id = ${id}`;
      await resolveReports("review", String(id));
      break;
    }
    case "message.delete": {
      const id = idOf(body.id);
      await deleteMessage(id);
      await resolveReports("message", String(id));
      break;
    }
    case "report.dismiss": {
      await resolveReports(String(body.kind), String(body.target));
      break;
    }
    case "report.accept": {
      const target = idOf(body.target);
      if (body.kind === "review") await q`UPDATE teacher_reviews SET hidden = true WHERE id = ${target}`;
      else if (body.kind === "message") await deleteMessage(target);
      else throw new ApiError(400, "bad_request");
      await resolveReports(String(body.kind), String(target));
      break;
    }
    case "ban.review": {
      const id = idOf(body.id);
      const rows = await q`SELECT device_hash FROM teacher_reviews WHERE id = ${id}`;
      if (!rows.length) throw new ApiError(404, "not_found");
      const days = daysOf(body.days);
      await q`INSERT INTO bans (device_hash, reason, until)
        VALUES (${String(rows[0].device_hash)}, ${cleanText(body.reason, 200)},
          ${days == null ? null : new Date(Date.now() + days * 86400_000).toISOString()}::timestamptz)`;
      if (body.hideAll === true) await q`UPDATE teacher_reviews SET hidden = true WHERE device_hash = ${String(rows[0].device_hash)}`;
      break;
    }
    case "ban.user": {
      await banUser(uuidOf(body.userId), daysOf(body.days), cleanText(body.reason, 200));
      break;
    }
    case "ban.message": {
      const id = idOf(body.id);
      const rows = await supabaseAdmin<{ user_id: string }[]>(`messages?id=eq.${id}&select=user_id`);
      if (!rows?.length) throw new ApiError(404, "not_found");
      await banUser(rows[0].user_id, daysOf(body.days), cleanText(body.reason, 200));
      if (body.deleteMessage === true) await deleteMessage(id);
      break;
    }
    case "ban.remove": {
      const id = idOf(body.id);
      const rows = await q`DELETE FROM bans WHERE id = ${id} RETURNING user_id`;
      const userId = rows[0]?.user_id ? String(rows[0].user_id) : null;
      if (userId && chatModerationEnabled()) await supabaseAdmin(`chat_bans?user_id=eq.${userId}`, { method: "DELETE" });
      break;
    }
    case "announce.create": {
      const text = cleanText(body.text, 500);
      if (!text) throw new ApiError(400, "bad_request");
      const days = daysOf(body.days);
      await q`INSERT INTO announcements (text, until)
        VALUES (${text}, ${days == null ? null : new Date(Date.now() + days * 86400_000).toISOString()}::timestamptz)`;
      break;
    }
    case "announce.remove": {
      await q`UPDATE announcements SET active = false WHERE id = ${idOf(body.id)}`;
      break;
    }
    case "user.delete": {
      await q`DELETE FROM users WHERE id = ${uuidOf(body.userId)}::uuid`;
      break;
    }
    default:
      throw new ApiError(400, "bad_request");
  }
  await log(action, body.id ?? body.target ?? body.userId, body.reason ?? body.text);
  return json({ ok: true });
});
