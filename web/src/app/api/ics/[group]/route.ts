import { addDays, isoOf } from "@/lib/dates";
import { rangeIcs } from "@/lib/ics";
import { parseBundle } from "@/lib/schedule";

export const runtime = "nodejs";

const PAST_DAYS = 14;
const AHEAD_DAYS = 120;

function moscowToday(): string {
  const now = new Date(Date.now() + 3 * 3600_000);
  return isoOf(new Date(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
}

/** Self-updating calendar of one group: /api/ics/<group>.ics (no personal notes or hidden lessons). */
export async function GET(request: Request, { params }: { params: Promise<{ group: string }> }) {
  const id = decodeURIComponent((await params).group).replace(/\.ics$/i, "");
  let bundle;
  try {
    const response = await fetch(new URL("/schedule.json", request.url), { next: { revalidate: 3600 } });
    if (!response.ok) throw new Error(String(response.status));
    bundle = parseBundle(await response.json());
  } catch {
    return new Response("schedule unavailable", { status: 503 });
  }
  const entry = bundle.find(id);
  if (!entry) return new Response("group not found", { status: 404 });
  const from = addDays(moscowToday(), -PAST_DAYS);
  const { text } = rangeIcs(entry.group, bundle.semesterStart, from, PAST_DAYS + AHEAD_DAYS, { hidden: new Set(), changes: null }, "Пара", {
    subscription: { name: `БГТУ ${entry.group.id}` },
  });
  return new Response(text, {
    headers: {
      "Content-Type": "text/calendar; charset=utf-8",
      "Content-Disposition": `inline; filename="schedule.ics"`,
      "Cache-Control": "public, max-age=0, s-maxage=21600, stale-while-revalidate=86400",
      "Access-Control-Allow-Origin": "*",
    },
  });
}
