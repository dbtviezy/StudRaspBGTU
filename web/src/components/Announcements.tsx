"use client";

import { useEffect, useState } from "react";
import type { Copy } from "@/lib/i18n";
import { PREFIX } from "@/lib/store";
import { Icon } from "./Icons";

interface Announcement {
  id: number;
  text: string;
}

const DISMISSED_KEY = `${PREFIX}dismissedAnnouncements`;

function dismissed(): number[] {
  try {
    return JSON.parse(localStorage.getItem(DISMISSED_KEY) ?? "[]") as number[];
  } catch {
    return [];
  }
}

/** Banner with admin announcements; each one can be closed and stays closed on this device. */
export function Announcements({ s }: { s: Copy }) {
  const [items, setItems] = useState<Announcement[]>([]);

  useEffect(() => {
    let alive = true;
    fetch("/api/announcements")
      .then((response) => (response.ok ? response.json() : { items: [] }))
      .then((body: { items: Announcement[] }) => {
        const hidden = new Set(dismissed());
        if (alive) setItems(body.items.filter((item) => !hidden.has(item.id)));
      })
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, []);

  const close = (id: number) => {
    try {
      localStorage.setItem(DISMISSED_KEY, JSON.stringify([...dismissed(), id].slice(-50)));
    } catch {}
    setItems((current) => current.filter((item) => item.id !== id));
  };

  return items.map((item) => (
    <div key={item.id} className="info-box announce" role="status">
      <span className="blue">
        <Icon name="campaign" size={20} />
      </span>
      <div className="grow">
        <div className="semibold small">{s.announcement}</div>
        <div className="small mt2 pre" style={{ overflowWrap: "anywhere" }}>{item.text}</div>
      </div>
      <button className="icon-btn" style={{ margin: "-8px -8px 0 0" }} aria-label={s.close} onClick={() => close(item.id)}>
        <Icon name="close" size={18} />
      </button>
    </div>
  ));
}
