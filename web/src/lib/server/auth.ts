import { createHash, createHmac, createPublicKey, randomBytes, scrypt, timingSafeEqual, verify, type JsonWebKey } from "node:crypto";
import { promisify } from "node:util";
import { ApiError, ensureSchema, sql } from "./db";

const scryptAsync = promisify(scrypt) as (password: string, salt: Buffer, keylen: number) => Promise<Buffer>;
const SESSION_DAYS = 180;

export interface PublicUser {
  id: string;
  name: string;
  login: string | null;
  telegram: boolean;
  google: boolean;
  createdAt: string;
}

type Row = Record<string, unknown>;

export function publicUser(row: Row): PublicUser {
  return {
    id: String(row.id),
    name: String(row.name),
    login: row.login ? String(row.login) : null,
    telegram: row.telegram_id != null,
    google: row.google_sub != null,
    createdAt: new Date(row.created_at as string).toISOString(),
  };
}

export function sha256(value: string): string {
  return createHash("sha256").update(value).digest("hex");
}

function safeEqual(a: string, b: string): boolean {
  const left = Buffer.from(a);
  const right = Buffer.from(b);
  return left.length === right.length && timingSafeEqual(left, right);
}

export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(16);
  const key = await scryptAsync(password, salt, 32);
  return `scrypt$${salt.toString("base64")}$${key.toString("base64")}`;
}

export async function verifyPassword(password: string, stored: string): Promise<boolean> {
  const [scheme, salt, key] = stored.split("$");
  if (scheme !== "scrypt" || !salt || !key) return false;
  const actual = await scryptAsync(password, Buffer.from(salt, "base64"), 32);
  return safeEqual(actual.toString("base64"), key);
}

export function cleanLogin(value: unknown): string {
  const login = typeof value === "string" ? value.trim().toLowerCase() : "";
  if (!/^[a-z0-9_.]{3,24}$/.test(login)) throw new ApiError(400, "invalid_login");
  return login;
}

export function cleanPassword(value: unknown): string {
  if (typeof value !== "string" || value.length < 6 || value.length > 100) throw new ApiError(400, "invalid_password");
  return value;
}

export function cleanName(value: unknown): string {
  const name = typeof value === "string" ? value.replace(/\s+/g, " ").trim() : "";
  if (Array.from(name).length < 2 || Array.from(name).length > 40) throw new ApiError(400, "invalid_name");
  return name;
}

/** "Android · Pixel 7" from the app's X-Device-Name header, otherwise a guess from the browser's user agent. */
export function deviceLabel(request: Request | null): string {
  if (!request) return "";
  const raw = request.headers.get("x-device-name") ?? "";
  let decoded = raw;
  try {
    decoded = decodeURIComponent(raw.replace(/\+/g, " "));
  } catch {}
  const named = decoded.replace(/[\u0000-\u001f]/g, "").trim();
  if (named) return named.slice(0, 80);
  const agent = request.headers.get("user-agent") ?? "";
  const system = /iPhone|iPad/.test(agent) ? "iPhone" : /Android/.test(agent) ? "Android"
    : /Windows/.test(agent) ? "Windows" : /Mac OS/.test(agent) ? "Mac" : /Linux/.test(agent) ? "Linux" : "";
  const browser = /YaBrowser/.test(agent) ? "Яндекс Браузер" : /Edg\//.test(agent) ? "Edge" : /OPR\//.test(agent) ? "Opera"
    : /Firefox\//.test(agent) ? "Firefox" : /Chrome\//.test(agent) ? "Chrome" : /Safari\//.test(agent) ? "Safari" : "";
  return [browser, system].filter(Boolean).join(" · ").slice(0, 80);
}

export async function createSession(userId: string, device = ""): Promise<string> {
  const token = randomBytes(32).toString("base64url");
  const q = sql();
  await q`INSERT INTO sessions (token_hash, user_id, expires_at, device)
    VALUES (${sha256(token)}, ${userId}::uuid, now() + make_interval(days => ${SESSION_DAYS}), ${device})`;
  if (Math.random() < 0.05) await q`DELETE FROM sessions WHERE expires_at < now()`;
  return token;
}

function bearer(request: Request): string | null {
  const header = request.headers.get("authorization") ?? "";
  const match = /^Bearer\s+([A-Za-z0-9_-]{20,100})$/.exec(header.trim());
  return match ? match[1] : null;
}

/** sha256 of this request's session token, to mark the current device in the session list. */
export function sessionHash(request: Request): string | null {
  const token = bearer(request);
  return token ? sha256(token) : null;
}

export async function userFromRequest(request: Request): Promise<Row | null> {
  const token = bearer(request);
  if (!token) return null;
  await ensureSchema();
  const rows = await sql()`
    WITH s AS (
      UPDATE sessions SET used_at = now()
      WHERE token_hash = ${sha256(token)} AND expires_at > now()
      RETURNING user_id
    )
    UPDATE users SET seen_at = now()
    FROM s
    WHERE users.id = s.user_id
    RETURNING users.*`;
  return rows[0] ?? null;
}

