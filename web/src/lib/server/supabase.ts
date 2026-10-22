import { ApiError } from "./db";

export interface ChatConfig {
  url: string;
  anonKey: string;
}

export function chatConfig(): ChatConfig | null {
  const url = process.env.SUPABASE_URL?.trim().replace(/\/+$/, "");
  const anonKey = process.env.SUPABASE_ANON_KEY?.trim();
  return url && anonKey && process.env.SUPABASE_JWT_SECRET ? { url, anonKey } : null;
}

/** Calls Supabase REST with the service role key (bypasses RLS) — moderation only. */
export async function supabaseAdmin<T = unknown>(path: string, init: RequestInit = {}): Promise<T> {
  const url = process.env.SUPABASE_URL?.trim().replace(/\/+$/, "");
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY?.trim();
  if (!url || !key) throw new ApiError(503, "chat_not_configured");
  const response = await fetch(`${url}/rest/v1/${path}`, {
    ...init,
    headers: {
      apikey: key,
      Authorization: `Bearer ${key}`,
      "Content-Type": "application/json",
      Prefer: "return=representation",
      ...(init.headers ?? {}),
    },
    cache: "no-store",
  });
  if (!response.ok) {
    console.error("supabase admin error", response.status, await response.text().catch(() => ""));
    throw new ApiError(502, "chat_unavailable");
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : null) as T;
}

export async function supabaseCount(path: string): Promise<number | null> {
  const url = process.env.SUPABASE_URL?.trim().replace(/\/+$/, "");
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY?.trim();
  if (!url || !key) return null;
  try {
    const response = await fetch(`${url}/rest/v1/${path}`, {
      method: "HEAD",
      headers: { apikey: key, Authorization: `Bearer ${key}`, Prefer: "count=exact" },
      cache: "no-store",
    });
    const total = response.headers.get("content-range")?.split("/")[1];
    return total && total !== "*" ? Number(total) : null;
  } catch {
    return null;
  }
}

export function chatModerationEnabled(): boolean {
  return Boolean(process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY);
}
