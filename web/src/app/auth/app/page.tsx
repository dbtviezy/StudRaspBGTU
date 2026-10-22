import AppAuth from "./AppAuth";

export const dynamic = "force-dynamic";

const SCHEMES = new Set(["studraspbgtu", "studraspbgtu-admin"]);

/** Browser step of Telegram/Google sign-in for the Android app; hands a one-time code back via a deep link. */
export default async function Page({ searchParams }: { searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const params = await searchParams;
  const provider = params.provider === "google" ? "google" : "telegram";
  const scheme = typeof params.return === "string" && SCHEMES.has(params.return) ? params.return : "studraspbgtu";
  const lang = params.lang === "en" ? "en" : "ru";
  return <AppAuth provider={provider} scheme={scheme} lang={lang} />;
}
