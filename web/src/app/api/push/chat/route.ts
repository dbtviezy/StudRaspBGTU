import { timingSafeEqual } from "node:crypto";
import { maskIf } from "@/lib/profanity";
import { ApiError, ensureSchema, sql } from "@/lib/server/db";
import { handle, json, readBody } from "@/lib/server/http";
import { pushKeys, sendPush } from "@/lib/server/push";
import { supabaseAdmin } from "@/lib/server/supabase";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function secretOk(request: Request): boolean {
  const expected = process.env.PUSH_WEBHOOK_SECRET?.trim();
  const given = request.headers.get("x-push-secret")?.trim() ?? "";
  if (!expected || given.length !== expected.length) return false;
  return timingSafeEqual(Buffer.from(given), Buffer.from(expected));
}

/** Called by the Supabase trigger after every new chat message (see web/db/chat.sql). */
export const POST = handle(async (request: Request) => {
  if (!secretOk(request)) throw new ApiError(401, "unauthorized");
  const keys = pushKeys();
  if (!keys) return json({ sent: 0 });
  const body = await readBody(request);
  const chatId = String(body.chat_id ?? "");
  const authorId = String(body.user_id ?? "");
  if (!UUID.test(chatId) || !UUID.test(authorId)) throw new ApiError(400, "bad_request");
  const author = String(body.author ?? "").slice(0, 60);
  const text = String(body.body ?? "").slice(0, 300);

  const [chat] = await supabaseAdmin<{ kind: string; title: string }[]>(`chats?id=eq.${chatId}&select=kind,title`);
  if (!chat) return json({ sent: 0 });
  const members = await supabaseAdmin<{ user_id: string }[]>(`chat_members?chat_id=eq.${chatId}&accepted=eq.true&select=user_id`);
  const ids = members.map((m) => m.user_id).filter((id) => id !== authorId && UUID.test(id));
  if (!ids.length) return json({ sent: 0 });

  await ensureSchema();
  const q = sql();
  const targets = await q`SELECT endpoint, p256dh, auth, hide_profanity, lang FROM push_subscriptions WHERE user_id = ANY(${ids}::uuid[])`;
  const group = chat.kind === "group";
  const gone: string[] = [];
  await Promise.all(
    targets.map(async (row) => {
      const shown = maskIf(Boolean(row.hide_profanity), text);
      const payload = {
        title: group ? chat.title || (row.lang === "en" ? "Group chat" : "Чат группы") : author,
        body: group ? `${author}: ${shown}` : shown,
        tag: `chat-${chatId}`,
        url: `/?chat=${chatId}`,
      };
      const alive = await sendPush(keys, { endpoint: String(row.endpoint), p256dh: String(row.p256dh), auth: String(row.auth) }, payload);
      if (!alive) gone.push(String(row.endpoint));
    }),
  );
  if (gone.length) await q`DELETE FROM push_subscriptions WHERE endpoint = ANY(${gone})`;
  return json({ sent: targets.length - gone.length });
});
