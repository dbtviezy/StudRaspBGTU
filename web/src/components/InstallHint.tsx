"use client";

import { useState } from "react";
import type { Copy } from "@/lib/i18n";
import { PREFIX } from "@/lib/store";
import { Icon } from "./Icons";

const KEY = `${PREFIX}installHintShown`;

function shouldShow(): boolean {
  const ua = navigator.userAgent;
  const ios = /iPad|iPhone|iPod/.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  const safari = /Safari/.test(ua) && !/CriOS|FxiOS|EdgiOS|OPiOS|YaBrowser/.test(ua);
  const standalone = (navigator as Navigator & { standalone?: boolean }).standalone === true || window.matchMedia("(display-mode: standalone)").matches;
  return ios && safari && !standalone && localStorage.getItem(KEY) == null;
}

export default function InstallHint({ s }: { s: Copy }) {
  const [visible, setVisible] = useState(() => {
    const show = shouldShow();
    if (show) localStorage.setItem(KEY, "1");
    return show;
  });
  if (!visible) return null;
  return (
    <div className="card install-hint" role="note">
      <span className="blue" style={{ marginTop: 2 }}>
        <Icon name="iosShare" size={26} />
      </span>
      <div className="grow">
        <div className="semibold">{s.installTitle}</div>
        <div className="muted small mt2">{s.installText}</div>
      </div>
      <button className="icon-btn" style={{ width: 32, height: 32, margin: -4 }} onClick={() => setVisible(false)} aria-label={s.close}>
        <Icon name="close" size={20} />
      </button>
    </div>
  );
}
