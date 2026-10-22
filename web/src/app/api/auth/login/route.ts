import { ApiError, checkRateLimit, ensureSchema, sql } from "@/lib/server/db";
import { cleanLogin, clientIp, sha256, verifyPassword } from "@/lib/server/auth";
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
  const password = typeof body.password === "string" ? body.password : "";
  await ensureSchema();
  await checkRateLimit(`login:${sha256(`${clientIp(request)}|${login}`)}`, 10);
  const rows = await sql()`SELECT * FROM users WHERE login = ${login}`;
  const user = rows[0];
  if (!user?.password_hash || !(await verifyPassword(password, String(user.password_hash)))) {
    throw new ApiError(401, "wrong_credentials");
  }
  return sessionResponse(user, request);
});