export async function requireUser(request: Request): Promise<Row> {
  const user = await userFromRequest(request);
  if (!user) throw new ApiError(401, "unauthorized");
  return user;
}

export async function endSession(request: Request): Promise<void> {
  const token = bearer(request);
  if (token) await sql()`DELETE FROM sessions WHERE token_hash = ${sha256(token)}`;
}

/** One-time code that the Android app trades for a session after a browser sign-in. */
export async function createAuthCode(userId: string): Promise<string> {
  const code = randomBytes(24).toString("base64url");
  const q = sql();
  await q`DELETE FROM auth_codes WHERE expires_at < now()`;
  await q`INSERT INTO auth_codes (code_hash, user_id, expires_at)
    VALUES (${sha256(code)}, ${userId}::uuid, now() + interval '5 minutes')`;
  return code;
}

export async function redeemAuthCode(code: string): Promise<string> {
  const rows = await sql()`
    DELETE FROM auth_codes WHERE code_hash = ${sha256(code)} AND expires_at > now() RETURNING user_id`;
  if (!rows.length) throw new ApiError(400, "invalid_code");
  return String(rows[0].user_id);
}

export function telegramConfig(): { bot: string; token: string } | null {
  const bot = process.env.TELEGRAM_BOT_NAME?.trim();
  const token = process.env.TELEGRAM_BOT_TOKEN?.trim();
  return bot && token ? { bot, token } : null;
}

export function googleClientId(): string | null {
  return process.env.GOOGLE_CLIENT_ID?.trim() || null;
}

export interface TelegramProfile {
  id: number;
  name: string;
}

/** Checks the payload of the Telegram Login Widget: https://core.telegram.org/widgets/login#checking-authorization */
export function verifyTelegram(data: Record<string, unknown>): TelegramProfile {
  const config = telegramConfig();
  if (!config) throw new ApiError(503, "provider_disabled");
  const hash = typeof data.hash === "string" ? data.hash : "";
  const fields = Object.entries(data)
    .filter(([key, value]) => key !== "hash" && value != null && value !== "")
    .map(([key, value]) => `${key}=${String(value)}`)
    .sort()
    .join("\n");
  const secret = createHash("sha256").update(config.token).digest();
  const expected = createHmac("sha256", secret).update(fields).digest("hex");
  if (!hash || !safeEqual(expected, hash)) throw new ApiError(401, "invalid_signature");
  const authDate = Number(data.auth_date);
  if (!Number.isFinite(authDate) || Date.now() / 1000 - authDate > 86400) throw new ApiError(401, "expired");
  const id = Number(data.id);
  if (!Number.isSafeInteger(id)) throw new ApiError(400, "bad_request");
  const name = [data.first_name, data.last_name].filter((part) => typeof part === "string" && part).join(" ")
    || (typeof data.username === "string" ? data.username : "") || `Telegram ${id}`;
  return { id, name: name.slice(0, 40) };
}

let googleKeys: { keys: (JsonWebKey & { kid?: string })[]; until: number } | null = null;

async function googleJwks(): Promise<(JsonWebKey & { kid?: string })[]> {
  if (googleKeys && googleKeys.until > Date.now()) return googleKeys.keys;
  const response = await fetch("https://www.googleapis.com/oauth2/v3/certs");
  if (!response.ok) throw new ApiError(502, "provider_unavailable");
  const body = (await response.json()) as { keys: (JsonWebKey & { kid?: string })[] };
  googleKeys = { keys: body.keys, until: Date.now() + 3600_000 };
  return body.keys;
}

function decodePart(part: string): Record<string, unknown> {
  try {
    return JSON.parse(Buffer.from(part, "base64url").toString("utf8")) as Record<string, unknown>;
  } catch {
    throw new ApiError(400, "bad_request");
  }
}

export interface GoogleProfile {
  sub: string;
  name: string;
}

/** Verifies a Google Identity Services ID token (RS256) against Google's public keys. */
export async function verifyGoogle(credential: unknown): Promise<GoogleProfile> {
  const clientId = googleClientId();
  if (!clientId) throw new ApiError(503, "provider_disabled");
  if (typeof credential !== "string") throw new ApiError(400, "bad_request");
  const parts = credential.split(".");
  if (parts.length !== 3) throw new ApiError(400, "bad_request");
  const header = decodePart(parts[0]);
  const payload = decodePart(parts[1]);
  if (header.alg !== "RS256") throw new ApiError(401, "invalid_signature");
  const jwk = (await googleJwks()).find((key) => key.kid === header.kid);
  if (!jwk) throw new ApiError(401, "invalid_signature");
  const ok = verify(
    "RSA-SHA256",
    Buffer.from(`${parts[0]}.${parts[1]}`),
    createPublicKey({ key: jwk, format: "jwk" }),
    Buffer.from(parts[2], "base64url"),
  );
  if (!ok) throw new ApiError(401, "invalid_signature");
  const issuer = String(payload.iss ?? "");
  if (issuer !== "accounts.google.com" && issuer !== "https://accounts.google.com") throw new ApiError(401, "invalid_signature");
  if (payload.aud !== clientId) throw new ApiError(401, "invalid_signature");
  if (typeof payload.exp !== "number" || payload.exp * 1000 < Date.now()) throw new ApiError(401, "expired");
  const sub = String(payload.sub ?? "");
  if (!sub) throw new ApiError(400, "bad_request");
  const name = String(payload.name ?? payload.given_name ?? payload.email ?? "Google").slice(0, 40);
  return { sub, name };
}

