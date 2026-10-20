"use client";

import { useRef, useState } from "react";
import { useAccount } from "@/lib/account";
import { download, exportBackup, importBackup } from "@/lib/backup";
import { APP_VERSION } from "@/lib/i18n";
import { subscriptionUrl } from "@/lib/ics";
import { clearFiles, setAvatar, useAvatar, useLibrary } from "@/lib/idb";
import { disablePush, enablePush, syncPush } from "@/lib/push";
import { weekInfo } from "@/lib/schedule";
import { actions, setState, sortedTodos } from "@/lib/store";
import { originOf, switchTheme } from "@/lib/theme";
import { useShell } from "../context";
import { Icon, TelegramIcon } from "../Icons";
import { AccountCard } from "../overlays/Account";
import CropPhoto from "../overlays/CropPhoto";
import WhatsNew from "../overlays/WhatsNew";
import { Alert, Detail, LanguageMark, Switch } from "../ui";

function SettingRow({ title, hint, on, onToggle }: { title: string; hint: string; on: boolean; onToggle: () => void }) {
  return (
    <div className="row mt12" style={{ gap: 12 }}>
      <div className="grow">
        <div className="semibold small">{title}</div>
        <div className="muted xsmall mt2">{hint}</div>
      </div>
      <Switch on={on} label={title} onToggle={onToggle} />
    </div>
  );
}

function Card({ title, hint, children }: { title: string; hint?: string; children?: React.ReactNode }) {
  return (
    <div className="card">
      <h2 className="h2">{title}</h2>
      {hint && <div className="muted small mt4">{hint}</div>}
      {children}
    </div>
  );
}

