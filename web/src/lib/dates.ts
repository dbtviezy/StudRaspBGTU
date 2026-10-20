export type ISODate = string;

const pad = (n: number) => String(n).padStart(2, "0");

export function isoOf(date: Date): ISODate {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function parts(iso: ISODate): { y: number; m: number; d: number } {
  const [y, m, d] = iso.split("-").map(Number);
  return { y, m, d };
}

function utc(iso: ISODate): Date {
  const { y, m, d } = parts(iso);
  return new Date(Date.UTC(y, m - 1, d));
}

function fromUtc(date: Date): ISODate {
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

export function isValidIso(value: unknown): value is ISODate {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  return fromUtc(utc(value)) === value;
}

export function addDays(iso: ISODate, days: number): ISODate {
  const date = utc(iso);
  date.setUTCDate(date.getUTCDate() + days);
  return fromUtc(date);
}

export function daysInMonth(y: number, m: number): number {
  return new Date(Date.UTC(y, m, 0)).getUTCDate();
}

export function addMonths(iso: ISODate, months: number): ISODate {
  const { y, m, d } = parts(iso);
  const index = y * 12 + (m - 1) + months;
  const ny = Math.floor(index / 12);
  const nm = (index % 12) + 1;
  return `${ny}-${pad(nm)}-${pad(Math.min(d, daysInMonth(ny, nm)))}`;
}

/** ISO weekday: Monday = 1 … Sunday = 7. */
export function dow(iso: ISODate): number {
  const w = utc(iso).getUTCDay();
  return w === 0 ? 7 : w;
}

export function mondayOf(iso: ISODate): ISODate {
  return addDays(iso, 1 - dow(iso));
}

export function daysBetween(from: ISODate, to: ISODate): number {
  return Math.round((utc(to).getTime() - utc(from).getTime()) / 86400000);
}

export function toMinutes(time: string): number | null {
  const match = /^(\d{1,2}):(\d{2})$/.exec(time.trim());
  if (!match) return null;
  const h = Number(match[1]);
  const m = Number(match[2]);
  if (h > 23 || m > 59) return null;
  return h * 60 + m;
}

/** Minutes since midnight including the seconds fraction, mirroring LocalTime precision. */
export function minutesOf(date: Date): number {
  return date.getHours() * 60 + date.getMinutes() + date.getSeconds() / 60;
}
