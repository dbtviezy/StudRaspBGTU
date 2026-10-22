/**
 * Masks swear words as "б***" for people who turned the filter on. Runs only in the browser.
 * Keep the patterns identical to `Profanity.kt` in the Android app.
 */
const PATTERNS = [
  "^(на|по|за|вы|от|до|ни|о|у)?ху[йеяию]",
  "пизд",
  "^(за|вы|до|на|по|от|отъ|раз|разъ|рас|съ|въ|объ|подъ|об|у|при|пере|недо|долбо)?еб(а|у|л|н|и|ш|т|е|ы|$)",
  "^бля(д|т|$)",
  "^сук(а|и|ой|у|е|ам)$",
  "^сучк",
  "^муда[кч]",
  "^мудил",
  "^п[ие]д[оа]р",
  "^г[ао]ндон",
  "залуп",
  "^шлю[хш]",
  "fuck",
  "^(shit|bitch|cunt|asshole|motherf|dickhead)",
].map((p) => new RegExp(p));

const LOOKALIKES: Record<string, string> = {
  a: "а", e: "е", o: "о", p: "р", c: "с", x: "х", y: "у", k: "к", b: "в", m: "м", h: "н", t: "т",
};

function normalize(raw: string): string {
  const lower = raw.toLowerCase().replace(/ё/g, "е");
  return /[а-я]/.test(lower) ? [...lower].map((ch) => LOOKALIKES[ch] ?? ch).join("") : lower;
}

export function isBad(raw: string): boolean {
  const text = normalize(raw);
  return PATTERNS.some((p) => p.test(text));
}

export function mask(text: string): string {
  return text.replace(/\p{L}+/gu, (value) => (value.length > 1 && isBad(value) ? value[0] + "*".repeat([...value].length - 1) : value));
}

export const maskIf = (enabled: boolean, text: string) => (enabled ? mask(text) : text);
