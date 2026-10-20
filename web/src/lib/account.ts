"use client";

import { useEffect, useSyncExternalStore } from "react";
import { PREFIX } from "./store";

export interface User {
  id: string;
  name: string;
  login: string | null;
  telegram: boolean;
  google: boolean;
}

export interface AuthConfig {
  password: boolean;
  telegramBot: string | null;
  googleClientId: string | null;
  chat: boolean;
}

export class ApiFailure extends Error {
  constructor(public code: string) {
    super(code);
  }
}

const TOKEN_KEY = `${PREFIX}session`;
const USER_KEY = `${PREFIX}account`;

interface AccountState {
  token: string | null;
  user: User | null;
}

let state: AccountState = { token: null, user: null };
let loaded = false;
const listeners = new Set<() => void>();
const EMPTY: AccountState = { token: null, user: null };

function load() {
  if (loaded || typeof window === "undefined") return;
  loaded = true;
  try {
    state = {
      token: localStorage.getItem(TOKEN_KEY),
      user: JSON.parse(localStorage.getItem(USER_KEY) ?? "null") as User | null,
    };
  } catch {
    state = EMPTY;
  }
}

function publish(next: AccountState) {
  state = next;
  try {
    if (next.token) localStorage.setItem(TOKEN_KEY, next.token);
    else localStorage.removeItem(TOKEN_KEY);
    if (next.user) localStorage.setItem(USER_KEY, JSON.stringify(next.user));
    else localStorage.removeItem(USER_KEY);
  } catch {}
  listeners.forEach((listener) => listener());
}

export function getAccount(): AccountState {
  load();
  return state;
}

export function useAccount(): AccountState {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    getAccount,
    () => EMPTY,
  );
}

/** Short public ID other students can use to start a direct chat. */
export const shortId = (user: User) => user.id.replace(/-/g, "").slice(0, 8).toUpperCase();

export async function api<T>(path: string, init: RequestInit = {}, auth = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  const token = getAccount().token;
  if (auth && token) headers.set("Authorization", `Bearer ${token}`);
  let response: Response;
  try {
    response = await fetch(path, { ...init, headers, cache: "no-store" });
  } catch {
    throw new ApiFailure("offline");
  }
  const body = (await response.json().catch(() => null)) as (T & { error?: string }) | null;
  if (response.status === 401 && auth && token && path !== "/api/auth/login") publish(EMPTY);
  if (!response.ok || !body) throw new ApiFailure(body?.error ?? (response.status >= 500 ? "server_error" : "bad_request"));
  return body;
}

let config: Promise<AuthConfig> | null = null;

export function authConfig(): Promise<AuthConfig> {
  config ??= api<AuthConfig>("/api/auth/config", {}, false).catch((error) => {
    config = null;
    throw error;
  });
  return config;
}

type Session = { token: string; user: User };

function signedIn(result: Session | { user: User }) {
  publish({ token: "token" in result ? result.token : getAccount().token, user: result.user });
}

export async function register(login: string, password: string, name: string) {
  signedIn(await api<Session>("/api/auth/register", { method: "POST", body: JSON.stringify({ login, password, name }) }, false));
}

export async function signIn(login: string, password: string) {
  signedIn(await api<Session>("/api/auth/login", { method: "POST", body: JSON.stringify({ login, password }) }, false));
}

/** Also links Telegram to the current account when signed in. */
export async function signInTelegram(payload: Record<string, unknown>) {
  signedIn(await api<Session | { user: User }>("/api/auth/telegram", { method: "POST", body: JSON.stringify(payload) }));
}

export async function signInGoogle(credential: string) {
  signedIn(await api<Session | { user: User }>("/api/auth/google", { method: "POST", body: JSON.stringify({ credential }) }));
}

export async function updateAccount(patch: Record<string, unknown>) {
  signedIn(await api<{ user: User }>("/api/auth/me", { method: "PATCH", body: JSON.stringify(patch) }));
}

export async function refreshAccount() {
  if (!getAccount().token) return;
  try {
    signedIn(await api<{ user: User }>("/api/auth/me"));
  } catch {}
}

export async function signOut(options: { everywhere?: boolean; deleteAccount?: boolean } = {}) {
  try {
    await api("/api/auth/me", { method: "DELETE", body: JSON.stringify(options) });
  } catch (error) {
    if (options.deleteAccount) throw error;
  }
  publish(EMPTY);
}

/** Local sign-out without a server call, used by the data reset. */
export function forgetAccount() {
  publish(EMPTY);
}

export function useAccountRefresh() {
  useEffect(() => {
    refreshAccount();
  }, []);
}
