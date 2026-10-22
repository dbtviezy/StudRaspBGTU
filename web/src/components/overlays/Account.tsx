"use client";

import { useEffect, useState } from "react";
import {
  type AuthConfig,
  authConfig,
  cancelRep,
  type DeviceSession,
  endSessionById,
  fetchMyReviews,
  fetchSessions,
  type MyReview,
  requestRep,
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
import { claimableReviews } from "@/lib/ratings";
import { lastSyncAt, syncNow, useSyncStatus } from "@/lib/sync";
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
      setError(s.failureText(e));
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

function MyReviewsSheet({ onClose }: { onClose: () => void }) {
  const { s, openTeacher } = useShell();
  const [items, setItems] = useState<MyReview[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    fetchMyReviews()
      .then((list) => alive && setItems(list))
      .catch((e) => alive && setError(s.failureText(e)));
    return () => {
      alive = false;
    };
  }, [s]);

  return (
    <Sheet onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 20 }}>{s.myReviews}</h2>
      {error && <div className="red small mt8">{error}</div>}
      {items?.length === 0 && <div className="muted small mt8">{s.noMyReviews}</div>}
      {items?.map((review) => (
        <button
          key={review.id}
          className="list-row mt8"
          style={{ width: "100%", textAlign: "left" }}
          onClick={() => {
            onClose();
            openTeacher(review.teacher);
          }}
        >
          <div className="grow">
            <div className="semibold small">
              {review.teacher}
              {review.hidden && <span className="muted xsmall">{` · ${s.hiddenReview}`}</span>}
            </div>
            <div className="muted xsmall">{`★ ${review.overall} · ${s.moment(review.updatedAt)} · 👍 ${review.likes} · 👎 ${review.dislikes}`}</div>
            {review.text && <div className="small mt2 ellipsis">{review.text}</div>}
          </div>
          <Icon name="chevronRight" />
        </button>
      ))}
    </Sheet>
  );
}

function DevicesSheet({ onClose }: { onClose: () => void }) {
  const { s, toast } = useShell();
  const [items, setItems] = useState<DeviceSession[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let alive = true;
    fetchSessions()
      .then((list) => alive && setItems(list))
      .catch((e) => alive && setError(s.failureText(e)));
    return () => {
      alive = false;
    };
  }, [s, attempt]);

  const end = (id: number) =>
    endSessionById(id)
      .then(() => setAttempt((n) => n + 1))
      .catch((e) => toast(s.failureText(e)));

  return (
    <Sheet onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 20 }}>{s.devices}</h2>
      {error && <div className="red small mt8">{error}</div>}
      {items?.map((item) => (
        <div key={item.id} className="row mt12" style={{ gap: 10 }}>
          <div className="grow">
            <div className="semibold small">
              {item.device || s.unknownDevice}
              {item.current && <span className="muted xsmall">{` · ${s.thisDevice}`}</span>}
            </div>
            <div className="muted xsmall">{s.deviceSeen(item.usedAt)}</div>
          </div>
          {!item.current && <button className="tbtn muted" onClick={() => end(item.id)}>{s.signOut}</button>}
        </div>
      ))}
    </Sheet>
  );
}

function RepBlock() {
  const { s, data, group, toast } = useShell();
  const { rep } = useAccount();
  const [writing, setWriting] = useState(false);
  const [comment, setComment] = useState("");
  const { busy, error, run } = useAction();
  const groupId = data.group ? group.id : null;
  const current = rep && (rep.status === "pending" || rep.status === "approved") ? rep : null;

  return (
    <div className="mt16">
      <div className="semibold small">{s.repTitle}</div>
      <div className="muted xsmall mt2">{s.repHint}</div>
      {current?.status === "approved" && <div className="small mt8"><span className="badge-rep" style={{ marginLeft: 0 }}>{s.roleRep}</span> {s.repApproved(current.groupId)}</div>}
      {current?.status === "pending" && <div className="small mt8">{s.repPending(current.groupId)}</div>}
      {rep?.status === "rejected" && <div className="muted small mt8">{s.repRejected}</div>}
      {current ? (
        <button className="tbtn muted flush mt4" disabled={busy} onClick={() => run(cancelRep)}>
          {current.status === "approved" ? s.repStepDown : s.repCancel}
        </button>
      ) : !groupId ? (
        <div className="muted small mt8">{s.repNoGroup}</div>
      ) : writing ? (
        <>
          <textarea className="input mt8" rows={3} maxLength={300} placeholder={s.repComment} value={comment} onChange={(e) => setComment(e.target.value)} />
          <div className="row mt4" style={{ justifyContent: "flex-end", gap: 4 }}>
            <button className="tbtn muted" onClick={() => setWriting(false)}>{s.cancel}</button>
            <button
              className="tbtn"
              disabled={busy}
              onClick={() =>
                run(() => requestRep(groupId, groupId, comment.trim()), () => {
                  setWriting(false);
                  setComment("");
                  toast(s.repPending(groupId));
                })
              }
            >
              {s.repSend}
            </button>
          </div>
        </>
      ) : (
        <button className="tbtn flush mt4" onClick={() => setWriting(true)}>{`${s.repRequest} · ${groupId}`}</button>
      )}
      {error && <div className="red small mt4">{error}</div>}
    </div>
  );
}

