"use client";

import { APP_VERSION, type Copy } from "@/lib/i18n";
import { Dialog } from "../ui";

export default function WhatsNew({ s, onClose }: { s: Copy; onClose: () => void }) {
  return (
    <Dialog onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 22 }}>{s.whatsNewTitle(APP_VERSION)}</h2>
      <div style={{ maxHeight: "60vh", overflowY: "auto" }}>
        {s.whatsNewItems.map(([title, text]) => (
          <div key={title} className="mt12">
            <div className="semibold">{title}</div>
            <div className="muted small mt2">{text}</div>
          </div>
        ))}
      </div>
      <div className="dialog-actions">
        <button className="tbtn" onClick={onClose}>{s.whatsNewOk}</button>
      </div>
    </Dialog>
  );
}
