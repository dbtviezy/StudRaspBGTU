"use client";

import { useMemo, useState } from "react";
import type { Copy } from "@/lib/i18n";
import { type Faculty, type Group, nextTeacherLessons, searchTeachers } from "@/lib/schedule";
import { type Summary, summaryFor, useSummaries } from "@/lib/ratings";
import { actions, setState } from "@/lib/store";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { Chip, Stars } from "../ui";
import { teacherFollowLine } from "./Home";

export function facultyColor(short: string): string {
  switch (short) {
    case "МТФ": return "#3B6FE0";
    case "УНИТ": return "var(--teal)";
    case "УНТИ": return "var(--amber)";
    case "ФОЦЭ": return "var(--violet)";
    case "ФЭЭ": return "#2E8CFF";
    default: return "var(--blue)";
  }
}

export function FacultyCard({ faculty, s, onClick }: { faculty: Faculty; s: Copy; onClick: () => void }) {
  const color = facultyColor(faculty.short);
  return (
    <button className="card row" style={{ gap: 14 }} onClick={onClick}>
      <span
        style={{
          width: 48,
          height: 48,
          borderRadius: 14,
          background: `color-mix(in srgb, ${color} 14%, transparent)`,
          color,
          fontWeight: 700,
          fontSize: 12,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          flexShrink: 0,
        }}
      >
        {faculty.short}
      </span>
      <div className="grow">
        <div className="semibold" style={{ fontSize: 15 }}>{faculty.name}</div>
        <div className="muted small mt2">{s.groupsCount(faculty.groups.length)}</div>
      </div>
      <span className="muted">
        <Icon name="chevronRight" />
      </span>
    </button>
  );
}

export function GroupRow({ group, faculty, selected, s, onClick }: { group: Group; faculty: Faculty; selected: boolean; s: Copy; onClick: () => void }) {
  const meta = [s.levelName(group.level), group.year != null ? s.cohortValue(group.year) : null, faculty.short].filter(Boolean).join(" · ");
  return (
    <button
      className="row"
      style={{ width: "100%", textAlign: "left", borderRadius: 18, background: selected ? "var(--blue-soft)" : "var(--card)", padding: "12px 14px" }}
      onClick={onClick}
    >
      <div className="grow">
        <div className="semibold" style={{ fontSize: 15 }}>{group.id}</div>
        <div className="muted xsmall mt2">{meta}</div>
      </div>
      {selected && (
        <span className="blue">
          <Icon name="check" />
        </span>
      )}
    </button>
  );
}

export function RatingLine({ summary, s }: { summary: Summary | undefined; s: Copy }) {
  if (!summary || summary.count === 0) return <span className="muted xsmall">{s.noRatings}</span>;
  return (
    <span className="row xsmall" style={{ gap: 6 }}>
      <Stars value={summary.overall} size={13} />
      <span className="semibold">{summary.overall.toFixed(1)}</span>
      <span className="muted">· {s.ratingsCount(summary.count)}</span>
    </span>
  );
}

function StarToggle({ on, s, onClick }: { on: boolean; s: Copy; onClick: () => void }) {
  return (
    <button
      className="icon-btn"
      style={{ color: on ? "var(--blue)" : "var(--muted)" }}
      aria-label={on ? s.removeFavorite : s.addFavorite}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
    >
      <Icon name={on ? "star" : "starBorder"} />
    </button>
  );
}