export default function ProfileScreen() {
  const { bundle, s, data, today, faculty, group, openTasks, openFiles, setLanguage, pickGroup, pickFriend, onReset, toast } = useShell();
  const avatar = useAvatar();
  const library = useLibrary();
  const photoInput = useRef<HTMLInputElement>(null);
  const backupInput = useRef<HTMLInputElement>(null);
  const [cropping, setCropping] = useState<File | null>(null);
  const [resetting, setResetting] = useState(false);
  const [withFiles, setWithFiles] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [whatsNew, setWhatsNew] = useState(false);
  const [pushBusy, setPushBusy] = useState(false);
  const { user } = useAccount();
  const info = weekInfo(today, bundle.semesterStart);
  const feed = subscriptionUrl(window.location.origin, group.id);
  const webcal = feed.replace(/^https?:/, "webcal:");

  const togglePush = async () => {
    if (pushBusy) return;
    if (data.chatPush) return disablePush();
    if (!user) return toast(s.chatNotifySignIn);
    setPushBusy(true);
    try {
      await enablePush();
    } catch (e) {
      const code = e instanceof Error ? e.message : "";
      setMessage(code === "unsupported" ? s.pushUnsupported : code === "denied" ? s.pushDenied : s.errorText(code));
    } finally {
      setPushBusy(false);
    }
  };
  const friend = data.friend ? bundle.find(data.friend) : null;
  const openTodos = sortedTodos(data.todos).filter((t) => !t.done);
  const initial = (data.name.trim() || s.studentFallback)[0]?.toUpperCase() ?? "?";

  const subjects = [...new Set(data.absences.map((a) => a.subject))].sort();
  const hiddenLessons = data.hidden
    .map((key) => group.lessons.find((l) => l.key === key))
    .filter((l): l is NonNullable<typeof l> => l != null);
  const subjectNotes = Object.entries(data.subjectNotes).filter(([, v]) => v.trim()).sort(([a], [b]) => a.localeCompare(b));
  const librarySize = library.reduce((sum, f) => sum + f.size, 0);

  const saveBackup = () => {
    download(`studrasp-backup-${today}.json`, exportBackup(library.map((f) => f.name)), "application/json");
    toast(s.backupDone);
  };

  const loadBackup = async (file: File | undefined) => {
    if (!file) return;
    try {
      const { group: restored } = importBackup(await file.text());
      if (restored && !bundle.find(restored)) setState({ group: group.id });
      toast(s.backupRestored);
    } catch {
      setMessage(s.backupFailed);
    }
  };

  const confirmReset = async () => {
    setResetting(false);
    try {
      if (withFiles) await clearFiles();
      await setAvatar(null);
    } catch {}
    setWithFiles(false);
    onReset();
  };

  return (
    <div className="stack">
      <div className="row">
        <h1 className="h1 grow">{s.profile}</h1>
        <LanguageMark lang={data.lang} onLanguage={setLanguage} />
        <span style={{ width: 8 }} />
        <Switch on={data.dark} label={s.theme} onToggle={(e) => switchTheme(!data.dark, originOf(e))} />
      </div>

      <div className="card" style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
        <button
          aria-label={s.changePhoto}
          onClick={() => photoInput.current?.click()}
          style={{
            width: 104,
            height: 104,
            borderRadius: "50%",
            overflow: "hidden",
            background: "var(--blue-soft)",
            color: "var(--blue)",
            fontSize: 40,
            fontWeight: 700,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          {avatar ? <img src={avatar} alt={s.photo} style={{ width: "100%", height: "100%", objectFit: "cover" }} /> : initial}
        </button>
        <div className="row" style={{ gap: 4 }}>
          <button className="tbtn" onClick={() => photoInput.current?.click()}>{s.changePhoto}</button>
          {avatar && (
            <button className="tbtn muted" onClick={() => setAvatar(null)}>{s.removePhoto}</button>
          )}
        </div>
        <input
          ref={photoInput}
          type="file"
          accept="image/*"
          hidden
          onChange={(e) => {
            const file = e.target.files?.[0];
            e.target.value = "";
            if (file) setCropping(file);
          }}
        />
        <label className="field" style={{ width: "100%" }}>
          <span>{s.name}</span>
          <input
            className="input"
            value={data.name}
            maxLength={60}
            placeholder={s.nameHint}
            autoComplete="given-name"
            onChange={(e) => setState({ name: e.target.value })}
          />
        </label>
      </div>

      <div className="card">
        <Detail label={s.group} value={group.id} />
        <Detail label={s.faculty} value={s.facultyName(faculty.name)} />
        <Detail label={s.level} value={s.levelName(group.level)} />
        {group.year != null && <Detail label={s.cohort} value={s.cohortValue(group.year)} />}
        <Detail label={s.period} value={s.periodLabel(bundle.periodLabel) || "—"} />
        <Detail label={s.form} value={s.fullTimeForm} />
        <button className="btn mt12" onClick={pickGroup}>{s.changeGroup}</button>
      </div>

      <AccountCard />

      <Card title={s.secondGroup} hint={s.secondGroupHint}>
        {friend && <div className="semibold mt8">{`${friend.group.id} · ${friend.faculty.short}`}</div>}
        <div className="row mt4" style={{ marginLeft: -8 }}>
          <button className="tbtn" onClick={pickFriend}>{s.pickSecond}</button>
          {friend && (
            <button className="tbtn muted" onClick={() => setState({ friend: null })}>{s.clearSecond}</button>
          )}
        </div>
      </Card>

      <button className="card" style={{ textAlign: "left" }} onClick={openTasks}>
        <div className="row">
          <div className="grow">
            <h2 className="h2">{s.tasks}</h2>
            <div className="muted small mt4">{openTodos.length === 0 ? s.tasksEmpty : s.openTasksLine(openTodos.length)}</div>
          </div>
          <span className="muted">
            <Icon name="chevronRight" />
          </span>
        </div>
      </button>

      <button className="card" style={{ textAlign: "left" }} onClick={openFiles}>
        <div className="row">
          <div className="grow">
            <h2 className="h2">{s.files}</h2>
            <div className="muted small mt4">{library.length === 0 ? s.filesHint : `${library.length} · ${s.formatBytes(librarySize)}`}</div>
          </div>
          <span className="muted">
            <Icon name="chevronRight" />
          </span>
        </div>
      </button>

      <Card title={s.settingsTitle}>
        <SettingRow title={s.themeSystem} hint={s.themeSystemHint} on={data.systemTheme} onToggle={() => setState({ systemTheme: !data.systemTheme })} />
        <SettingRow
          title={s.hideProfanity}
          hint={s.hideProfanityHint}
          on={data.hideProfanity}
          onToggle={() => {
            setState({ hideProfanity: !data.hideProfanity });
            syncPush();
          }}
        />
        <SettingRow title={s.chatNotify} hint={user ? s.chatNotifyHint : s.chatNotifySignIn} on={data.chatPush} onToggle={togglePush} />
      </Card>

      <Card title={s.calendarSubTitle} hint={s.calendarSubHint}>
        <div className="row mt4" style={{ marginLeft: -8, flexWrap: "wrap" }}>
          <a className="tbtn" href={webcal}>{s.calendarSubscribe}</a>
          <a className="tbtn" href={`https://calendar.google.com/calendar/render?cid=${encodeURIComponent(webcal)}`} target="_blank" rel="noreferrer">
            {s.calendarGoogle}
          </a>
          <button
            className="tbtn"
            onClick={() => navigator.clipboard?.writeText(feed).then(() => toast(s.linkCopied)).catch(() => setMessage(s.copyFailed))}
          >
            {s.copyLink}
          </button>
        </div>
      </Card>

      <Card title={s.absences} hint={subjects.length === 0 ? s.absencesEmpty : undefined}>
        {subjects.map((subject) => {
          const count = data.absences.filter((a) => a.subject === subject).length;
          const limit = data.absenceLimits[subject];
          const left = limit != null ? limit - count : null;
          return (
            <div key={subject} className="mt12">
              <div className="row">
                <div className="grow semibold small">{subject}</div>
                <div className="small muted">{s.absenceCount(count)}</div>
              </div>
              <div className="row" style={{ gap: 4, marginTop: 2 }}>
                {limit == null ? (
                  <button className="tbtn flush" onClick={() => actions.setLimit(subject, 4)}>{s.setLimit}</button>
                ) : (
                  <>
                    <span className={`small grow ${left != null && left <= 0 ? "red" : "blue"}`}>
                      {left != null && left <= 0 ? s.limitUsed : s.skipsLeft(left ?? 0)}
                    </span>
                    <button className="icon-btn" aria-label="-" onClick={() => actions.setLimit(subject, Math.max(0, limit - 1))}>−</button>
                    <span className="semibold" style={{ minWidth: 20, textAlign: "center" }}>{limit}</span>
                    <button className="icon-btn" aria-label="+" onClick={() => actions.setLimit(subject, limit + 1)}>+</button>
                    <button className="tbtn muted" onClick={() => actions.setLimit(subject, null)}>{s.clearLimit}</button>
                  </>
                )}
              </div>
            </div>
          );
        })}
      </Card>

      <Card title={s.subjectNotesTitle} hint={subjectNotes.length === 0 ? s.subjectNotesEmpty : undefined}>
        {subjectNotes.map(([subject, note]) => (
          <div key={subject} className="mt8">
            <div className="semibold small">{subject}</div>
            <div className="small muted pre">{note}</div>
          </div>
        ))}
      </Card>

      <Card title={s.hiddenTitle} hint={hiddenLessons.length === 0 ? s.hiddenEmpty : undefined}>
        {hiddenLessons.map((lesson) => (
          <div key={lesson.key} className="row mt6">
            <div className="grow small ellipsis">{`${s.dayName(lesson.dayName)} · ${lesson.start} · ${lesson.subject}`}</div>
            <button className="tbtn" onClick={() => actions.setHidden(lesson.key, false)}>{s.restore}</button>
          </div>
        ))}
      </Card>

      <Card title={s.backupSave} hint={s.backupHint}>
        <div className="row mt4" style={{ marginLeft: -8, flexWrap: "wrap" }}>
          <button className="tbtn" onClick={saveBackup}>{s.backupSave}</button>
          <button className="tbtn" onClick={() => backupInput.current?.click()}>{s.backupLoad}</button>
        </div>
        <input
          ref={backupInput}
          type="file"
          accept="application/json,.json"
          hidden
          onChange={(e) => {
            const file = e.target.files?.[0];
            e.target.value = "";
            loadBackup(file);
          }}
        />
      </Card>

      <Card title={s.contact} hint={s.updatesInTelegram}>
        <a className="btn mt12 row" style={{ justifyContent: "center", gap: 8 }} href="https://t.me/ag3r_software" target="_blank" rel="noreferrer">
          <TelegramIcon size={20} />
          {s.telegram}
        </a>
      </Card>

      <div className="info-box">
        <span className="blue">
          <Icon name="info" size={20} />
        </span>
        <div className="small">{s.localData}</div>
      </div>

      <button className="btn" style={{ background: "transparent", color: "var(--red)", boxShadow: "inset 0 0 0 1px var(--line)" }} onClick={() => setResetting(true)}>
        {s.resetData}
      </button>

      <Card title={s.weekHelpTitle}>
        <div className="blue semibold small mt6">{s.weekNow(info.number, info.even)}</div>
        <div className="small mt6" style={{ lineHeight: 1.45 }}>{s.weekHelp}</div>
        <div className="muted xsmall mt8">{s.fetchedLine}</div>
        {bundle.source && (
          <a className="tbtn flush mt4" style={{ display: "inline-block" }} href={bundle.source} target="_blank" rel="noreferrer">
            {s.openSite}
          </a>
        )}
      </Card>

      <div className="muted xsmall" style={{ textAlign: "center" }}>
        {`${s.appName} ${APP_VERSION} · `}
        <button className="blue xsmall" onClick={() => setWhatsNew(true)}>{s.whatsNewLink}</button>
      </div>

      {whatsNew && <WhatsNew s={s} onClose={() => setWhatsNew(false)} />}

      {cropping && (
        <CropPhoto
          file={cropping}
          s={s}
          onCancel={() => setCropping(null)}
          onSave={(blob) => {
            setCropping(null);
            setAvatar(blob).catch(() => setMessage(s.copyFailed));
          }}
        />
      )}

      {resetting && (
        <Alert
          title={s.resetTitle}
          text={
            <>
              {s.resetText}
              {library.length > 0 && (
                <label className="checkbox mt12" style={{ color: "var(--ink)" }}>
                  <input type="checkbox" checked={withFiles} onChange={(e) => setWithFiles(e.target.checked)} />
                  <span>{s.resetFiles(library.length, s.formatBytes(librarySize))}</span>
                </label>
              )}
            </>
          }
          onClose={() => {
            setResetting(false);
            setWithFiles(false);
          }}
        >
          <button
            className="tbtn muted"
            onClick={() => {
              setResetting(false);
              setWithFiles(false);
            }}
          >
            {s.cancel}
          </button>
          <button className="tbtn danger" onClick={confirmReset}>{s.reset}</button>
        </Alert>
      )}

      {message && (
        <Alert title={s.appName} text={message} onClose={() => setMessage(null)}>
          <button className="tbtn" onClick={() => setMessage(null)}>{s.ok}</button>
        </Alert>
      )}
    </div>
  );
}