function SyncBlock() {
  const { s } = useShell();
  const status = useSyncStatus();
  const at = status.at ?? lastSyncAt();
  return (
    <div className="mt16">
      <div className="semibold small">{s.syncTitle}</div>
      <div className="muted xsmall mt2">{s.syncHint}</div>
      <div className="row mt4">
        <span className={`grow xsmall ${status.failed ? "red" : "muted"}`}>
          {status.failed ? s.syncFailed : at ? s.syncDone(at) : ""}
        </span>
        <button className="tbtn" disabled={status.busy} onClick={() => syncNow().catch(() => undefined)}>{s.syncNow}</button>
      </div>
    </div>
  );
}

function LegacyNotice() {
  const { s } = useShell();
  const [warning, setWarning] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    claimableReviews()
      .then(({ count, until }) => alive && count > 0 && setWarning(s.legacyWarn(until)))
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, [s]);
  return warning ? <div className="warn small mt8">{warning}</div> : null;
}

/** Account card for the profile screen. */
export function AccountCard() {
  const { s, data, toast } = useShell();
  const { user, restrictions } = useAccount();
  const config = useAuthConfig();
  const [signing, setSigning] = useState(false);
  const [editing, setEditing] = useState<"name" | "password" | "credentials" | null>(null);
  const [sheet, setSheet] = useState<"reviews" | "devices" | null>(null);
  const [deleting, setDeleting] = useState<"confirm" | "reviews" | null>(null);
  const { error, run } = useAction();
  const ready = config && config !== "failed";

  if (!user) {
    return (
      <div className="card">
        <h2 className="h2">{s.account}</h2>
        <div className="muted small mt4">{s.accountHint}</div>
        <LegacyNotice />
        <button className="btn mt12" onClick={() => setSigning(true)}>{s.signIn}</button>
        {signing && <SignInSheet onClose={() => setSigning(false)} />}
      </div>
    );
  }

  const leave = (options?: { everywhere?: boolean; deleteAccount?: boolean; keepReviews?: boolean }) =>
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

      {restrictions && restrictions.length > 0 && (
        <div className="mt16">
          <div className="semibold small">{s.restrictionsTitle}</div>
          {restrictions.map((r) => (
            <div key={r.scope} className="red small mt4">{s.restrictionText(r.scope, r.until, r.reason)}</div>
          ))}
        </div>
      )}

      <SyncBlock />
      <RepBlock />

      <div className="row mt12" style={{ marginLeft: -8, flexWrap: "wrap" }}>
        <button className="tbtn" onClick={() => setSheet("reviews")}>{s.myReviews}</button>
        <button className="tbtn" onClick={() => setSheet("devices")}>{s.devices}</button>
      </div>

      <div className="row mt4" style={{ marginLeft: -8, flexWrap: "wrap" }}>
        {user.login ? (
          <button className="tbtn" onClick={() => setEditing("password")}>{s.changePassword}</button>
        ) : (
          <button className="tbtn" onClick={() => setEditing("credentials")}>{s.setPassword}</button>
        )}
        <button className="tbtn" onClick={() => leave()}>{s.signOut}</button>
        <button className="tbtn muted" onClick={() => leave({ everywhere: true }).then(() => toast(s.signOut))}>{s.signOutEverywhere}</button>
        <button className="tbtn danger" onClick={() => setDeleting("confirm")}>{s.deleteAccount}</button>
      </div>

      {editing && <EditSheet kind={editing} onClose={() => setEditing(null)} />}
      {sheet === "reviews" && <MyReviewsSheet onClose={() => setSheet(null)} />}
      {sheet === "devices" && <DevicesSheet onClose={() => setSheet(null)} />}
      {deleting === "confirm" && (
        <Alert title={s.deleteAccount} text={s.deleteAccountText} onClose={() => setDeleting(null)}>
          <button className="tbtn muted" onClick={() => setDeleting(null)}>{s.cancel}</button>
          <button className="tbtn danger" onClick={() => setDeleting("reviews")}>{s.delete}</button>
        </Alert>
      )}
      {deleting === "reviews" && (
        <Sheet onClose={() => setDeleting(null)}>
          <h2 className="h2" style={{ fontSize: 20 }}>{s.deleteAccount}</h2>
          <div className="muted small mt4">{s.deleteReviewsQuestion}</div>
          <div className="stack tight mt12">
            <button
              className="choice-row"
              onClick={() => {
                setDeleting(null);
                leave({ deleteAccount: true, keepReviews: true });
              }}
            >
              <span className="semibold small">{s.keepReviewsAnon}</span>
            </button>
            <button
              className="choice-row"
              onClick={() => {
                setDeleting(null);
                leave({ deleteAccount: true, keepReviews: false });
              }}
            >
              <span className="semibold small red">{s.deleteWithReviews}</span>
            </button>
            <button className="tbtn muted" onClick={() => setDeleting(null)}>{s.cancel}</button>
          </div>
        </Sheet>
      )}
    </div>
  );
}
