import { ApiError, checkRateLimit, ensureSchema, sql } from "@/lib/server/db";
import { clientIp, redeemAuthCode, sha256 } from "@/lib/server/auth";
import { sessionResponse } from "@/lib/server/accounts";
import { handle, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

export const POST = handle(async (request: Request) => {
  const body = await readBody(request);
  if (typeof body.code !== "string" || !/^[A-Za-z0-9_-]{20,64}$/.test(body.code)) throw new ApiError(400, "invalid_code");
  await ensureSchema();
  await checkRateLimit(`exchange:${sha256(clientIp(request))}`, 20);
  const userId = await redeemAuthCode(body.code);
  const rows = await sql()`SELECT * FROM users WHERE id = ${userId}::uuid`;
  if (!rows.length) throw new ApiError(400, "invalid_code");
  return sessionResponse(rows[0], request);
});