export default function GroupsScreen() {
  const { bundle, s, data, now, today, group: current, setTab, openTeacher } = useShell();
  const [query, setQuery] = useState("");
  const [mode, setMode] = useState<"groups" | "teachers">("groups");
  const [opened, setOpened] = useState<string | null>(null);
  const summaries = useSummaries();
  const favorites = [...data.favoriteTeachers].sort();
  const search = query.trim();
  const teachers = useMemo(() => (mode === "teachers" ? searchTeachers(bundle, query) : []), [bundle, query, mode]);
  const results = useMemo(() => {
    if (!search) return [];
    const q = search.toLowerCase();
    return bundle.groups.filter(
      ({ faculty, group }) =>
        group.id.toLowerCase().includes(q) || faculty.name.toLowerCase().includes(q) || faculty.short.toLowerCase().includes(q) || group.level.toLowerCase().includes(q),
    );
  }, [bundle, search]);
  const faculty = bundle.faculties.find((f) => f.short === opened) ?? null;
  const pick = (id: string) => {
    setState({ group: id });
    setTab(1);
  };

  return (
    <div className="stack tight">
      <div>
        {faculty && !search ? (
          <div className="row">
            <button className="icon-btn" style={{ marginLeft: -10 }} aria-label={s.toFaculties} onClick={() => setOpened(null)}>
              <Icon name="arrowBack" />
            </button>
            <div className="grow">
              <h1 className="h1" style={{ fontSize: 24 }}>{faculty.short}</h1>
              <div className="muted small">{faculty.name}</div>
            </div>
          </div>
        ) : (
          <>
            <h1 className="h1">{s.groupsTitle}</h1>
            <div className="muted small">{s.groupsHint}</div>
          </>
        )}
        <div className="row mt8">
          <Chip on={mode === "groups"} onClick={() => setMode("groups")}>{s.groupsMode}</Chip>
          <Chip on={mode === "teachers"} onClick={() => setMode("teachers")}>{s.teachersMode}</Chip>
        </div>
        <label className="search mt8" style={{ display: "block" }}>
          <Icon name="search" />
          <input
            className="input"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={mode === "teachers" ? s.findTeacher : s.findGroupOrFaculty}
            autoComplete="off"
            enterKeyHint="search"
          />
        </label>
      </div>

      {mode === "teachers" ? (
        search.length < 2 ? (
          <>
            {favorites.length > 0 && <h3 className="h3">{s.favoriteTeachers}</h3>}
            {favorites.map((name) => {
              const next = nextTeacherLessons(bundle, name, today, now);
              return (
                <div key={`fav|${name}`} className="card" role="button" tabIndex={0} style={{ cursor: "pointer" }} onClick={() => openTeacher(name)}>
                  <div className="row">
                    <div className="grow">
                      <div className="semibold">{name}</div>
                      <div className="mt2">
                        <RatingLine summary={summaryFor(summaries, name)} s={s} />
                      </div>
                    </div>
                    <StarToggle on s={s} onClick={() => actions.setFavorite(name, false)} />
                  </div>
                  {next.length === 0 ? (
                    <div className="muted small">{s.noTeacherSoon}</div>
                  ) : (
                    next.map((item) => (
                      <div key={`${item.day}${item.groupId}${item.lesson.key}`} className="blue small mt4">
                        {teacherFollowLine(s, today, item)}
                      </div>
                    ))
                  )}
                </div>
              );
            })}
            <div className="muted" style={{ padding: 8 }}>{s.teacherHint}</div>
          </>
        ) : teachers.length === 0 ? (
          <div className="muted" style={{ padding: 8 }}>{s.nothingFound}</div>
        ) : (
          teachers.map((hit) => {
            const starred = favorites.includes(hit.teacher.trim());
            return (
              <div
                key={`${hit.teacher}|${hit.groupId}|${hit.lesson.key}`}
                className="card row"
                role="button"
                tabIndex={0}
                style={{ cursor: "pointer", alignItems: "flex-start" }}
                onClick={() => hit.teacher.trim() && openTeacher(hit.teacher)}
              >
                <div className="grow">
                  <div className="semibold">{hit.teacher}</div>
                  {hit.teacher.trim() && (
                    <div className="mt2">
                      <RatingLine summary={summaryFor(summaries, hit.teacher)} s={s} />
                    </div>
                  )}
                  <div className="muted small mt2">{`${s.dayName(hit.lesson.dayName)} · ${hit.lesson.start} · ${hit.lesson.subject}`}</div>
                  <div className="blue xsmall mt2">{`${hit.groupId} · ${hit.facultyShort} · ${hit.lesson.room}`}</div>
                </div>
                {hit.teacher.trim() && <StarToggle on={starred} s={s} onClick={() => actions.setFavorite(hit.teacher, !starred)} />}
              </div>
            );
          })
        )
      ) : search ? (
        <>
          {results.map(({ faculty: f, group }) => (
            <GroupRow key={group.id + f.short} group={group} faculty={f} selected={group.id === current.id} s={s} onClick={() => pick(group.id)} />
          ))}
          {results.length === 0 && <div className="muted" style={{ padding: 8 }}>{s.nothingFound}</div>}
        </>
      ) : !faculty ? (
        bundle.faculties.map((f) => <FacultyCard key={f.short} faculty={f} s={s} onClick={() => setOpened(f.short)} />)
      ) : (
        faculty.groups.map((group) => (
          <GroupRow key={group.id} group={group} faculty={faculty} selected={group.id === current.id} s={s} onClick={() => pick(group.id)} />
        ))
      )}
    </div>
  );
}
