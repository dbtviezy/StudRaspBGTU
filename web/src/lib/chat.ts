"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import { api, ApiFailure, getAccount } from "./account";
import { deviceId } from "./ratings";
import { newId, PREFIX } from "./store";

export interface ChatInfo {
  id: string;
  kind: "group" | "dm";
  title: string;
  groupId: string | null;
  inviteCode: string | null;
  role: "owner" | "member";
  accepted: boolean;
  peerName: string | null;
  lastId: number | null;
  lastBody: string | null;
  lastAuthor: string | null;
  lastAt: string | null;
  unread: number;
}

export interface Message {
  id: number | null;
  chatId: string;
  userId: string;
  author: string;
  body: string;
  createdAt: string;
  clientId: string;
  status: "sent" | "pending" | "failed";
}

interface Ticket {
  url: string;
  anonKey: string;
  token: string;
  expiresAt: number;
  userId: string;
}

export const MAX_MESSAGE = 1000;
const CHATS_KEY = `${PREFIX}chats`;
const OUTBOX_KEY = `${PREFIX}outbox`;
const chatKey = (id: string) => `${PREFIX}chat.${id}`;
const KEEP = 300;

let ticket: Ticket | null = null;
let ticketLoading: Promise<Ticket> | null = null;
const listeners = new Set<() => void>();
let version = 0;

function emit() {
  version++;
  listeners.forEach((listener) => listener());
}

function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw == null ? fallback : (JSON.parse(raw) as T);
  } catch {
    return fallback;
  }
}

function write(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {}
}

export function cachedChats(): ChatInfo[] {
  return read<ChatInfo[]>(CHATS_KEY, []);
}

function outbox(): Message[] {
  return read<Message[]>(OUTBOX_KEY, []);
}

export function cachedMessages(chatId: string): Message[] {
  const sent = read<Message[]>(chatKey(chatId), []);
  const waiting = outbox().filter((m) => m.chatId === chatId);
  return [...sent, ...waiting];
}

function storeMessages(chatId: string, incoming: Message[], replace = false) {
  const current = replace ? [] : read<Message[]>(chatKey(chatId), []);
  const byId = new Map<number, Message>();
  for (const message of [...current, ...incoming]) if (message.id != null) byId.set(message.id, message);
  const merged = [...byId.values()].sort((a, b) => (a.id ?? 0) - (b.id ?? 0)).slice(-KEEP);
  write(chatKey(chatId), merged);
}

/** Drops everything cached on this device; used on sign-out and data reset. */
export function clearChatCache() {
  try {
    for (const key of Object.keys(localStorage)) {
      if (key === CHATS_KEY || key === OUTBOX_KEY || key.startsWith(`${PREFIX}chat.`)) localStorage.removeItem(key);
    }
  } catch {}
  ticket = null;
  emit();
}

async function getTicket(force = false): Promise<Ticket> {
  if (!force && ticket && ticket.expiresAt - Date.now() > 120_000 && ticket.userId === getAccount().user?.id) return ticket;
  ticketLoading ??= api<Ticket>("/api/chat/token", { method: "POST" })
    .then((next) => (ticket = next))
    .finally(() => {
      ticketLoading = null;
    });
  return ticketLoading;
}

function errorCode(status: number, body: string): string {
  try {
    const parsed = JSON.parse(body) as { message?: string };
    const message = parsed.message ?? "";
    for (const code of ["rate_limited", "invalid_code", "forbidden", "bad_request"]) if (message.includes(code)) return code;
  } catch {}
  if (status === 401 || status === 403) return "forbidden";
  return status >= 500 ? "server_error" : "bad_request";
}

async function rest<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const current = await getTicket();
  let response: Response;
  try {
    response = await fetch(`${current.url}/rest/v1/${path}`, {
      ...init,
      headers: {
        apikey: current.anonKey,
        Authorization: `Bearer ${current.token}`,
        "Content-Type": "application/json",
        ...(init.headers ?? {}),
      },
      cache: "no-store",
    });
  } catch {
    throw new ApiFailure("offline");
  }
  const text = await response.text();
  if (response.status === 401 && retry) {
    await getTicket(true);
    return rest<T>(path, init, false);
  }
  if (!response.ok) throw new ApiFailure(errorCode(response.status, text));
  return (text ? JSON.parse(text) : null) as T;
}

const rpc = <T>(name: string, args: Record<string, unknown> = {}) => rest<T>(`rpc/${name}`, { method: "POST", body: JSON.stringify(args) });

