"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import type { Copy } from "@/lib/i18n";
import {
  deleteReview,
  fetchTeacher,
  RatingError,
  reportReview,
  type Review,
  saveReview,
  type Scores,
  type TeacherRatings,
  useSummaries,
  voteReview,
} from "@/lib/ratings";
import { maskIf } from "@/lib/profanity";
import { nextTeacherLessons, splitTeachers, teacherHeading, teacherWhere } from "@/lib/schedule";
import { actions } from "@/lib/store";
import { reviewBlock, useAccount } from "@/lib/account";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { Alert, Overlay, Sheet, StarPicker, Stars } from "../ui";
import ReportDialog from "./Report";

const MAX_TEXT = 300;

function errorText(s: Copy, error: unknown): string {
  const code = error instanceof RatingError ? error.code : "";
  switch (code) {
    case "offline":
    case "db_unavailable":
    case "not_configured": return s.ratingOffline;
    case "rate_limited": return s.rateLimited;
    case "text_too_long": return s.textTooLong;
    case "unknown_teacher": return s.unknownTeacher;
    case "banned":
    case "restricted":
    case "social_required":
    case "account_too_new":
    case "device_used":
    case "own_review":
    case "unauthorized": return s.failureText(error);
    default: return s.ratingFailed;
  }
}

/** Runs `action` only when signed in; otherwise explains why an account is needed and offers to sign in. */
function useAccountGate(): [(action: () => void) => void, React.ReactNode] {
  const { s, signIn } = useShell();
  const { user } = useAccount();
  const [asking, setAsking] = useState(false);
  const gate = (action: () => void) => (user ? action() : setAsking(true));
  const alert = asking ? (
    <Alert title={s.needAccountTitle} text={s.needAccountText} onClose={() => setAsking(false)}>
      <button className="tbtn muted" onClick={() => setAsking(false)}>{s.cancel}</button>
      <button
        className="tbtn"
        onClick={() => {
          setAsking(false);
          signIn();
        }}
      >
        {s.signIn}
      </button>
    </Alert>
  ) : null;
  return [gate, alert];
}

