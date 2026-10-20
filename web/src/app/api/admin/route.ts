import { ApiError, BAN_SCOPES, cleanText, ensureSchema, sql, type BanScope } from "@/lib/server/db";
import { requireAdmin } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";
import { syncClassRep, toRepRequest } from "@/lib/server/reps";
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
    (SELECT count(*) FROM announcements WHERE active AND (until IS NULL OR until > now()))::int AS announcements,
    (SELECT count(*) FROM rep_requests WHERE status = 'pending')::int AS pending_reps`;
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
    pendingReps: row.pending_reps,
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
  return (rows ?? []).map(toMessage);
}

function toMessage(row: Row) {
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
}

const toBan = (row: Row) => ({
  id: Number(row.id),
  userId: row.user_id ? String(row.user_id) : null,
  device: Boolean(row.device),
  name: row.name ? String(row.name) : null,
  login: row.login ? String(row.login) : null,
  scope: String(row.scope ?? "all"),
  reason: String(row.reason),
  createdAt: iso(row.created_at),
  until: iso(row.until),
  active: row.until == null || new Date(row.until as string) > new Date(),
});

async function bans() {
  const rows = await sql()`
    SELECT b.id, b.user_id, b.device_hash IS NOT NULL AS device, b.scope, b.reason, b.created_at, b.until, u.name, u.login
    FROM bans b LEFT JOIN users u ON u.id = b.user_id
    WHERE b.until IS NULL OR b.until > now()
    ORDER BY b.created_at DESC LIMIT 200`;
  return rows.map(toBan);
}

/** Everything about one person for the moderator's card. */
async function person(url: URL) {
  const id = uuidOf(url.searchParams.get("id"));
  const q = sql();
  const [users, reviewRows, reportRows, banRows, sessionRows, repRows] = await Promise.all([
    q`SELECT * FROM users WHERE id = ${id}::uuid`,
    q`SELECT r.id, r.teacher, r.overall, r.text, r.hidden, r.updated_at,
        (SELECT count(*) FROM reports p WHERE p.kind = 'review' AND p.target = r.id::text)::int AS reports
      FROM teacher_reviews r WHERE r.user_id = ${id}::uuid ORDER BY r.updated_at DESC LIMIT 100`,
    q`SELECT count(*)::int AS sent FROM reports WHERE user_id = ${id}::uuid`,
    q`SELECT b.id, b.user_id, b.device_hash IS NOT NULL AS device, b.scope, b.reason, b.created_at, b.until
      FROM bans b WHERE b.user_id = ${id}::uuid ORDER BY b.created_at DESC LIMIT 100`,
    q`SELECT count(*)::int AS n FROM sessions WHERE user_id = ${id}::uuid AND expires_at > now()`,
    q`SELECT * FROM rep_requests WHERE user_id = ${id}::uuid ORDER BY created_at DESC LIMIT 1`,
  ]);
  if (!users.length) throw new ApiError(404, "not_found");
  const user = users[0];
  let chatMessages: ReturnType<typeof toMessage>[] = [];
  if (chatModerationEnabled()) {
    const rows = await supabaseAdmin<Row[]>(
      `messages?user_id=eq.${id}&select=id,chat_id,user_id,author,body,created_at,chats(kind,title,group_id)&order=id.desc&limit=30`,
    );
    chatMessages = (rows ?? []).map(toMessage);
  }
  return {
    user: {
      id: String(user.id),
      name: String(user.name),
      login: user.login ? String(user.login) : null,
      telegram: user.telegram_id != null,
      google: user.google_sub != null,
      createdAt: iso(user.created_at),
      seenAt: iso(user.seen_at),
    },
    devices: Number(sessionRows[0]?.n ?? 0),
    reportsSent: Number(reportRows[0]?.sent ?? 0),
    reviews: reviewRows.map((row) => ({
      id: Number(row.id),
      teacher: String(row.teacher),
      overall: Number(row.overall),
      text: String(row.text),
      hidden: Boolean(row.hidden),
      updatedAt: iso(row.updated_at),
      reports: Number(row.reports),
    })),
    messages: chatMessages,
    bans: banRows.map(toBan),
    rep: repRows.length ? toRepRequest(repRows[0]) : null,
  };
}

async function reps() {
  const rows = await sql()`
    SELECT r.*, u.name, u.login FROM rep_requests r JOIN users u ON u.id = r.user_id
    WHERE r.status IN ('pending', 'approved')
    ORDER BY (r.status = 'pending') DESC, r.created_at DESC LIMIT 200`;
  return rows.map((row) => ({
    ...toRepRequest(row),
    userId: String(row.user_id),
    name: String(row.name),
    login: row.login ? String(row.login) : null,
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
      EXISTS (SELECT 1 FROM bans b WHERE b.user_id = u.id AND b.scope = 'all' AND (b.until IS NULL OR b.until > now())) AS banned,
      EXISTS (SELECT 1 FROM bans b WHERE b.user_id = u.id AND b.scope <> 'all' AND (b.until IS NULL OR b.until > now())) AS restricted,
      EXISTS (SELECT 1 FROM rep_requests r WHERE r.user_id = u.id AND r.status = 'approved') AS rep
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
    restricted: Boolean(row.restricted),
    rep: Boolean(row.rep),
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
    case "user": return json(await person(url));
    case "reps": return json({ items: await reps() });
    default: throw new ApiError(400, "bad_request");
  }
});

async function deleteMessage(id: number) {
  await supabaseAdmin(`messages?id=eq.${id}`, { method: "DELETE" });
}

function scopeOf(value: unknown): BanScope {
  if (value == null) return "all";
  if (typeof value !== "string" || !(BAN_SCOPES as readonly string[]).includes(value)) throw new ApiError(400, "bad_request");
  return value as BanScope;
}

const untilOf = (days: number | null) => (days == null ? null : new Date(Date.now() + days * 86400_000).toISOString());

/** Rewrites the user's rows in Supabase chat_bans from the active chat-related bans here. */
async function syncChatBans(userId: string) {
  if (!chatModerationEnabled()) return;
  const rows = await sql()`
    SELECT DISTINCT ON (scope) scope, until, reason FROM bans
    WHERE user_id = ${userId}::uuid AND scope IN ('all', 'chat', 'chat_create') AND (until IS NULL OR until > now())
    ORDER BY scope, until DESC NULLS FIRST`;
  await supabaseAdmin(`chat_bans?user_id=eq.${userId}`, { method: "DELETE" });
  if (rows.length) {
    await supabaseAdmin("chat_bans", {
      method: "POST",
      headers: { Prefer: "return=minimal" },
      body: JSON.stringify(rows.map((row) => ({ user_id: userId, scope: row.scope, until: iso(row.until), reason: row.reason }))),
    });
  }
}

/** A full ban (`all`) also signs the person out everywhere. */
async function banUser(userId: string, days: number | null, reason: string, scope: BanScope, device: string | null = null) {
  const q = sql();
  await q`INSERT INTO bans (user_id, device_hash, scope, reason, until)
    VALUES (${userId}::uuid, ${device}, ${scope}, ${reason}, ${untilOf(days)}::timestamptz)`;
  if (scope === "all") await q`DELETE FROM sessions WHERE user_id = ${userId}::uuid`;
  await syncChatBans(userId);
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
      const rows = await q`SELECT device_hash, user_id FROM teacher_reviews WHERE id = ${id}`;
      if (!rows.length) throw new ApiError(404, "not_found");
      const days = daysOf(body.days);
      const scope = scopeOf(body.scope);
      const reason = cleanText(body.reason, 200);
      const device = String(rows[0].device_hash);
      if (rows[0].user_id) {
        await banUser(String(rows[0].user_id), days, reason, scope, device);
      } else {
        await q`INSERT INTO bans (device_hash, scope, reason, until) VALUES (${device}, ${scope}, ${reason}, ${untilOf(days)}::timestamptz)`;
      }
      if (body.hideAll === true) {
        if (rows[0].user_id) await q`UPDATE teacher_reviews SET hidden = true WHERE user_id = ${String(rows[0].user_id)}::uuid`;
        await q`UPDATE teacher_reviews SET hidden = true WHERE device_hash = ${device}`;
      }
      break;
    }
    case "ban.user": {
      await banUser(uuidOf(body.userId), daysOf(body.days), cleanText(body.reason, 200), scopeOf(body.scope));
      break;
    }
    case "ban.message": {
      const id = idOf(body.id);
      const rows = await supabaseAdmin<{ user_id: string }[]>(`messages?id=eq.${id}&select=user_id`);
      if (!rows?.length) throw new ApiError(404, "not_found");
      await banUser(rows[0].user_id, daysOf(body.days), cleanText(body.reason, 200), scopeOf(body.scope));
      if (body.deleteMessage === true) await deleteMessage(id);
      break;
    }
    case "ban.remove": {
      const id = idOf(body.id);
      const rows = await q`DELETE FROM bans WHERE id = ${id} RETURNING user_id`;
      if (rows[0]?.user_id) await syncChatBans(String(rows[0].user_id));
      break;
    }
    case "rep.approve": {
      const id = idOf(body.id);
      const rows = await q`UPDATE rep_requests SET status = 'approved', decided_at = now()
        WHERE id = ${id} AND status = 'pending' RETURNING user_id, group_id`;
      if (!rows.length) throw new ApiError(404, "not_found");
      const userId = String(rows[0].user_id);
      await q`UPDATE rep_requests SET status = 'revoked', decided_at = now()
        WHERE user_id = ${userId}::uuid AND status = 'approved' AND id <> ${id}`;
      const [user] = await q`SELECT name FROM users WHERE id = ${userId}::uuid`;
      await syncClassRep(userId, String(rows[0].group_id), String(user?.name ?? ""));
      break;
    }
    case "rep.reject": {
      await q`UPDATE rep_requests SET status = 'rejected', decided_at = now() WHERE id = ${idOf(body.id)} AND status = 'pending'`;
      break;
    }
    case "rep.revoke": {
      const rows = await q`UPDATE rep_requests SET status = 'revoked', decided_at = now()
        WHERE id = ${idOf(body.id)} AND status = 'approved' RETURNING user_id`;
      if (rows.length) await syncClassRep(String(rows[0].user_id), null);
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
  const detail = action.startsWith("ban.") && action !== "ban.remove"
    ? `[${String(body.scope ?? "all")}] ${String(body.reason ?? "")}`
    : body.reason ?? body.text;
  await log(action, body.id ?? body.target ?? body.userId, detail);
  return json({ ok: true });
});
