import { ApiError, checkRateLimit, ensureSchema, sql } from "@/lib/server/db";
import { cleanLogin, cleanName, cleanPassword, clientIp, hashPassword, sha256 } from "@/lib/server/auth";
import { sessionResponse } from "@/lib/server/accounts";
import { handle, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

export const POST = handle(async (request: Request) => {
  const body = await readBody(request);
  const login = cleanLogin(body.login);
  const password = cleanPassword(body.password);
  const name = cleanName(body.name);
  await ensureSchema();
  await checkRateLimit(`register:${sha256(clientIp(request))}`, 5);
  const taken = await sql()`SELECT 1 FROM users WHERE login = ${login}`;
  if (taken.length) throw new ApiError(409, "login_taken");
  const rows = await sql()`
    INSERT INTO users (login, password_hash, name) VALUES (${login}, ${await hashPassword(password)}, ${name})
    ON CONFLICT (login) DO NOTHING RETURNING *`;
  if (!rows.length) throw new ApiError(409, "login_taken");
  return sessionResponse(rows[0], request);
});
