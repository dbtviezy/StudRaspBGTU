import { api, ApiFailure } from "./account";
import { getState, setState } from "./store";

export function pushSupported(): boolean {
  return typeof window !== "undefined" && "serviceWorker" in navigator && "PushManager" in window && "Notification" in window;
}

function keyBytes(base64: string): Uint8Array<ArrayBuffer> {
  const padded = (base64 + "=".repeat((4 - (base64.length % 4)) % 4)).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(padded);
  const bytes = new Uint8Array(new ArrayBuffer(raw.length));
  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
  return bytes;
}

async function registration(): Promise<ServiceWorkerRegistration> {
  return (await navigator.serviceWorker.getRegistration("/")) ?? navigator.serviceWorker.register("/sw.js", { scope: "/", updateViaCache: "none" });
}

async function current(): Promise<PushSubscription | null> {
  if (!pushSupported()) return null;
  const reg = await navigator.serviceWorker.getRegistration("/");
  return (await reg?.pushManager.getSubscription()) ?? null;
}

async function upload(subscription: PushSubscription) {
  const { hideProfanity, lang } = getState();
  await api("/api/push", {
    method: "POST",
    body: JSON.stringify({ ...subscription.toJSON(), hideProfanity, lang: lang ?? "ru" }),
  });
}

/** Throws ApiFailure("unsupported" | "denied" | "push_not_configured" | ...). */
export async function enablePush(): Promise<void> {
  if (!pushSupported()) throw new ApiFailure("unsupported");
  const permission = await Notification.requestPermission();
  if (permission !== "granted") throw new ApiFailure("denied");
  const { publicKey } = await api<{ publicKey: string | null }>("/api/push", {}, false);
  if (!publicKey) throw new ApiFailure("push_not_configured");
  const reg = await registration();
  await navigator.serviceWorker.ready;
  const subscription =
    (await reg.pushManager.getSubscription()) ??
    (await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: keyBytes(publicKey) }));
  await upload(subscription);
  setState({ chatPush: true });
}

export async function disablePush(): Promise<void> {
  setState({ chatPush: false });
  const subscription = await current().catch(() => null);
  if (!subscription) return;
  await api("/api/push", { method: "DELETE", body: JSON.stringify({ endpoint: subscription.endpoint }) }, false).catch(() => undefined);
  await subscription.unsubscribe().catch(() => false);
}

/** Re-sends settings (profanity filter, language) for an existing subscription. */
export async function syncPush(): Promise<void> {
  if (!getState().chatPush) return;
  const subscription = await current().catch(() => null);
  if (subscription) await upload(subscription).catch(() => undefined);
}
