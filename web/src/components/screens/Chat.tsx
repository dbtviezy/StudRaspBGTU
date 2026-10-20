"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { authConfig, useAccount } from "@/lib/account";
import {
  acceptChat,
  cachedChats,
  cachedMessages,
  type ChatInfo,
  createGroupChat,
  deleteOwnMessage,
  discardMessage,
  joinChat,
  leaveChat,
  lookupUser,
  markRead,
  MAX_MESSAGE,
  type Message,
  refreshChats,
  refreshMessages,
  regenerateInvite,
  reportMessage,
  resyncMessages,
  retryMessage,
  sendMessage,
  startDm,
  subscribeChat,
  useChatVersion,
  useOnline,
} from "@/lib/chat";
import type { Copy } from "@/lib/i18n";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { SignInSheet } from "../overlays/Account";
import ReportDialog from "../overlays/Report";
import { Alert, Centered, Chip, Dialog, useLongPress } from "../ui";

const errorOf = (s: Copy, e: unknown) => s.errorText(e instanceof Error ? e.message : "");

export function chatTitle(s: Copy, chat: ChatInfo): string {
  if (chat.kind === "dm") return chat.peerName ?? s.directChat;
  return chat.title || (chat.groupId ? `${s.groupChat} ${chat.groupId}` : s.groupChat);
}

