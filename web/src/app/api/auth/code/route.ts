import { createAuthCode, requireUser } from "@/lib/server/auth";
import { handle, json, preflight } from "@/lib/server/http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export function OPTIONS() {
  return preflight();
}

/** Issues a 5-minute one-time code for handing a browser sign-in over to the Android app. */
export const POST = handle(async (request: Request) => {
  const user = await requireUser(request);
  return json({ code: await createAuthCode(String(user.id)) });
});
