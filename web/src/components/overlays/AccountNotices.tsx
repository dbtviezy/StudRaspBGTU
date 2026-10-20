"use client";

import { useEffect, useRef, useState } from "react";
import { useAccount } from "@/lib/account";
import type { Copy } from "@/lib/i18n";
import { claimableReviews, claimDeviceReviews, refreshSummaries } from "@/lib/ratings";
import { onDataChange, PREFIX, setState } from "@/lib/store";
import { answerSyncQuestion, forgetSync, scheduleSync, syncNow, useSyncStatus } from "@/lib/sync";
import { Alert, Dialog } from "../ui";

const claimedKey = (userId: string) => `${PREFIX}claimed.${userId}`;

/** Claims this device's old reviews and keeps data in sync while signed in. */
export function useAccountSync() {
  const { user } = useAccount();
  const userId = user?.id ?? null;
  const previous = useRef<string | null>(null);

  useEffect(() => {
    const was = previous.current;
    previous.current = userId;
    if (!userId) {
      if (was) forgetSync();
      return;
    }
    let alive = true;
    (async () => {
      try {
        if (!localStorage.getItem(claimedKey(userId))) {
          const claimed = await claimDeviceReviews();
          localStorage.setItem(claimedKey(userId), "1");
          if (claimed > 0) void refreshSummaries();
        }
      } catch {}
      if (alive) await syncNow();
    })();
    const offData = onDataChange(scheduleSync);
    const onVisible = () => {
      if (document.visibilityState === "visible") void syncNow();
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      alive = false;
      offData();
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [userId]);
}

/** One-time "sign in" suggestion, with a warning when this device has old reviews without an account. */
export function LoginOffer({ s, onSignIn }: { s: Copy; onSignIn: () => void }) {
  const [warning, setWarning] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    claimableReviews()
      .then((result) => alive && result.count > 0 && setWarning(s.legacyWarn(result.until)))
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, [s]);
  const close = () => setState({ loginOffered: true });
  return (
    <Alert
      title={s.loginOfferTitle}
      text={
        <>
          {s.loginOfferText}
          {warning && <div className="red mt8">{warning}</div>}
        </>
      }
      onClose={close}
    >
      <button className="tbtn muted" onClick={close}>{s.later}</button>
      <button
        className="tbtn"
        onClick={() => {
          close();
          onSignIn();
        }}
      >
        {s.signIn}
      </button>
    </Alert>
  );
}

/** Asked on the first sync when both this device and the account already have different data. */
export function SyncQuestion({ s }: { s: Copy }) {
  const { asking } = useSyncStatus();
  if (!asking) return null;
  const options: [Parameters<typeof answerSyncQuestion>[0], string, string][] = [
    ["merge", s.syncMerge, s.syncMergeHint],
    ["account", s.syncTakeAccount, s.syncTakeAccountHint],
    ["device", s.syncKeepDevice, s.syncKeepDeviceHint],
  ];
  return (
    <Dialog onClose={() => undefined}>
      <h2 className="h2" style={{ fontSize: 22 }}>{s.syncAskTitle}</h2>
      <div className="muted mt8" style={{ fontSize: 14 }}>{s.syncAskText}</div>
      <div className="stack tight mt12">
        {options.map(([choice, label, hint]) => (
          <button key={choice} className="choice-row" onClick={() => answerSyncQuestion(choice)}>
            <span className="semibold">{label}</span>
            <span className="muted xsmall">{hint}</span>
          </button>
        ))}
      </div>
    </Dialog>
  );
}