function toChat(row: Record<string, unknown>): ChatInfo {
  return {
    id: String(row.id),
    kind: row.kind === "dm" ? "dm" : "group",
    title: String(row.title ?? ""),
    groupId: row.group_id ? String(row.group_id) : null,
    inviteCode: row.invite_code ? String(row.invite_code) : null,
    role: row.role === "owner" ? "owner" : "member",
    accepted: row.accepted !== false,
    peerName: row.peer_name ? String(row.peer_name) : null,
    lastId: row.last_id == null ? null : Number(row.last_id),
    lastBody: row.last_body ? String(row.last_body) : null,
    lastAuthor: row.last_author ? String(row.last_author) : null,
    lastAt: row.last_at ? String(row.last_at) : null,
    unread: Number(row.unread ?? 0),
  };
}

function toMessage(row: Record<string, unknown>): Message {
  return {
    id: Number(row.id),
    chatId: String(row.chat_id),
    userId: String(row.user_id),
    author: String(row.author),
    body: String(row.body),
    createdAt: String(row.created_at),
    clientId: String(row.client_id),
    status: "sent",
  };
}

export async function refreshChats(): Promise<ChatInfo[]> {
  const rows = await rpc<Record<string, unknown>[]>("my_chats");
  const chats = (rows ?? []).map(toChat);
  write(CHATS_KEY, chats);
  emit();
  return chats;
}

export async function refreshMessages(chatId: string): Promise<void> {
  const known = read<Message[]>(chatKey(chatId), []);
  const lastId = known.length ? known[known.length - 1].id ?? 0 : 0;
  const rows = lastId
    ? await rest<Record<string, unknown>[]>(`messages?chat_id=eq.${chatId}&id=gt.${lastId}&order=id.asc&limit=200`)
    : await rest<Record<string, unknown>[]>(`messages?chat_id=eq.${chatId}&order=id.desc&limit=100`);
  if (rows?.length) storeMessages(chatId, rows.map(toMessage));
  emit();
}

/** Re-reads the last 100 messages, dropping ones deleted on the server (by the author or a moderator). */
export async function resyncMessages(chatId: string): Promise<void> {
  const rows = await rest<Record<string, unknown>[]>(`messages?chat_id=eq.${chatId}&order=id.desc&limit=100`);
  storeMessages(chatId, (rows ?? []).map(toMessage), true);
  emit();
}

export function sendMessage(chatId: string, body: string): void {
  const text = body.trim();
  if (!text) return;
  const user = getAccount().user;
  const message: Message = {
    id: null,
    chatId,
    userId: user?.id ?? "",
    author: user?.name ?? "",
    body: text.slice(0, MAX_MESSAGE),
    createdAt: new Date().toISOString(),
    clientId: newId(),
    status: "pending",
  };
  write(OUTBOX_KEY, [...outbox(), message]);
  emit();
  flushOutbox();
}

let flushing: Promise<void> | null = null;
const TRANSIENT = new Set(["offline", "server_error", "unauthorized", "rate_limited", "chat_not_configured", "chat_unavailable"]);

/** Sends queued messages; anything that fails because of the network stays queued for the next try. */
export function flushOutbox(): Promise<void> {
  flushing ??= (async () => {
    for (const message of outbox()) {
      if (message.status === "failed") continue;
      try {
        const rows = await rest<Record<string, unknown>[]>("messages?on_conflict=client_id", {
          method: "POST",
          headers: { Prefer: "return=representation,resolution=ignore-duplicates" },
          body: JSON.stringify({ chat_id: message.chatId, body: message.body, client_id: message.clientId }),
        });
        write(OUTBOX_KEY, outbox().filter((m) => m.clientId !== message.clientId));
        if (rows?.length) storeMessages(message.chatId, rows.map(toMessage));
        emit();
      } catch (error) {
        const code = error instanceof ApiFailure ? error.code : "offline";
        if (TRANSIENT.has(code)) break;
        write(OUTBOX_KEY, outbox().map((m) => (m.clientId === message.clientId ? { ...m, status: "failed" as const } : m)));
        emit();
      }
    }
  })().finally(() => {
    flushing = null;
  });
  return flushing;
}

export function retryMessage(clientId: string) {
  write(OUTBOX_KEY, outbox().map((m) => (m.clientId === clientId ? { ...m, status: "pending" as const } : m)));
  emit();
  flushOutbox();
}

export function discardMessage(clientId: string) {
  write(OUTBOX_KEY, outbox().filter((m) => m.clientId !== clientId));
  emit();
}

export async function deleteOwnMessage(chatId: string, id: number) {
  await rest(`messages?id=eq.${id}`, { method: "DELETE" });
  write(chatKey(chatId), read<Message[]>(chatKey(chatId), []).filter((m) => m.id !== id));
  emit();
}

export async function createGroupChat(groupId: string, title: string): Promise<ChatInfo> {
  const row = await rpc<Record<string, unknown>>("create_group_chat", { p_group: groupId, p_title: title });
  await refreshChats();
  return toChat(row);
}

