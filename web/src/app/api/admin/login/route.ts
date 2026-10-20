import { ApiError, checkRateLimit, ensureSchema, sql } from "@/lib/server/db";
import { checkAdminPassword, clientIp, createAdminToken, sha256 } from "@/lib/server/auth";
import { handle, json, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

export const POST = handle(async (request: Request) => {
  const body = await readBody(request);
  await ensureSchema();
  await checkRateLimit(`admin:${sha256(clientIp(request))}`, 5);
  if (!checkAdminPassword(body.password)) throw new ApiError(401, "wrong_credentials");
  await sql()`INSERT INTO admin_log (action) VALUES ('login')`;
  return json(createAdminToken());
});
