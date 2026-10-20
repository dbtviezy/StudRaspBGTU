import webpush from "web-push";

export interface PushKeys {
  publicKey: string;
  privateKey: string;
  subject: string;
}

export function pushKeys(): PushKeys | null {
  const publicKey = process.env.VAPID_PUBLIC_KEY?.trim();
  const privateKey = process.env.VAPID_PRIVATE_KEY?.trim();
  if (!publicKey || !privateKey) return null;
  const subject = process.env.VAPID_SUBJECT?.trim() || "https://studraspbgtu-web.vercel.app";
  return { publicKey, privateKey, subject };
}

export interface PushTarget {
  endpoint: string;
  p256dh: string;
  auth: string;
}

/** Sends one notification; returns false when the subscription is gone and should be deleted. */
export async function sendPush(keys: PushKeys, target: PushTarget, payload: unknown): Promise<boolean> {
  try {
    await webpush.sendNotification(
      { endpoint: target.endpoint, keys: { p256dh: target.p256dh, auth: target.auth } },
      JSON.stringify(payload),
      { TTL: 6 * 3600, vapidDetails: keys, timeout: 8000 },
    );
    return true;
  } catch (error) {
    const status = (error as { statusCode?: number }).statusCode;
    if (status === 404 || status === 410) return false;
    console.error("push failed", status ?? error);
    return true;
  }
}
