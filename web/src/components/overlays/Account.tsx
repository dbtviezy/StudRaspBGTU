"use client";

import { useEffect, useState } from "react";
import {
  type AuthConfig,
  authConfig,
  register,
  signIn,
  signInGoogle,
  signInTelegram,
  signOut,
  shortId,
  updateAccount,
  useAccount,
} from "@/lib/account";
import { clearChatCache } from "@/lib/chat";
import { disablePush } from "@/lib/push";
import { useShell } from "../context";
import { GoogleLogin, TelegramLogin } from "../auth/Providers";
import { Icon } from "../Icons";
import { Alert, Detail, Sheet } from "../ui";

function useAuthConfig(): AuthConfig | null | "failed" {
  const [config, setConfig] = useState<AuthConfig | null | "failed">(null);
  useEffect(() => {
    let alive = true;
    authConfig()
      .then((value) => alive && setConfig(value))
      .catch(() => alive && setConfig("failed"));
    return () => {
      alive = false;
    };
  }, []);
  return config;
}

function useAction() {
  const { s } = useShell();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const run = async (body: () => Promise<unknown>, after?: () => void) => {
    setBusy(true);
    setError(null);
    try {
      await body();
      after?.();
    } catch (e) {
      setError(s.errorText(e instanceof Error ? e.message : ""));
    } finally {
      setBusy(false);
    }
  };
  return { busy, error, setError, run };
}

/** Sign-in / sign-up sheet. Closes itself once a session exists. */
export function SignInSheet({ onClose }: { onClose: () => void }) {
  const { s, data } = useShell();
  const config = useAuthConfig();
  const [mode, setMode] = useState<"in" | "up">("in");
  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState(data.name.trim());
  const { busy, error, run } = useAction();
  const ready = config && config !== "failed";

  const submit = () =>
    run(() => (mode === "in" ? signIn(login.trim(), password) : register(login.trim(), password, name.trim())), onClose);

  return (
    <Sheet
      onClose={onClose}
      foot={
        <div className="row" style={{ justifyContent: "flex-end", gap: 4 }}>
          <button className="tbtn muted" onClick={onClose}>{s.cancel}</button>
          <button className="tbtn" disabled={busy || !login.trim() || !password} onClick={submit}>
            {mode === "in" ? s.signIn : s.signUp}
          </button>
        </div>
      }
    >
      <h2 className="h2" style={{ fontSize: 20 }}>{mode === "in" ? s.signIn : s.signUp}</h2>
      <div className="muted small mt4">{s.accountHint}</div>
      {config === "failed" && <div className="red small mt8">{s.serverOff}</div>}

      {ready && config.telegramBot && (
        <div className="mt16">
          <TelegramLogin bot={config.telegramBot} onAuth={(user) => run(() => signInTelegram(user), onClose)} />
        </div>
      )}
      {ready && config.googleClientId && (
        <div className="mt8">
          <GoogleLogin clientId={config.googleClientId} dark={data.dark} onCredential={(credential) => run(() => signInGoogle(credential), onClose)} />
        </div>
      )}
      {ready && (!config.telegramBot || !config.googleClientId) && (
        <div className="mt12 stack tight">
          {!config.telegramBot && <button className="provider-btn" disabled>{s.withTelegram}</button>}
          {!config.googleClientId && <button className="provider-btn" disabled>{s.withGoogle}</button>}
          <div className="muted xsmall">{s.providerOff}</div>
        </div>
      )}

      <div className="or-line">{s.orWord}</div>

      <label className="field">
        <span>{s.loginField}</span>
        <input className="input" value={login} autoComplete="username" autoCapitalize="none" maxLength={24} onChange={(e) => setLogin(e.target.value)} />
      </label>
      {mode === "up" && <div className="muted xsmall mt2">{s.loginRule}</div>}
      <label className="field mt8">
        <span>{s.passwordField}</span>
        <input
          className="input"
          type="password"
          value={password}
          autoComplete={mode === "in" ? "current-password" : "new-password"}
          maxLength={100}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && mode === "in" && submit()}
        />
      </label>
      {mode === "up" && (
        <>
          <div className="muted xsmall mt2">{s.passwordRule}</div>
          <label className="field mt8">
            <span>{s.chatName}</span>
            <input className="input" value={name} maxLength={40} autoComplete="nickname" onChange={(e) => setName(e.target.value)} />
          </label>
          <div className="muted xsmall mt2">{s.chatNameRule}</div>
        </>
      )}
      {error && <div className="red small mt8">{error}</div>}
      <button className="tbtn flush mt8" onClick={() => setMode(mode === "in" ? "up" : "in")}>
        {mode === "in" ? s.noAccount : s.haveAccount}
      </button>
    </Sheet>
  );
}

