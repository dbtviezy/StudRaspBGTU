"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import type { Copy } from "@/lib/i18n";
import { deleteReview, fetchTeacher, RatingError, type Review, saveReview, type Scores, type TeacherRatings, useSummaries } from "@/lib/ratings";
import { nextTeacherLessons, splitTeachers, teacherHeading } from "@/lib/schedule";
import { actions } from "@/lib/store";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { Overlay, Sheet, StarPicker, Stars } from "../ui";

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
    default: return s.ratingFailed;
  }
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

function ReviewCard({ review, s, title }: { review: Review; s: Copy; title?: string }) {
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
      {review.text && <div className="small mt4 pre" style={{ overflowWrap: "anywhere" }}>{review.text}</div>}
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
  const summaries = useSummaries();
  const [ratings, setRatings] = useState<TeacherRatings | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [rating, setRating] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const load = useCallback(() => setAttempt((n) => n + 1), []);

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
  }, [name, attempt, s]);

  const summary = ratings?.summary ?? summaries.get(name) ?? null;
  const mine = ratings?.mine ?? null;
  const others = (ratings?.reviews ?? []).filter((r) => !(mine && r.updatedAt === mine.updatedAt && r.text === mine.text && r.overall === mine.overall));

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
          <button className="btn compact mt12" disabled={!ratings} onClick={() => setRating(true)}>
            {mine ? s.editReview : s.rateTeacher}
          </button>
        )}
        {mine && <ReviewCard review={mine} s={s} title={s.yourReview} />}
        {ratings && (
          <>
            <h3 className="h3 mt16">{s.reviews}</h3>
            {others.length === 0 && !mine && <div className="muted small mt4">{s.noReviews}</div>}
            {others.map((review, index) => (
              <ReviewCard key={`${review.updatedAt}-${index}`} review={review} s={s} />
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
                    <div className="muted xsmall">{[item.groupId, item.facultyShort, item.lesson.room, s.lessonType(item.lesson.type)].filter(Boolean).join(" · ")}</div>
                  </div>
                </div>
              </div>
            ))}
        </div>
      ))}
    </Overlay>
  );
}
