import { ApiError, assertNotBanned } from "@/lib/server/db";
import { publicUser, requireUser, supabaseToken } from "@/lib/server/auth";
import { handle, json, preflight } from "@/lib/server/http";
import { chatConfig } from "@/lib/server/supabase";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** Returns the Supabase project address plus a 1-hour token for the signed-in user. */
export const POST = handle(async (request: Request) => {
  const config = chatConfig();
  if (!config) throw new ApiError(503, "chat_not_configured");
  const user = await requireUser(request);
  await assertNotBanned(null, String(user.id));
  const { token, expiresAt } = await supabaseToken(publicUser(user));
  return json({ url: config.url, anonKey: config.anonKey, token, expiresAt, userId: String(user.id) });
});
