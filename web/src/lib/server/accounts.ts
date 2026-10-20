import { ApiError, assertNotBanned, sql } from "./db";
import { createSession, deviceLabel, publicUser, type PublicUser } from "./auth";
import { json } from "./http";

type Row = Record<string, unknown>;

export async function sessionResponse(user: Row, request: Request | null = null): Promise<Response> {
  await assertNotBanned(null, String(user.id));
  const token = await createSession(String(user.id), deviceLabel(request));
  return json({ token, user: publicUser(user) });
}

/**
 * Signs in with an external provider. If `current` is set, links the provider to that account instead.
 */
export async function providerSignIn(
  provider: "telegram" | "google",
  externalId: string,
  name: string,
  current: Row | null,
  request: Request,
): Promise<Response> {
  const q = sql();
  const existing = provider === "telegram"
    ? await q`SELECT * FROM users WHERE telegram_id = ${externalId}::bigint`
    : await q`SELECT * FROM users WHERE google_sub = ${externalId}`;
  if (current) {
    if (existing.length && String(existing[0].id) !== String(current.id)) throw new ApiError(409, "already_linked");
    const rows = provider === "telegram"
      ? await q`UPDATE users SET telegram_id = ${externalId}::bigint WHERE id = ${String(current.id)}::uuid RETURNING *`
      : await q`UPDATE users SET google_sub = ${externalId} WHERE id = ${String(current.id)}::uuid RETURNING *`;
    return json({ user: publicUser(rows[0]) });
  }
  if (existing.length) return sessionResponse(existing[0], request);
  const rows = provider === "telegram"
    ? await q`INSERT INTO users (name, telegram_id) VALUES (${name}, ${externalId}::bigint) RETURNING *`
    : await q`INSERT INTO users (name, google_sub) VALUES (${name}, ${externalId}) RETURNING *`;
  return sessionResponse(rows[0], request);
}

export function userJson(user: Row): { user: PublicUser } {
  return { user: publicUser(user) };
}