export async function joinChat(code: string): Promise<ChatInfo> {
  const row = await rpc<Record<string, unknown>>("join_chat", { p_code: code.trim().toUpperCase() });
  await refreshChats();
  return toChat(row);
}

export async function startDm(peerId: string, peerName: string): Promise<ChatInfo> {
  const row = await rpc<Record<string, unknown>>("start_dm", { p_peer: peerId, p_peer_name: peerName });
  await refreshChats();
  return toChat(row);
}

export async function acceptChat(chatId: string) {
  await rpc("accept_chat", { p_chat: chatId });
  await refreshChats();
}

export async function leaveChat(chatId: string) {
  await rpc("leave_chat", { p_chat: chatId });
  try {
    localStorage.removeItem(chatKey(chatId));
  } catch {}
  await refreshChats();
}

export async function regenerateInvite(chatId: string): Promise<string> {
  const code = await rpc<string>("regenerate_invite", { p_chat: chatId });
  await refreshChats();
  return code;
}

export async function markRead(chatId: string, id: number) {
  await rpc("mark_read", { p_chat: chatId, p_id: id }).catch(() => undefined);
  write(CHATS_KEY, cachedChats().map((c) => (c.id === chatId ? { ...c, unread: 0 } : c)));
  emit();
}

export async function lookupUser(query: string): Promise<{ id: string; name: string }> {
  return api(`/api/users/lookup?q=${encodeURIComponent(query.trim())}`);
}

export async function reportMessage(id: number, reason: string) {
  await api("/api/reports", { method: "POST", body: JSON.stringify({ deviceId: deviceId(), kind: "message", target: id, reason }) });
}

/**
 * Supabase Realtime (Phoenix protocol) subscription to new/deleted messages of one chat.
 * Only used as a trigger to re-fetch; callers also poll in case the socket is down.
 */
export function subscribeChat(chatId: string, onChange: () => void): () => void {
  let socket: WebSocket | null = null;
  let heartbeat: number | null = null;
  let closed = false;
  let ref = 0;
  const topic = `realtime:chat-${chatId}`;

  const open = async () => {
    try {
      const current = await getTicket();
      if (closed) return;
      const url = `${current.url.replace(/^http/, "ws")}/realtime/v1/websocket?apikey=${encodeURIComponent(current.anonKey)}&vsn=1.0.0`;
      socket = new WebSocket(url);
      socket.onopen = () => {
        socket?.send(JSON.stringify({
          topic,
          event: "phx_join",
          ref: String(++ref),
          join_ref: "1",
          payload: {
            config: {
              broadcast: { self: false },
              presence: { key: "" },
              postgres_changes: [{ event: "*", schema: "public", table: "messages", filter: `chat_id=eq.${chatId}` }],
              private: false,
            },
            access_token: current.token,
          },
        }));
        heartbeat = window.setInterval(() => {
          socket?.send(JSON.stringify({ topic: "phoenix", event: "heartbeat", payload: {}, ref: String(++ref) }));
          if (ticket && ticket.token !== current.token) {
            socket?.send(JSON.stringify({ topic, event: "access_token", payload: { access_token: ticket.token }, ref: String(++ref) }));
          }
        }, 25_000);
      };
      socket.onmessage = (event) => {
        try {
          const message = JSON.parse(String(event.data)) as { event?: string };
          if (message.event === "postgres_changes") onChange();
        } catch {}
      };
      socket.onclose = () => {
        if (heartbeat != null) window.clearInterval(heartbeat);
        heartbeat = null;
        if (!closed) window.setTimeout(open, 5000);
      };
    } catch {
      if (!closed) window.setTimeout(open, 15000);
    }
  };
  open();
  return () => {
    closed = true;
    if (heartbeat != null) window.clearInterval(heartbeat);
    socket?.close();
  };
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useChatVersion(): number {
  return useSyncExternalStore(subscribe, () => version, () => 0);
}

/** Keeps the outbox moving whenever the browser comes back online. */
export function useOutboxPump(enabled: boolean) {
  useEffect(() => {
    if (!enabled) return;
    const flush = () => {
      flushOutbox();
    };
    flush();
    window.addEventListener("online", flush);
    const timer = window.setInterval(flush, 30_000);
    return () => {
      window.removeEventListener("online", flush);
      window.clearInterval(timer);
    };
  }, [enabled]);
}

export function useOnline(): boolean {
  const [online, setOnline] = useState(true);
  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    update();
    window.addEventListener("online", update);
    window.addEventListener("offline", update);
    return () => {
      window.removeEventListener("online", update);
      window.removeEventListener("offline", update);
    };
  }, []);
  return online;
}
