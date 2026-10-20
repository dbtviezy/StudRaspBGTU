import { ensureSchema } from "@/lib/server/db";
import { userFromRequest, verifyGoogle } from "@/lib/server/auth";
import { providerSignIn } from "@/lib/server/accounts";
import { handle, preflight, readBody } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** Body: `{ credential }` — the ID token from Google Identity Services. */
export const POST = handle(async (request: Request) => {
  const body = await readBody(request);
  const profile = await verifyGoogle(body.credential);
  await ensureSchema();
  return providerSignIn("google", profile.sub, profile.name, await userFromRequest(request));
});
