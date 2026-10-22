import { ApiError, checkRateLimit, sql } from "@/lib/server/db";
import { requireUser } from "@/lib/server/auth";
import { handle, json, preflight } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** `?q=` is a login or the 8-character ID shown in the profile. */
export const GET = handle(async (request: Request) => {
  const me = await requireUser(request);
  const query = (new URL(request.url).searchParams.get("q") ?? "").trim().toLowerCase().replace(/^@/, "");
  if (!query) throw new ApiError(400, "bad_request");
  await checkRateLimit(`lookup:${String(me.id)}`, 60);
  const rows = /^[0-9a-f]{8}$/.test(query)
    ? await sql()`SELECT id, name FROM users WHERE login = ${query} OR id::text LIKE ${`${query}%`} LIMIT 2`
    : /^[a-z0-9_.]{3,24}$/.test(query)
      ? await sql()`SELECT id, name FROM users WHERE login = ${query} LIMIT 1`
      : [];
  if (rows.length !== 1 || String(rows[0].id) === String(me.id)) throw new ApiError(404, "user_not_found");
  return json({ id: String(rows[0].id), name: String(rows[0].name) });
});