function EditSheet({ kind, onClose }: { kind: "name" | "password" | "credentials"; onClose: () => void }) {
  const { s } = useShell();
  const { user } = useAccount();
  const [name, setName] = useState(user?.name ?? "");
  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");
  const [next, setNext] = useState("");
  const { busy, error, run } = useAction();
  const title = kind === "name" ? s.chatName : kind === "password" ? s.changePassword : s.setPassword;

  const save = () =>
    run(
      () =>
        kind === "name"
          ? updateAccount({ name: name.trim() })
          : kind === "password"
            ? updateAccount({ password, newPassword: next })
            : updateAccount({ login: login.trim(), password }),
      onClose,
    );

  return (
    <Sheet
      onClose={onClose}
      foot={
        <div className="row" style={{ justifyContent: "flex-end", gap: 4 }}>
          <button className="tbtn muted" onClick={onClose}>{s.cancel}</button>
          <button className="tbtn" disabled={busy} onClick={save}>{s.save}</button>
        </div>
      }
    >
      <h2 className="h2" style={{ fontSize: 20 }}>{title}</h2>
      {kind === "name" && (
        <>
          <input className="input mt12" value={name} maxLength={40} onChange={(e) => setName(e.target.value)} />
          <div className="muted xsmall mt2">{s.chatNameRule}</div>
        </>
      )}
      {kind === "credentials" && (
        <>
          <label className="field mt12">
            <span>{s.loginField}</span>
            <input className="input" value={login} autoCapitalize="none" maxLength={24} onChange={(e) => setLogin(e.target.value)} />
          </label>
          <div className="muted xsmall mt2">{s.loginRule}</div>
        </>
      )}
      {kind !== "name" && (
        <label className="field mt8">
          <span>{s.passwordField}</span>
          <input className="input" type="password" value={password} maxLength={100} onChange={(e) => setPassword(e.target.value)} />
        </label>
      )}
      {kind === "password" && (
        <label className="field mt8">
          <span>{s.newPassword}</span>
          <input className="input" type="password" value={next} maxLength={100} autoComplete="new-password" onChange={(e) => setNext(e.target.value)} />
        </label>
      )}
      {kind !== "name" && <div className="muted xsmall mt2">{s.passwordRule}</div>}
      {error && <div className="red small mt8">{error}</div>}
    </Sheet>
  );
}

/** Account card for the profile screen. */
export function AccountCard() {
  const { s, data, toast } = useShell();
  const { user } = useAccount();
  const config = useAuthConfig();
  const [signing, setSigning] = useState(false);
  const [editing, setEditing] = useState<"name" | "password" | "credentials" | null>(null);
  const [deleting, setDeleting] = useState(false);
  const { error, run } = useAction();
  const ready = config && config !== "failed";

  if (!user) {
    return (
      <div className="card">
        <h2 className="h2">{s.account}</h2>
        <div className="muted small mt4">{s.accountHint}</div>
        <button className="btn mt12" onClick={() => setSigning(true)}>{s.signIn}</button>
        {signing && <SignInSheet onClose={() => setSigning(false)} />}
      </div>
    );
  }

  const leave = (options?: { everywhere?: boolean; deleteAccount?: boolean }) =>
    run(async () => {
      await signOut(options);
      await disablePush();
      clearChatCache();
    });

  return (
    <div className="card">
      <div className="row">
        <span className="avatar">{user.name[0]?.toUpperCase() ?? "?"}</span>
        <div className="grow">
          <div className="semibold">{user.name}</div>
          <div className="muted xsmall">{user.login ? `@${user.login}` : s.account}</div>
        </div>
        <button className="icon-btn" aria-label={s.chatName} onClick={() => setEditing("name")}>
          <Icon name="chevronRight" />
        </button>
      </div>
      <Detail label={s.yourId} value={shortId(user)} />
      <div className="muted xsmall">{s.yourIdHint}</div>

      <div className="mt12 stack tight">
        <div className="row">
          <span className="grow small">{`Telegram${user.telegram ? ` · ${s.linked}` : ""}`}</span>
          {user.telegram ? (
            <button className="tbtn muted" onClick={() => run(() => updateAccount({ unlink: "telegram" }))}>{s.unlink}</button>
          ) : ready && config.telegramBot ? (
            <TelegramLogin bot={config.telegramBot} onAuth={(payload) => run(() => signInTelegram(payload))} />
          ) : (
            <span className="muted xsmall">{s.providerOff}</span>
          )}
        </div>
        <div className="row">
          <span className="grow small">{`Google${user.google ? ` · ${s.linked}` : ""}`}</span>
          {user.google ? (
            <button className="tbtn muted" onClick={() => run(() => updateAccount({ unlink: "google" }))}>{s.unlink}</button>
          ) : ready && config.googleClientId ? (
            <GoogleLogin clientId={config.googleClientId} dark={data.dark} onCredential={(credential) => run(() => signInGoogle(credential))} />
          ) : (
            <span className="muted xsmall">{s.providerOff}</span>
          )}
        </div>
      </div>
      {error && <div className="red small mt8">{error}</div>}

      <div className="row mt8" style={{ marginLeft: -8, flexWrap: "wrap" }}>
        {user.login ? (
          <button className="tbtn" onClick={() => setEditing("password")}>{s.changePassword}</button>
        ) : (
          <button className="tbtn" onClick={() => setEditing("credentials")}>{s.setPassword}</button>
        )}
        <button className="tbtn" onClick={() => leave()}>{s.signOut}</button>
        <button className="tbtn muted" onClick={() => leave({ everywhere: true }).then(() => toast(s.signOut))}>{s.signOutEverywhere}</button>
        <button className="tbtn danger" onClick={() => setDeleting(true)}>{s.deleteAccount}</button>
      </div>

      {editing && <EditSheet kind={editing} onClose={() => setEditing(null)} />}
      {deleting && (
        <Alert title={s.deleteAccount} text={s.deleteAccountText} onClose={() => setDeleting(false)}>
          <button className="tbtn muted" onClick={() => setDeleting(false)}>{s.cancel}</button>
          <button
            className="tbtn danger"
            onClick={() => {
              setDeleting(false);
              leave({ deleteAccount: true });
            }}
          >
            {s.delete}
          </button>
        </Alert>
      )}
    </div>
  );
}
