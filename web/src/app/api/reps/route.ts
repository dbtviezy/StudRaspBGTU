import { ApiError, assertNotBanned, checkRateLimit, cleanText, ensureSchema, sql } from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";
import { latestRepRequest, syncClassRep } from "@/lib/server/reps";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** The caller's latest class rep request: `{ rep }`. */
export const GET = handle(async (request: Request) => {
  const user = await requireUser(request);
  return json({ rep: await latestRepRequest(String(user.id)) });
});

/** Body `{ groupId, groupName, comment }`: asks the moderators to confirm the caller as class rep of that group. */
export const POST = handle(async (request: Request) => {
  const user = await requireUser(request);
  const body = await readBody(request);
  const id = String(user.id);
  const groupId = typeof body.groupId === "string" ? body.groupId.trim().slice(0, 40) : "";
  if (!groupId) throw new ApiError(400, "bad_request");
  const groupName = cleanText(body.groupName, 80);
  const comment = cleanText(body.comment, 300);
  await ensureSchema();
  await assertNotBanned(null, id);
  await checkRateLimit(`rep:${id}`, 5);
  const q = sql();
  const current = await latestRepRequest(id);
  if (current?.status === "approved" && current.groupId === groupId) throw new ApiError(409, "already_rep");
  await q`DELETE FROM rep_requests WHERE user_id = ${id}::uuid AND status = 'pending'`;
  await q`INSERT INTO rep_requests (user_id, group_id, group_name, comment)
    VALUES (${id}::uuid, ${groupId}, ${groupName}, ${comment})`;
  return json({ rep: await latestRepRequest(id) });
});

/** Withdraws a pending request or steps down as class rep. */
export const DELETE = handle(async (request: Request) => {
  const user = await requireUser(request);
  const id = String(user.id);
  const q = sql();
  await q`DELETE FROM rep_requests WHERE user_id = ${id}::uuid AND status = 'pending'`;
  const stepped = await q`UPDATE rep_requests SET status = 'revoked', decided_at = now()
    WHERE user_id = ${id}::uuid AND status = 'approved' RETURNING id`;
  if (stepped.length) await syncClassRep(id, null);
  return json({ rep: await latestRepRequest(id) });
});
