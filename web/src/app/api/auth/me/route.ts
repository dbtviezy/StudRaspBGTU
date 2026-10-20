import { ApiError, ensureSchema, sql } from "@/lib/server/db";
import { cleanLogin, cleanName, cleanPassword, endSession, hashPassword, requireUser, verifyPassword } from "@/lib/server/auth";
import { userJson } from "@/lib/server/accounts";
import { handle, json, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

export const GET = handle(async (request: Request) => json(userJson(await requireUser(request))));

/**
 * Body (all optional): `name`; `login` + `password` to add a password to a Telegram/Google account;
 * `password` + `newPassword` to change it; `unlink: "telegram" | "google"`.
 */
export const PATCH = handle(async (request: Request) => {
  const user = await requireUser(request);
  const body = await readBody(request);
  const id = String(user.id);
  const q = sql();
  let row = user;
  if (body.name !== undefined) {
    row = (await q`UPDATE users SET name = ${cleanName(body.name)} WHERE id = ${id}::uuid RETURNING *`)[0];
  }
  if (body.newPassword !== undefined) {
    const current = typeof body.password === "string" ? body.password : "";
    if (!row.password_hash || !(await verifyPassword(current, String(row.password_hash)))) {
      throw new ApiError(401, "wrong_credentials");
    }
    const hash = await hashPassword(cleanPassword(body.newPassword));
    row = (await q`UPDATE users SET password_hash = ${hash} WHERE id = ${id}::uuid RETURNING *`)[0];
  } else if (body.login !== undefined) {
    if (row.login) throw new ApiError(409, "login_taken");
    const login = cleanLogin(body.login);
    const hash = await hashPassword(cleanPassword(body.password));
    const rows = await q`
      UPDATE users SET login = ${login}, password_hash = ${hash}
      WHERE id = ${id}::uuid AND NOT EXISTS (SELECT 1 FROM users WHERE login = ${login})
      RETURNING *`;
    if (!rows.length) throw new ApiError(409, "login_taken");
    row = rows[0];
  }
  if (body.unlink === "telegram" || body.unlink === "google") {
    const methods = [row.password_hash, row.telegram_id, row.google_sub].filter((value) => value != null).length;
    if (methods < 2) throw new ApiError(400, "last_method");
    row = body.unlink === "telegram"
      ? (await q`UPDATE users SET telegram_id = NULL WHERE id = ${id}::uuid RETURNING *`)[0]
      : (await q`UPDATE users SET google_sub = NULL WHERE id = ${id}::uuid RETURNING *`)[0];
  }
  return json(userJson(row));
});

/** Same as PATCH: Android's HttpURLConnection cannot send PATCH requests. */
export const POST = PATCH;

/** Signs out this device. With `{ everywhere: true }` ends all sessions; with `{ deleteAccount: true }` removes the account. */
export const DELETE = handle(async (request: Request) => {
  const user = await requireUser(request);
  const body = await readBody(request).catch(() => ({}) as Record<string, unknown>);
  await ensureSchema();
  if (body.deleteAccount === true) {
    await sql()`DELETE FROM users WHERE id = ${String(user.id)}::uuid`;
  } else if (body.everywhere === true) {
    await sql()`DELETE FROM sessions WHERE user_id = ${String(user.id)}::uuid`;
  } else {
    await endSession(request);
  }
  return json({ ok: true });
});
