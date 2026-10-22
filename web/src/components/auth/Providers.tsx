"use client";

import { useEffect, useRef } from "react";

declare global {
  interface Window {
    onTelegramAuth?: (user: Record<string, unknown>) => void;
    google?: {
      accounts: {
        id: {
          initialize(options: { client_id: string; callback: (response: { credential: string }) => void }): void;
          renderButton(element: HTMLElement, options: Record<string, unknown>): void;
        };
      };
    };
  }
}

/** Official Telegram Login Widget; works only on the domain set for the bot via /setdomain. */
export function TelegramLogin({ bot, onAuth }: { bot: string; onAuth: (user: Record<string, unknown>) => void }) {
  const box = useRef<HTMLDivElement>(null);
  const callback = useRef(onAuth);
  useEffect(() => {
    callback.current = onAuth;
  });
  useEffect(() => {
    const element = box.current;
    if (!element) return;
    window.onTelegramAuth = (user) => callback.current(user);
    const script = document.createElement("script");
    script.src = "https://telegram.org/js/telegram-widget.js?22";
    script.async = true;
    script.setAttribute("data-telegram-login", bot);
    script.setAttribute("data-size", "large");
    script.setAttribute("data-radius", "14");
    script.setAttribute("data-request-access", "write");
    script.setAttribute("data-onauth", "onTelegramAuth(user)");
    element.replaceChildren(script);
    return () => {
      element.replaceChildren();
      delete window.onTelegramAuth;
    };
  }, [bot]);
  return <div ref={box} style={{ display: "flex", justifyContent: "center", minHeight: 44 }} />;
}

let gsi: Promise<void> | null = null;

function loadGsi(): Promise<void> {
  gsi ??= new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      gsi = null;
      reject(new Error("gsi"));
    };
    document.head.appendChild(script);
  });
  return gsi;
}

/** Google Identity Services button; returns the ID token to `onCredential`. */
export function GoogleLogin({ clientId, dark, onCredential }: { clientId: string; dark: boolean; onCredential: (credential: string) => void }) {
  const box = useRef<HTMLDivElement>(null);
  const callback = useRef(onCredential);
  useEffect(() => {
    callback.current = onCredential;
  });
  useEffect(() => {
    let alive = true;
    loadGsi()
      .then(() => {
        if (!alive || !box.current || !window.google) return;
        window.google.accounts.id.initialize({ client_id: clientId, callback: (response) => callback.current(response.credential) });
        window.google.accounts.id.renderButton(box.current, {
          theme: dark ? "filled_black" : "outline",
          size: "large",
          shape: "pill",
          width: Math.min(320, box.current.clientWidth || 320),
        });
      })
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, [clientId, dark]);
  return <div ref={box} style={{ display: "flex", justifyContent: "center", minHeight: 44 }} />;
}
