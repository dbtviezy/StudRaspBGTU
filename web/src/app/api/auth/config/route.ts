import { googleClientId, telegramConfig } from "@/lib/server/auth";
import { json, preflight } from "@/lib/server/http";
import { chatConfig } from "@/lib/server/supabase";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** Tells clients which sign-in methods and features are switched on by the server env. */
export function GET() {
  return json({
    password: Boolean(process.env.DATABASE_URL),
    telegramBot: telegramConfig()?.bot ?? null,
    googleClientId: googleClientId(),
    chat: chatConfig() != null,
  });
}
