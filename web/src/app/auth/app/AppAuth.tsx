"use client";

import { useEffect, useState } from "react";
import { GoogleLogin, TelegramLogin } from "@/components/auth/Providers";
import { copyOf } from "@/lib/i18n";

interface Config {
  telegramBot: string | null;
  googleClientId: string | null;
}

async function post<T>(path: string, body: unknown, token?: string): Promise<T> {
  const response = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(body),
  });
  const json = (await response.json().catch(() => null)) as (T & { error?: string }) | null;
  if (!response.ok || !json) throw new Error(json?.error ?? "server_error");
  return json;
}

export default function AppAuth({ provider, scheme, lang }: { provider: "telegram" | "google"; scheme: string; lang: "ru" | "en" }) {
  const s = copyOf(lang);
  const [config, setConfig] = useState<Config | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [link, setLink] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/auth/config")
      .then((response) => response.json())
      .then(setConfig)
      .catch(() => setError(s.serverOff));
  }, [s.serverOff]);

  const finish = async (path: string, body: unknown) => {
    setError(null);
    try {
      const { token } = await post<{ token: string }>(path, body);
      const { code } = await post<{ code: string }>("/api/auth/code", {}, token);
      await fetch("/api/auth/me", { method: "DELETE", headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" }, body: "{}" });
      const target = `${scheme}://auth?code=${encodeURIComponent(code)}`;
      setLink(target);
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination -- custom app scheme, not a Next.js page
      window.location.assign(target);
    } catch (e) {
      setError(s.errorText(e instanceof Error ? e.message : ""));
    }
  };

  const ready = config != null;
  const off = ready && (provider === "telegram" ? !config.telegramBot : !config.googleClientId);

  return (
    <main className="fullscreen" style={{ justifyContent: "center", padding: 28, maxWidth: 480, margin: "0 auto" }}>
      <h1 className="h1" style={{ fontSize: 24 }}>{provider === "telegram" ? s.withTelegram : s.withGoogle}</h1>
      <p className="muted small">{s.appName}</p>
      <div className="mt16">
        {ready && provider === "telegram" && config.telegramBot && (
          <TelegramLogin bot={config.telegramBot} onAuth={(user) => finish("/api/auth/telegram", user)} />
        )}
        {ready && provider === "google" && config.googleClientId && (
          <GoogleLogin clientId={config.googleClientId} dark={false} onCredential={(credential) => finish("/api/auth/google", { credential })} />
        )}
        {off && <div className="muted small">{s.providerOff}</div>}
      </div>
      {error && <div className="red small mt12">{error}</div>}
      {link && (
        <a className="btn mt16" href={link} style={{ textAlign: "center" }}>
          {s.open}
        </a>
      )}
    </main>
  );
}