function timeOf(iso: string | null): string {
  if (!iso) return "";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function shortWhen(s: Copy, iso: string | null): string {
  if (!iso) return "";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  const now = new Date();
  if (date.toDateString() === now.toDateString()) return timeOf(iso);
  return `${date.getDate()} ${s.monthsGenitive[date.getMonth()].slice(0, 3)}`;
}

function dayLabel(s: Copy, iso: string): string {
  const date = new Date(iso);
  const today = new Date();
  const yesterday = new Date(Date.now() - 86400_000);
  if (date.toDateString() === today.toDateString()) return s.today;
  if (date.toDateString() === yesterday.toDateString()) return s.code === "en" ? "Yesterday" : "Вчера";
  return `${date.getDate()} ${s.monthsGenitive[date.getMonth()]}`;
}

function FormDialog({
  title,
  hint,
  label,
  initial = "",
  action,
  maxLength,
  upper,
  onSubmit,
  onClose,
}: {
  title: string;
  hint?: string;
  label: string;
  initial?: string;
  action: string;
  maxLength: number;
  upper?: boolean;
  onSubmit: (value: string) => Promise<void>;
  onClose: () => void;
}) {
  const { s } = useShell();
  const [value, setValue] = useState(initial);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submit = async () => {
    if (!value.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await onSubmit(value.trim());
      onClose();
    } catch (e) {
      setError(errorOf(s, e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <Dialog onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 22 }}>{title}</h2>
      {hint && <div className="muted small mt8">{hint}</div>}
      <label className="field mt12">
        <span>{label}</span>
        <input
          className="input"
          value={value}
          maxLength={maxLength}
          autoCapitalize={upper ? "characters" : "none"}
          autoFocus
          onChange={(e) => setValue(upper ? e.target.value.toUpperCase() : e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
        />
      </label>
      {error && <div className="red small mt8">{error}</div>}
      <div className="dialog-actions">
        <button className="tbtn muted" onClick={onClose}>{s.cancel}</button>
        <button className="tbtn" disabled={busy || !value.trim()} onClick={submit}>{action}</button>
      </div>
    </Dialog>
  );
}

function InviteDialog({ chat, onClose }: { chat: ChatInfo; onClose: () => void }) {
  const { s, toast } = useShell();
  const [code, setCode] = useState(chat.inviteCode ?? "");
  const [error, setError] = useState<string | null>(null);
  return (
    <Dialog onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 22 }}>{s.inviteCode}</h2>
      <div className="code mt12 blue" style={{ textAlign: "center" }}>{code || "—"}</div>
      <div className="muted small mt12">{s.inviteShare}</div>
      {error && <div className="red small mt8">{error}</div>}
      <div className="dialog-actions">
        {chat.role === "owner" && (
          <button
            className="tbtn muted"
            onClick={() =>
              regenerateInvite(chat.id)
                .then(setCode)
                .catch((e) => setError(errorOf(s, e)))
            }
          >
            {s.newInviteCode}
          </button>
        )}
        <button
          className="tbtn"
          onClick={() => {
            navigator.clipboard?.writeText(code).then(() => toast(s.codeCopied)).catch(() => undefined);
          }}
        >
          {s.copyCode}
        </button>
      </div>
    </Dialog>
  );
}

function MessageBubble({ message, mine, showAuthor, onMenu }: { message: Message; mine: boolean; showAuthor: boolean; onMenu: () => void }) {
  const { s } = useShell();
  const press = useLongPress(onMenu, () => message.status !== "sent" && onMenu());
  const meta = message.status === "pending" ? s.pendingMessage : message.status === "failed" ? s.failedMessage : timeOf(message.createdAt);
  return (
    <div className={`bubble-row${mine ? " mine" : ""}`}>
      <div className={`bubble ${message.status}`} {...press}>
        {showAuthor && !mine && <div className="who">{message.author}</div>}
        {message.body}
        <div className="meta">
          {message.status === "pending" && <Icon name="schedule" size={11} />} {meta}
        </div>
      </div>
    </div>
  );
}

function ChatView({ chat, onClose, onOpenChat }: { chat: ChatInfo; onClose: () => void; onOpenChat: (chat: ChatInfo) => void }) {
  const { s, toast } = useShell();
  const { user } = useAccount();
  const online = useOnline();
  useChatVersion();
  const [text, setText] = useState("");
  const [menu, setMenu] = useState<Message | null>(null);
  const [reporting, setReporting] = useState<Message | null>(null);
  const [leaving, setLeaving] = useState(false);
  const [invite, setInvite] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const log = useRef<HTMLDivElement>(null);
  const live = cachedChats().find((c) => c.id === chat.id) ?? chat;
  const messages = cachedMessages(chat.id);
  const lastId = messages.reduce((max, m) => Math.max(max, m.id ?? 0), 0);
  const request = live.kind === "dm" && !live.accepted;

  useEffect(() => {
    let alive = true;
    const pull = () => refreshMessages(chat.id).catch((e) => alive && setError(errorOf(s, e)));
    resyncMessages(chat.id).catch(() => pull());
    const stop = subscribeChat(chat.id, () => pull());
    const timer = window.setInterval(pull, 15_000);
    return () => {
      alive = false;
      stop();
      window.clearInterval(timer);
    };
  }, [chat.id, s]);

  useEffect(() => {
    if (lastId > 0) markRead(chat.id, lastId);
  }, [chat.id, lastId]);

  useEffect(() => {
    const element = log.current;
    if (element) element.scrollTop = element.scrollHeight;
  }, [messages.length]);

  const send = () => {
    if (!text.trim()) return;
    sendMessage(chat.id, text);
    setText("");
  };

  return (
    <div className="chat-view" role="dialog" aria-modal="true">
      <div className="chat-head">
        <div className="inner row">
          <button className="icon-btn" aria-label={s.back} onClick={onClose}>
            <Icon name="arrowBack" />
          </button>
          <div className="grow">
            <div className="semibold ellipsis">{chatTitle(s, live)}</div>
            <div className="muted xsmall ellipsis">{live.kind === "dm" ? s.directChat : `${s.groupChat}${live.groupId ? ` · ${live.groupId}` : ""}`}</div>
          </div>
          {live.kind === "group" && live.inviteCode && (
            <button className="icon-btn" aria-label={s.inviteCode} onClick={() => setInvite(true)}>
              <Icon name="add" />
            </button>
          )}
          <button className="icon-btn" aria-label={s.leaveChat} onClick={() => setLeaving(true)}>
            <Icon name="logout" />
          </button>
        </div>
      </div>
      {!online && <div className="offline-bar">{s.offlineBanner}</div>}
      <div className="chat-log" ref={log}>
        <div className="inner">
          {messages.length === 0 && <div className="muted small" style={{ textAlign: "center", marginTop: 24 }}>{s.noMessages}</div>}
          {messages.map((message, index) => {
            const day = new Date(message.createdAt).toDateString();
            const before = index > 0 ? new Date(messages[index - 1].createdAt).toDateString() : "";
            const separator = day !== before ? <div className="day-sep">{dayLabel(s, message.createdAt)}</div> : null;
            return (
              <div key={message.clientId || message.id}>
                {separator}
                <MessageBubble message={message} mine={message.userId === user?.id || message.id == null} showAuthor={live.kind === "group"} onMenu={() => setMenu(message)} />
              </div>
            );
          })}
          {error && <div className="red small mt8" style={{ textAlign: "center" }}>{error}</div>}
          <div className="muted xsmall mt16" style={{ textAlign: "center" }}>{s.chatRetention}</div>
        </div>
      </div>
      {request ? (
        <div className="chat-input">
          <div className="inner">
            <div className="small">{`${live.peerName ?? ""} ${s.chatRequest}`}</div>
            <div className="row mt8" style={{ justifyContent: "flex-end" }}>
              <button className="tbtn danger" onClick={() => leaveChat(chat.id).then(onClose).catch((e) => setError(errorOf(s, e)))}>{s.decline}</button>
              <button className="tbtn" onClick={() => acceptChat(chat.id).catch((e) => setError(errorOf(s, e)))}>{s.accept}</button>
            </div>
          </div>
        </div>
      ) : (
        <div className="chat-input">
          <div className="inner row" style={{ alignItems: "flex-end" }}>
            <textarea
              rows={1}
              value={text}
              maxLength={MAX_MESSAGE}
              placeholder={s.messagePlaceholder}
              onChange={(e) => setText(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  send();
                }
              }}
            />
            <button className="icon-btn blue" aria-label={s.sendMessage} disabled={!text.trim()} onClick={send}>
              <Icon name="send" />
            </button>
          </div>
        </div>
      )}

      {menu && (
        <Centered onClose={() => setMenu(null)}>
          <div className="stack tight" style={{ minWidth: 240 }}>
            {menu.status === "failed" && (
              <button className="tbtn" onClick={() => { retryMessage(menu.clientId); setMenu(null); }}>{s.retry}</button>
            )}
            {menu.status !== "sent" && (
              <button className="tbtn danger" onClick={() => { discardMessage(menu.clientId); setMenu(null); }}>{s.delete}</button>
            )}
            {menu.status === "sent" && (
              <button
                className="tbtn"
                onClick={() => {
                  navigator.clipboard?.writeText(menu.body).then(() => toast(s.copied)).catch(() => undefined);
                  setMenu(null);
                }}
              >
                {s.copyText}
              </button>
            )}
            {menu.status === "sent" && menu.userId === user?.id && menu.id != null && (
              <button
                className="tbtn danger"
                onClick={() => {
                  deleteOwnMessage(chat.id, menu.id!).catch((e) => toast(errorOf(s, e)));
                  setMenu(null);
                }}
              >
                {s.delete}
              </button>
            )}
            {menu.status === "sent" && menu.userId !== user?.id && live.kind === "group" && (
              <button
                className="tbtn"
                onClick={() => {
                  const peer = menu;
                  setMenu(null);
                  startDm(peer.userId, peer.author).then(onOpenChat).catch((e) => toast(errorOf(s, e)));
                }}
              >
                {s.newDirect}
              </button>
            )}
            {menu.status === "sent" && menu.userId !== user?.id && (
              <button className="tbtn danger" onClick={() => { setReporting(menu); setMenu(null); }}>{s.report}</button>
            )}
          </div>
        </Centered>
      )}
      {reporting && reporting.id != null && (
        <ReportDialog onSend={(reason) => reportMessage(reporting.id!, reason)} onClose={() => setReporting(null)} />
      )}
      {invite && <InviteDialog chat={live} onClose={() => setInvite(false)} />}
      {leaving && (
        <Alert title={s.leaveChat} text={s.leaveChatText} onClose={() => setLeaving(false)}>
          <button className="tbtn muted" onClick={() => setLeaving(false)}>{s.cancel}</button>
          <button
            className="tbtn danger"
            onClick={() => {
              setLeaving(false);
              leaveChat(chat.id).then(onClose).catch((e) => toast(errorOf(s, e)));
            }}
          >
            {s.leaveChat}
          </button>
        </Alert>
      )}
    </div>
  );
}

function ChatRow({ chat, onOpen }: { chat: ChatInfo; onOpen: () => void }) {
  const { s } = useShell();
  const title = chatTitle(s, chat);
  const preview = !chat.accepted
    ? `${chat.peerName ?? ""} ${s.chatRequest}`
    : chat.lastBody
      ? `${chat.kind === "group" && chat.lastAuthor ? `${chat.lastAuthor}: ` : ""}${chat.lastBody}`
      : s.noMessages;
  return (
    <button className="card row" style={{ gap: 12 }} onClick={onOpen}>
      <span className="avatar">{chat.kind === "group" ? <Icon name="groups" size={22} /> : title[0]?.toUpperCase()}</span>
      <div className="grow">
        <div className="row">
          <span className="semibold grow ellipsis">{title}</span>
          <span className="muted xsmall">{shortWhen(s, chat.lastAt)}</span>
        </div>
        <div className="row mt2">
          <span className={`small grow ellipsis ${chat.accepted ? "muted" : "blue"}`}>{preview}</span>
          {chat.unread > 0 && <span className="badge">{s.unreadCount(chat.unread)}</span>}
        </div>
      </div>
    </button>
  );
}

export default function ChatScreen() {
  const { s, group, toast } = useShell();
  const { user } = useAccount();
  const online = useOnline();
  const version = useChatVersion();
  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [signing, setSigning] = useState(false);
  const [opened, setOpened] = useState<ChatInfo | null>(null);
  const [dialog, setDialog] = useState<"create" | "join" | "direct" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const chats = useMemo(() => (version >= 0 ? cachedChats() : []), [version]);

  useEffect(() => {
    authConfig()
      .then((config) => setEnabled(config.chat))
      .catch(() => setEnabled(null));
  }, []);

  useEffect(() => {
    if (!user || enabled === false) return;
    let alive = true;
    const pull = () => refreshChats().then(() => alive && setError(null)).catch((e) => alive && setError(errorOf(s, e)));
    pull();
    const timer = window.setInterval(pull, 20_000);
    return () => {
      alive = false;
      window.clearInterval(timer);
    };
  }, [user, enabled, s]);

  return (
    <div className="stack">
      <h1 className="h1">{s.chatsTitle}</h1>

      {!user ? (
        <div className="card">
          <div className="small">{s.chatsSignIn}</div>
          <button className="btn mt12" onClick={() => setSigning(true)}>{s.signIn}</button>
        </div>
      ) : enabled === false ? (
        <div className="info-box">
          <span className="blue">
            <Icon name="info" size={20} />
          </span>
          <div className="small">{s.chatsOff}</div>
        </div>
      ) : (
        <>
          {!online && <div className="offline-bar" style={{ borderRadius: 14 }}>{s.offlineBanner}</div>}
          <div className="scroll-x">
            <Chip on={false} onClick={() => setDialog("create")}>{s.createGroupChat}</Chip>
            <Chip on={false} onClick={() => setDialog("join")}>{s.joinByCode}</Chip>
            <Chip on={false} onClick={() => setDialog("direct")}>{s.newDirect}</Chip>
          </div>
          {error && <div className="red small">{error}</div>}
          {chats.length === 0 && <div className="muted small" style={{ padding: 8 }}>{s.noChats}</div>}
          {chats.map((chat) => (
            <ChatRow key={chat.id} chat={chat} onOpen={() => setOpened(chat)} />
          ))}
        </>
      )}

      <div className="muted xsmall" style={{ padding: "0 8px" }}>{s.chatRetention}</div>
      <div className="muted xsmall" style={{ padding: "0 8px" }}>{s.nearbyAndroidOnly}</div>

      {signing && <SignInSheet onClose={() => setSigning(false)} />}
      {opened && <ChatView key={opened.id} chat={opened} onClose={() => setOpened(null)} onOpenChat={setOpened} />}
      {dialog === "create" && (
        <FormDialog
          title={s.createGroupChat}
          hint={s.inviteShare}
          label={s.chatTitleField}
          initial={`${s.groupChat} ${group.id}`}
          action={s.createGroupChat}
          maxLength={60}
          onSubmit={async (title) => setOpened(await createGroupChat(group.id, title))}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === "join" && (
        <FormDialog
          title={s.joinByCode}
          hint={s.inviteCodeHint}
          label={s.inviteCode}
          action={s.joinByCode}
          maxLength={8}
          upper
          onSubmit={async (code) => setOpened(await joinChat(code))}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === "direct" && (
        <FormDialog
          title={s.newDirect}
          label={s.directHint}
          action={s.find}
          maxLength={24}
          onSubmit={async (query) => {
            const peer = await lookupUser(query);
            const chat = await startDm(peer.id, peer.name);
            toast(s.waitingAccept);
            setOpened(chat);
          }}
          onClose={() => setDialog(null)}
        />
      )}
    </div>
  );
}
