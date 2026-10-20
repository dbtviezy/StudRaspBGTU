"use client";

import { useRef, useState } from "react";
import { APP_VERSION, type Copy } from "@/lib/i18n";
import { originOf, switchTheme } from "@/lib/theme";
import { Icon, type IconName } from "../Icons";

interface Props {
  s: Copy;
  lang: "ru" | "en" | null;
  dark: boolean;
  onLanguage: (code: "ru" | "en") => void;
  onStart: () => void;
}

function Art({ icon }: { icon: IconName }) {
  return (
    <div className="art" aria-hidden>
      <div className="c1" />
      <div className="c2" />
      <div className="box">
        <Icon name={icon} size={64} />
      </div>
    </div>
  );
}

export default function Onboarding({ s, lang, dark, onLanguage, onStart }: Props) {
  const pager = useRef<HTMLDivElement>(null);
  const [page, setPage] = useState(0);
  const current = lang ?? "ru";
  const pages: { icon: IconName; title: string; text: string }[] = [
    { icon: "viewDay", title: s.welcomeTitle, text: s.welcomeText },
    { icon: "iosShare", title: s.offlinePageTitle, text: s.offlinePageText },
    { icon: "search", title: s.searchPageTitle, text: s.searchPageText },
  ];
  const last = page === pages.length - 1;

  const next = () => {
    if (last) return onStart();
    const el = pager.current;
    el?.scrollTo({ left: el.clientWidth * (page + 1), behavior: "smooth" });
  };

  return (
    <div className="fullscreen">
      <div
        ref={pager}
        className="pager"
        onScroll={(e) => {
          const el = e.currentTarget;
          setPage(Math.round(el.scrollLeft / Math.max(1, el.clientWidth)));
        }}
      >
        {pages.map((item, index) => (
          <section key={item.icon} className="page" aria-hidden={index !== page}>
            <Art icon={item.icon} />
            <h1 className="h1" style={{ fontSize: 26, marginTop: 28 }}>{item.title}</h1>
            <p className="muted" style={{ fontSize: 15, lineHeight: 1.45, maxWidth: 340 }}>{item.text}</p>
            {index === 0 && (
              <div className="stack" style={{ width: "100%", maxWidth: 340, marginTop: 8, gap: 10 }}>
                <div className="row">
                  <button className={`choice${current === "ru" ? " on" : ""}`} onClick={() => onLanguage("ru")}>{s.langRu}</button>
                  <button className={`choice${current === "en" ? " on" : ""}`} onClick={() => onLanguage("en")}>{s.langEn}</button>
                </div>
                <div className="row">
                  <button className={`choice${!dark ? " on" : ""}`} onClick={(e) => dark && switchTheme(false, originOf(e))}>{s.themeLight}</button>
                  <button className={`choice${dark ? " on" : ""}`} onClick={(e) => !dark && switchTheme(true, originOf(e))}>{s.themeDark}</button>
                </div>
              </div>
            )}
          </section>
        ))}
      </div>
      <div style={{ padding: "0 24px calc(var(--safe-bottom) + 16px)" }}>
        <div className="dots">
          {pages.map((item, index) => (
            <span key={item.icon} className={index === page ? "on" : undefined} />
          ))}
        </div>
        <button className="btn" onClick={next}>{last ? s.begin : s.nextPage}</button>
        <div className="muted xsmall mt8" style={{ textAlign: "center" }}>{`${s.appName} ${APP_VERSION}`}</div>
      </div>
    </div>
  );
}
