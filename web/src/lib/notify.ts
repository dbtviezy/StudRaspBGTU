/** One field that decides which notification kinds are allowed. Easy to extend later. */
export type NotifyProfileId = "normal" | "quiet" | "session" | "off";

export const NOTIFY_PROFILES: NotifyProfileId[] = ["normal", "quiet", "session", "off"];

export function cleanNotifyProfile(value: unknown): NotifyProfileId {
  return NOTIFY_PROFILES.includes(value as NotifyProfileId) ? (value as NotifyProfileId) : "normal";
}

export function allowsLessons(profile: NotifyProfileId) {
  return profile === "normal";
}

export function allowsDigest(profile: NotifyProfileId) {
  return profile === "normal";
}

export function allowsDeadlines(profile: NotifyProfileId) {
  return profile !== "off";
}

export function allowsChats(profile: NotifyProfileId) {
  return profile === "normal" || profile === "quiet";
}

export function allowsOngoing(profile: NotifyProfileId) {
  return profile === "normal";
}

/**
 * Rough campus building from an audience string (same rules as Android Buildings.kt).
 * А213 / Б205 → letter; 57Б → letter; plain numbers → main (#); named halls stay as themselves.
 */
export function buildingKey(room: string): string | null {
  const raw = room.trim();
  if (!raw) return null;
  const upper = raw.toUpperCase();
  if (upper.startsWith("АУД")) return "Д";
  if (
    upper.startsWith("ЧИТ") ||
    upper.startsWith("СПОР") ||
    upper.startsWith("ЛАБ") ||
    upper.startsWith("КРЕМ") ||
    upper.startsWith("БЭМ") ||
    upper.startsWith("1-Т")
  ) {
    return upper.slice(0, 16);
  }
  const first = upper[0];
  if ((first >= "А" && first <= "Я") || (first >= "A" && first <= "Z")) return first;
  const m = upper.match(/(\d+)\s*([А-ЯA-Z])\b/);
  if (m) return m[2];
  if (/\d/.test(upper)) return "#";
  return upper.slice(0, 20);
}

export function differentBuilding(previousRoom: string | null | undefined, nextRoom: string | null | undefined): boolean {
  if (!previousRoom || !nextRoom) return false;
  const a = buildingKey(previousRoom);
  const b = buildingKey(nextRoom);
  return !!a && !!b && a !== b;
}
