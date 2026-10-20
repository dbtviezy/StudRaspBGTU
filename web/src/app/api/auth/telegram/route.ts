import { ensureSchema } from "@/lib/server/db";
import { userFromRequest, verifyTelegram } from "@/lib/server/auth";
import { providerSignIn } from "@/lib/server/accounts";
import { handle, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** Body: the object passed to the Telegram Login Widget `data-onauth` callback. */
export const POST = handle(async (request: Request) => {
  const profile = verifyTelegram(await readBody(request));
  await ensureSchema();
  return providerSignIn("telegram", String(profile.id), profile.name, await userFromRequest(request), request);
});
