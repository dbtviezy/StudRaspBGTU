"use client";

import { type ReactNode, useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Icon } from "./Icons";

let locks = 0;

function useBodyLock() {
  useEffect(() => {
    locks++;
    document.body.classList.add("locked");
    return () => {
      locks--;
      if (locks <= 0) document.body.classList.remove("locked");
    };
  }, []);
}

function useEscape(onClose?: () => void) {
  const ref = useRef(onClose);
  useEffect(() => {
    ref.current = onClose;
  });
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") ref.current?.();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
}

function Portal({ children }: { children: ReactNode }) {
  return createPortal(children, document.body);
}

export function Dialog({ onClose, children }: { onClose: () => void; children: ReactNode }) {
  useBodyLock();
  useEscape(onClose);
  return (
    <Portal>
      <div className="scrim center" onClick={onClose}>
        <div className="dialog" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
          {children}
        </div>
      </div>
    </Portal>
  );
}

export function Sheet({ onClose, children, foot }: { onClose: () => void; children: ReactNode; foot?: ReactNode }) {
  useBodyLock();
  useEscape(onClose);
  return (
    <Portal>
      <div className="scrim bottom" onClick={onClose}>
        <div className="sheet" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
          <div className="sheet-body">
            <div className="handle" />
            {children}
          </div>
          {foot && <div className="sheet-foot">{foot}</div>}
        </div>
      </div>
    </Portal>
  );
}

export function Overlay({ onClose, children }: { onClose: () => void; children: ReactNode }) {
  useBodyLock();
  useEscape(onClose);
  return (
    <Portal>
      <div className="overlay" role="dialog" aria-modal="true">
        <div className="inner">{children}</div>
      </div>
    </Portal>
  );
}

export function Centered({ onClose, children }: { onClose: () => void; children: ReactNode }) {
  useBodyLock();
  useEscape(onClose);
  return (
    <Portal>
      <div className="scrim center" onClick={onClose}>
        <div className="quick" onClick={(e) => e.stopPropagation()}>
          {children}
        </div>
      </div>
    </Portal>
  );
}

export function Alert({
  title,
  text,
  onClose,
  children,
}: {
  title: string;
  text?: ReactNode;
  onClose: () => void;
  children: ReactNode;
}) {
  return (
    <Dialog onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 22 }}>
        {title}
      </h2>
      {text != null && <div className="muted mt8" style={{ fontSize: 14 }}>{text}</div>}
      <div className="dialog-actions">{children}</div>
    </Dialog>
  );
}

export function OverlayHeader({ title, onClose, closeLabel }: { title: string; onClose: () => void; closeLabel: string }) {
  return (
    <div className="row">
      <h1 className="h1 grow">{title}</h1>
      <button className="icon-btn" onClick={onClose} aria-label={closeLabel}>
        <Icon name="close" />
      </button>
    </div>
  );
}

export function Mark({ size = 44 }: { size?: number }) {
  return (
    <span className="mark" style={{ width: size, height: size }}>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src="/icons/mark.png" alt="" />
    </span>
  );
}

export function Stars({ value, size = 14 }: { value: number; size?: number }) {
  return (
    <span className="stars" aria-label={value.toFixed(1)}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Icon key={n} name="star" size={size} className={value >= n - 0.25 ? undefined : "off"} />
      ))}
    </span>
  );
}

export function StarPicker({ value, onChange, size = 30, label }: { value: number; onChange: (v: number) => void; size?: number; label: string }) {
  return (
    <div className="star-pick" role="radiogroup" aria-label={label}>
      {[1, 2, 3, 4, 5].map((n) => (
        <button key={n} type="button" className={value >= n ? "on" : undefined} onClick={() => onChange(n)} role="radio" aria-checked={value === n} aria-label={String(n)}>
          <Icon name="star" size={size} />
        </button>
      ))}
    </div>
  );
}

export function Switch({ on, onToggle, label }: { on: boolean; onToggle: (e: React.MouseEvent) => void; label: string }) {
  return <button type="button" role="switch" aria-checked={on} aria-label={label} className={`switch${on ? " on" : ""}`} onClick={onToggle} />;
}

export function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail">
      <div className="label">{label}</div>
      <div className="value">{value}</div>
    </div>
  );
}

export function Chip({ on, onClick, children }: { on: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button type="button" className={`chip${on ? " on" : ""}`} onClick={onClick}>
      {children}
    </button>
  );
}

export function LanguageMark({ lang, onLanguage }: { lang: string | null; onLanguage: (code: "ru" | "en") => void }) {
  const current = lang === "en" ? "en" : "ru";
  return (
    <button className="lang-mark" onClick={() => onLanguage(current === "en" ? "ru" : "en")}>
      {current === "en" ? "EN" : "RU"}
    </button>
  );
}

export function useLongPress(onLong: () => void, onClick: () => void) {
  const timer = useRef<number | null>(null);
  const start = useRef<{ x: number; y: number } | null>(null);
  const fired = useRef(false);
  const clear = () => {
    if (timer.current != null) window.clearTimeout(timer.current);
    timer.current = null;
  };
  useEffect(() => clear, []);
  return {
    onPointerDown(event: React.PointerEvent) {
      fired.current = false;
      start.current = { x: event.clientX, y: event.clientY };
      clear();
      timer.current = window.setTimeout(() => {
        fired.current = true;
        navigator.vibrate?.(10);
        onLong();
      }, 480);
    },
    onPointerMove(event: React.PointerEvent) {
      const origin = start.current;
      if (origin && Math.hypot(event.clientX - origin.x, event.clientY - origin.y) > 10) clear();
    },
    onPointerUp: clear,
    onPointerCancel: clear,
    onPointerLeave: clear,
    onContextMenu(event: React.MouseEvent) {
      event.preventDefault();
      if (!fired.current) {
        clear();
        fired.current = true;
        onLong();
      }
    },
    onClick() {
      if (fired.current) {
        fired.current = false;
        return;
      }
      onClick();
    },
  };
}

export function useToast(): [string | null, (text: string) => void] {
  const [text, setText] = useState<string | null>(null);
  const timer = useRef<number | null>(null);
  const show = (value: string) => {
    setText(value);
    if (timer.current != null) window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setText(null), 2600);
  };
  return [text, show];
}

export function Appear({ index, children }: { index: number; children: ReactNode }) {
  return (
    <div className="appear" style={{ animationDelay: `${Math.min(index, 8) * 36}ms` }}>
      {children}
    </div>
  );
}
