import { ensureSchema, sql } from "@/lib/server/db";
import { handle, json, preflight } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

export const GET = handle(async () => {
  await ensureSchema();
  const rows = await sql()`
    SELECT id, text, created_at FROM announcements
    WHERE active AND (until IS NULL OR until > now())
    ORDER BY created_at DESC LIMIT 3`;
  return json(
    { items: rows.map((row) => ({ id: Number(row.id), text: String(row.text), createdAt: new Date(row.created_at as string).toISOString() })) },
    200,
    { "Cache-Control": "public, s-maxage=60, stale-while-revalidate=300" },
  );
});
