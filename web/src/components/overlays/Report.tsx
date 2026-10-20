"use client";

import { useState } from "react";
import { useShell } from "../context";
import { Dialog } from "../ui";

/** Asks for an optional reason and sends a complaint to the moderators. */
export default function ReportDialog({ onSend, onClose }: { onSend: (reason: string) => Promise<void>; onClose: () => void }) {
  const { s, toast } = useShell();
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const send = async () => {
    setBusy(true);
    setError(null);
    try {
      await onSend(reason.trim());
      toast(s.reportSent);
      onClose();
    } catch (e) {
      setError(s.errorText(e instanceof Error ? e.message : ""));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 22 }}>{s.reportTitle}</h2>
      <div className="muted small mt8">{s.reportText}</div>
      <textarea className="input mt12" rows={3} maxLength={200} value={reason} onChange={(e) => setReason(e.target.value)} />
      {error && <div className="red small mt8">{error}</div>}
      <div className="dialog-actions">
        <button className="tbtn muted" onClick={onClose}>{s.cancel}</button>
        <button className="tbtn danger" disabled={busy} onClick={send}>{s.report}</button>
      </div>
    </Dialog>
  );
}