/**
 * Real Supabase Auth access token for this app user (same UUID as Neon).
 * Projects with JWT Signing Keys (ES256) reject hand-rolled HS256 tokens — mint via Admin API instead.
 */
export async function supabaseToken(user: PublicUser): Promise<{ token: string; expiresAt: number }> {
  const url = process.env.SUPABASE_URL?.trim().replace(/\/+$/, "");
  const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY?.trim();
  const anonKey = process.env.SUPABASE_ANON_KEY?.trim();
  if (!url || !serviceKey || !anonKey) throw new ApiError(503, "chat_not_configured");

  const email = `${user.id.replace(/-/g, "")}@chat.local`;
  const admin = {
    apikey: serviceKey,
    Authorization: `Bearer ${serviceKey}`,
    "Content-Type": "application/json",
  };

  const existing = await fetch(`${url}/auth/v1/admin/users/${user.id}`, { headers: admin, cache: "no-store" });
  if (existing.status === 404) {
    const created = await fetch(`${url}/auth/v1/admin/users`, {
      method: "POST",
      headers: admin,
      body: JSON.stringify({
        id: user.id,
        email,
        email_confirm: true,
        password: randomBytes(24).toString("base64url"),
        user_metadata: { name: user.name },
      }),
      cache: "no-store",
    });
    if (!created.ok) {
      console.error("supabase create user", created.status, await created.text().catch(() => ""));
      throw new ApiError(502, "chat_unavailable");
    }
  } else if (existing.ok) {
    await fetch(`${url}/auth/v1/admin/users/${user.id}`, {
      method: "PUT",
      headers: admin,
      body: JSON.stringify({ email, user_metadata: { name: user.name } }),
      cache: "no-store",
    }).catch(() => undefined);
  } else {
    console.error("supabase get user", existing.status, await existing.text().catch(() => ""));
    throw new ApiError(502, "chat_unavailable");
  }

  const linkRes = await fetch(`${url}/auth/v1/admin/generate_link`, {
    method: "POST",
    headers: admin,
    body: JSON.stringify({ type: "magiclink", email }),
    cache: "no-store",
  });
  const link = (await linkRes.json().catch(() => ({}))) as { email_otp?: string };
  if (!linkRes.ok || !link.email_otp) {
    console.error("supabase generate_link", linkRes.status, link);
    throw new ApiError(502, "chat_unavailable");
  }

  const verifyRes = await fetch(`${url}/auth/v1/verify`, {
    method: "POST",
    headers: {
      apikey: anonKey,
      Authorization: `Bearer ${anonKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ type: "magiclink", email, token: link.email_otp }),
    cache: "no-store",
  });
  const session = (await verifyRes.json().catch(() => ({}))) as {
    access_token?: string;
    expires_at?: number;
  };
  if (!verifyRes.ok || !session.access_token) {
    console.error("supabase verify", verifyRes.status, session);
    throw new ApiError(502, "chat_unavailable");
  }

  const expiresAt = typeof session.expires_at === "number" ? session.expires_at * 1000 : Date.now() + 3600_000;
  return { token: session.access_token, expiresAt };
}

function adminKey(): Buffer {
  const password = process.env.ADMIN_PASSWORD;
  const salt = process.env.RATING_SALT;
  if (!password || !salt) throw new ApiError(503, "not_configured");
  return createHash("sha256").update(`admin:${password}:${salt}`).digest();
}

export function checkAdminPassword(value: unknown): boolean {
  const password = process.env.ADMIN_PASSWORD;
  if (!password) throw new ApiError(503, "not_configured");
  return typeof value === "string" && safeEqual(sha256(value), sha256(password));
}

/** Changing ADMIN_PASSWORD invalidates every issued admin token. */
export function createAdminToken(hours = 12): { token: string; expiresAt: number } {
  const expiresAt = Date.now() + hours * 3600_000;
  const body = `adm.${expiresAt}`;
  const signature = createHmac("sha256", adminKey()).update(body).digest("base64url");
  return { token: `${body}.${signature}`, expiresAt };
}

export function requireAdmin(request: Request): void {
  const token = request.headers.get("x-admin-token") ?? "";
  const match = /^adm\.(\d+)\.([A-Za-z0-9_-]+)$/.exec(token);
  if (!match || Number(match[1]) < Date.now()) throw new ApiError(401, "unauthorized");
  const expected = createHmac("sha256", adminKey()).update(`adm.${match[1]}`).digest("base64url");
  if (!safeEqual(expected, match[2])) throw new ApiError(401, "unauthorized");
}

export function clientIp(request: Request): string {
  return (request.headers.get("x-forwarded-for") ?? "").split(",")[0].trim() || "unknown";
}