function shortDate(s: Copy, iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getDate()} ${s.monthsGenitive[date.getMonth()]} ${date.getFullYear()}`;
}

function ScoreRow({ label, value }: { label: string; value: number }) {
  return (
    <div className="row mt6">
      <span className="grow small">{label}</span>
      <Stars value={value} size={14} />
      <span className="semibold small" style={{ width: 28, textAlign: "right" }}>{value > 0 ? value.toFixed(1) : "—"}</span>
    </div>
  );
}

function VoteButton({ on, icon, count, label, onClick }: { on: boolean; icon: "thumbUp" | "thumbDown"; count: number; label: string; onClick: () => void }) {
  return (
    <button className={`vote${on ? " on" : ""}`} aria-label={label} aria-pressed={on} onClick={onClick}>
      <Icon name={icon} size={16} />
      <span>{count}</span>
    </button>
  );
}

function WhereCard({ name }: { name: string }) {
  const { bundle, s, today, now } = useShell();
  const where = teacherWhere(bundle, name, today, now);
  const title = !where ? s.whereNone : where.inClass ? s.whereInClass : s.whereFree;
  const when = !where
    ? ""
    : where.inClass
      ? `${s.untilTime(where.lesson.end)} · ${s.leftPhrase(where.minutes)}`
      : where.day === today
        ? `${s.whereNext}: ${where.lesson.start} · ${s.untilPhrase(where.minutes)}`
        : `${s.whereNext}: ${s.formatLongDate(where.day)}, ${where.lesson.start}`;
  return (
    <div className="card mt12">
      <div className="muted xsmall semibold">{s.whereTitle.toUpperCase()}</div>
      <div className={`semibold mt4 ${where?.inClass ? "blue" : ""}`}>{title}</div>
      {where && (
        <>
          <div className="small mt2">{when}</div>
          <div className="small mt6">
            {[s.roomName(where.lesson.room), where.lesson.subject].filter(Boolean).join(" · ")}
          </div>
          <div className="muted xsmall mt2">{s.groupsLine(where.groups)}</div>
        </>
      )}
    </div>
  );
}

function ReviewCard({ review, s, title, own }: { review: Review; s: Copy; title?: string; own?: boolean }) {
  const { toast, data } = useShell();
  const [votes, setVotes] = useState({ likes: review.likes ?? 0, dislikes: review.dislikes ?? 0, myVote: review.myVote ?? 0 });
  const [reporting, setReporting] = useState(false);
  const [gate, gateAlert] = useAccountGate();

  const vote = async (value: number) => {
    const next = votes.myVote === value ? 0 : value;
    const previous = votes;
    setVotes({
      likes: votes.likes - (votes.myVote === 1 ? 1 : 0) + (next === 1 ? 1 : 0),
      dislikes: votes.dislikes - (votes.myVote === -1 ? 1 : 0) + (next === -1 ? 1 : 0),
      myVote: next,
    });
    try {
      setVotes(await voteReview(review.id, next));
    } catch (e) {
      setVotes(previous);
      toast(s.failureText(e));
    }
  };

  return (
    <div className="mt12" style={{ borderTop: "1px solid var(--line)", paddingTop: 10 }}>
      <div className="row">
        <Stars value={review.overall} size={14} />
        <span className="grow semibold small">{title ?? ""}</span>
        <span className="muted xsmall">{shortDate(s, review.updatedAt)}</span>
      </div>
      <div className="muted xsmall mt2">
        {`${s.delivery} ${review.delivery} · ${s.strictness} ${review.strictness} · ${s.loyalty} ${review.loyalty}`}
      </div>
      {review.text && <div className="small mt4 pre" style={{ overflowWrap: "anywhere" }}>{own ? review.text : maskIf(data.hideProfanity, review.text)}</div>}
      {review.id > 0 && (
        <div className="row mt6" style={{ gap: 6 }}>
          {own ? (
            <span className="row muted xsmall" style={{ gap: 4 }}>
              <Icon name="thumbUp" size={14} />
              {votes.likes}
              <Icon name="thumbDown" size={14} />
              {votes.dislikes}
            </span>
          ) : (
            <>
              <VoteButton on={votes.myVote === 1} icon="thumbUp" count={votes.likes} label={s.like} onClick={() => gate(() => vote(1))} />
              <VoteButton on={votes.myVote === -1} icon="thumbDown" count={votes.dislikes} label={s.dislike} onClick={() => gate(() => vote(-1))} />
              <span className="grow" />
              <button className="icon-btn muted" aria-label={s.report} title={s.report} onClick={() => gate(() => setReporting(true))}>
                <Icon name="flag" size={18} />
              </button>
            </>
          )}
        </div>
      )}
      {reporting && <ReportDialog onSend={(reason) => reportReview(review.id, reason)} onClose={() => setReporting(false)} />}
      {gateAlert}
    </div>
  );
}

function RatingSheet({ teacher, mine, onClose, onDone }: { teacher: string; mine: Review | null; onClose: () => void; onDone: () => void }) {
  const { s } = useShell();
  const [scores, setScores] = useState<Scores>({
    overall: mine?.overall ?? 0,
    delivery: mine?.delivery ?? 0,
    strictness: mine?.strictness ?? 0,
    loyalty: mine?.loyalty ?? 0,
  });
  const [text, setText] = useState(mine?.text ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const length = Array.from(text).length;
  const complete = Object.values(scores).every((v) => v >= 1);

  const run = async (body: () => Promise<unknown>) => {
    setBusy(true);
    setError(null);
    try {
      await body();
      onDone();
    } catch (e) {
      setError(errorText(s, e));
    } finally {
      setBusy(false);
    }
  };

  const set = (key: keyof Scores) => (value: number) => setScores((current) => ({ ...current, [key]: value }));
  const rows: [keyof Scores, string][] = [
    ["overall", s.overall],
    ["delivery", s.delivery],
    ["strictness", s.strictness],
    ["loyalty", s.loyalty],
  ];

  return (
    <Sheet
      onClose={onClose}
      foot={
        <div className="row" style={{ justifyContent: "flex-end", gap: 4 }}>
          {mine && (
            <button className="tbtn danger" disabled={busy} onClick={() => run(() => deleteReview(teacher))} style={{ marginRight: "auto" }}>
              {s.deleteReview}
            </button>
          )}
          <button className="tbtn muted" onClick={onClose}>{s.cancel}</button>
          <button
            className="tbtn"
            disabled={busy}
            onClick={() => {
              if (!complete) return setError(s.pickOverall);
              if (length > MAX_TEXT) return setError(s.textTooLong);
              run(() => saveReview(teacher, scores, text));
            }}
          >
            {mine ? s.save : s.send}
          </button>
        </div>
      }
    >
      <h2 className="h2" style={{ fontSize: 20 }}>{mine ? s.editReview : s.rateTeacher}</h2>
      <div className="muted small mt2 pre">{teacherHeading(teacher)}</div>
      {rows.map(([key, label]) => (
        <div key={key} className="mt12">
          <div className="small semibold">{label}</div>
          <StarPicker value={scores[key]} onChange={set(key)} label={label} size={key === "overall" ? 34 : 28} />
        </div>
      ))}
      <label className="field mt16">
        <span>{s.reviewText}</span>
        <textarea
          className="input"
          rows={4}
          maxLength={MAX_TEXT}
          value={text}
          placeholder={s.reviewHint}
          onChange={(e) => setText(e.target.value)}
        />
      </label>
      <div className={`counter${length > MAX_TEXT ? " red" : ""}`}>{`${length}/${MAX_TEXT}`}</div>
      <div className="muted xsmall mt8">{s.ratingAnon}</div>
      {error && <div className="red small mt8">{error}</div>}
    </Sheet>
  );
}

function RatingCard({ name, showName }: { name: string; showName: boolean }) {
  const { s } = useShell();
  const { user } = useAccount();
  const summaries = useSummaries();
  const [ratings, setRatings] = useState<TeacherRatings | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [rating, setRating] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [blocked, setBlocked] = useState<string | null>(null);
  const [gate, gateAlert] = useAccountGate();
  const load = useCallback(() => setAttempt((n) => n + 1), []);
  const userId = user?.id ?? null;

  const startRating = () =>
    gate(() => {
      const block = reviewBlock(user);
      if (block === "social") return setBlocked(s.socialRequired);
      if (block && typeof block === "object") return setBlocked(s.accountTooNew(block.until));
      setBlocked(null);
      setRating(true);
    });

  useEffect(() => {
    let alive = true;
    fetchTeacher(name)
      .then((result) => {
        if (!alive) return;
        setRatings(result);
        setError(null);
      })
      .catch((e) => alive && setError(errorText(s, e)));
    return () => {
      alive = false;
    };
  }, [name, attempt, s, userId]);

  const summary = ratings?.summary ?? summaries.get(name) ?? null;
  const mine = ratings?.mine ?? null;
  const mineNote = mine?.claimable
    ? mine.hidden ? s.mineHiddenUnclaimed : s.legacyMine(ratings?.legacyUntil ?? null)
    : null;
  const others = (ratings?.reviews ?? []).filter((r) => !(mine && r.id === mine.id));

  return (
    <>
      <div className="card mt12">
        <h2 className="h2">{showName ? `${s.rating} · ${name}` : s.rating}</h2>
        {summary && summary.count > 0 ? (
          <>
            <div className="row mt8" style={{ gap: 10 }}>
              <span className="bold" style={{ fontSize: 34, lineHeight: 1 }}>{summary.overall.toFixed(1)}</span>
              <div>
                <Stars value={summary.overall} size={18} />
                <div className="muted xsmall mt2">{s.ratingsCount(summary.count)}</div>
              </div>
            </div>
            <ScoreRow label={s.delivery} value={summary.delivery} />
            <ScoreRow label={s.strictness} value={summary.strictness} />
            <ScoreRow label={s.loyalty} value={summary.loyalty} />
          </>
        ) : (
          <div className="muted small mt4">{s.noRatings}</div>
        )}
        {error ? (
          <div className="mt8">
            <div className="red small">{error}</div>
            <button className="tbtn flush" onClick={load}>{s.retry}</button>
          </div>
        ) : (
          <button className="btn compact mt12" disabled={!ratings} onClick={startRating}>
            {mine ? s.editReview : s.rateTeacher}
          </button>
        )}
        {blocked && <div className="red small mt8">{blocked}</div>}
        {mine && <ReviewCard key={`mine-${mine.updatedAt}`} review={mine} s={s} title={s.yourReview} own />}
        {mineNote && <div className="red xsmall mt4">{mineNote}</div>}
        {ratings && (
          <>
            <h3 className="h3 mt16">{s.reviews}</h3>
            {others.length === 0 && !mine && <div className="muted small mt4">{s.noReviews}</div>}
            {others.map((review, index) => (
              <ReviewCard key={`${review.id}-${index}`} review={review} s={s} />
            ))}
          </>
        )}
      </div>
      {rating && (
        <RatingSheet
          teacher={name}
          mine={mine}
          onClose={() => setRating(false)}
          onDone={() => {
            setRating(false);
            load();
          }}
        />
      )}
      {gateAlert}
    </>
  );
}

export default function TeacherDialog({ name: raw, onClose }: { name: string; onClose: () => void }) {
  const { bundle, s, data, today, now } = useShell();
  const name = raw.trim();
  const favorite = data.favoriteTeachers.includes(name);
  const people = useMemo(() => splitTeachers(name), [name]);
  const lessons = nextTeacherLessons(bundle, name, today, now, 60);
  const days = [...new Set(lessons.map((l) => l.day))];

  return (
    <Overlay onClose={onClose}>
      <div className="row" style={{ alignItems: "flex-start" }}>
        <button className="icon-btn" style={{ marginLeft: -10 }} aria-label={s.back} onClick={onClose}>
          <Icon name="arrowBack" />
        </button>
        <h1 className="h1 grow pre" style={{ fontSize: 22, paddingTop: 8 }}>{teacherHeading(name)}</h1>
        <button
          className="icon-btn"
          style={{ color: favorite ? "var(--blue)" : "var(--muted)" }}
          aria-label={favorite ? s.removeFavorite : s.addFavorite}
          onClick={() => actions.setFavorite(name, !favorite)}
        >
          <Icon name={favorite ? "star" : "starBorder"} />
        </button>
      </div>

      <WhereCard name={name} />

      {people.map((person) => (
        <RatingCard key={person} name={person} showName={people.length > 1} />
      ))}

      <h2 className="h2 mt16">{s.teacherSoon}</h2>
      {days.length === 0 && <div className="muted small mt4">{s.noTeacherSoon}</div>}
      {days.map((day) => (
        <div key={day} className="mt12">
          <div className="blue semibold small">{s.formatLongDateCap(day)}</div>
          {lessons
            .filter((l) => l.day === day)
            .map((item) => (
              <div key={`${item.groupId}|${item.lesson.key}`} className="card mt6" style={{ padding: "10px 14px" }}>
                <div className="row">
                  <span className="bold small" style={{ width: 48 }}>{item.lesson.start}</span>
                  <div className="grow">
                    <div className="semibold small">{item.lesson.subject}</div>
                    <div className="muted xsmall">{[item.groupId, item.facultyShort, s.roomName(item.lesson.room), s.lessonType(item.lesson.type)].filter(Boolean).join(" · ")}</div>
                  </div>
                </div>
              </div>
            ))}
        </div>
      ))}
    </Overlay>
  );
}
