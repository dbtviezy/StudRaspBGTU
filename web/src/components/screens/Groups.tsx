"use client";

import { Fragment, useMemo, useState } from "react";
import type { Copy } from "@/lib/i18n";
import { type Faculty, type Group, nextTeacherLessons, searchTeachers, type TeacherCard } from "@/lib/schedule";
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
        <div className="semibold" style={{ fontSize: 15 }}>{s.facultyName(faculty.name)}</div>
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

function TeacherRow({ name, card, summary, favorite, onFavorite }: { name: string; card?: TeacherCard; summary: Summary | undefined; favorite: boolean; onFavorite: () => void }) {
  const { bundle, s, now, today, openTeacher } = useShell();
  const next = nextTeacherLessons(bundle, name, today, now, 1)[0];
  const subjects = card?.subjects ?? [];
  return (
    <div className="card row" role="button" tabIndex={0} style={{ cursor: "pointer", alignItems: "flex-start" }} onClick={() => openTeacher(name)}>
      <div className="grow">
        <div className="semibold">{name}</div>
        <div className="mt2">
          <RatingLine summary={summary} s={s} />
        </div>
        {subjects.length > 0 && (
          <div className="muted small mt2 ellipsis">
            {subjects.slice(0, 2).join(" · ") + (subjects.length > 2 ? ` +${subjects.length - 2}` : "")}
          </div>
        )}
        {card && <div className="muted xsmall mt2">{s.teacherGroups(card.groups)}</div>}
        <div className={`xsmall mt4 ${next ? "blue" : "muted"}`}>{next ? teacherFollowLine(s, today, next) : s.noTeacherSoon}</div>
      </div>
      <StarToggle on={favorite} s={s} onClick={onFavorite} />
    </div>
  );
}

export default function GroupsScreen() {
  const { bundle, s, data, group: current, setTab } = useShell();
  const [query, setQuery] = useState("");
  const [mode, setMode] = useState<"groups" | "teachers">("groups");
  const [opened, setOpened] = useState<string | null>(null);
  const summaries = useSummaries();
  const favorites = [...data.favoriteTeachers].sort();
  const search = query.trim();
  const searching = search.length >= 2;
  const teachers = useMemo(() => {
    if (mode !== "teachers") return [];
    const found = searchTeachers(bundle, searching ? search : "", current);
    return searching ? found : found.filter((card) => !data.favoriteTeachers.includes(card.name));
  }, [bundle, search, searching, mode, current, data.favoriteTeachers]);
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
              <div className="muted small">{s.facultyName(faculty.name)}</div>
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
        <>
          {!searching && favorites.length > 0 && (
            <>
              <h3 className="h3">{s.favoriteTeachers}</h3>
              {favorites.map((name) => (
                <TeacherRow key={`fav|${name}`} name={name} summary={summaryFor(summaries, name)} favorite onFavorite={() => actions.setFavorite(name, false)} />
              ))}
            </>
          )}
          {searching && teachers.length === 0 && <div className="muted" style={{ padding: 8 }}>{s.nothingFound}</div>}
          {teachers.some((t) => t.mine) && <h3 className="h3 mt4">{s.myTeachers}</h3>}
          {!searching && teachers.length === 0 && favorites.length === 0 && <div className="muted small" style={{ padding: "0 8px" }}>{s.myTeachersEmpty}</div>}
          {teachers.map((card, index) => (
            <Fragment key={card.name}>
              {!card.mine && (index === 0 || teachers[index - 1].mine) && <h3 className="h3 mt4">{s.otherTeachers}</h3>}
              <TeacherRow
                name={card.name}
                card={card}
                summary={summaries.get(card.name)}
                favorite={favorites.includes(card.name)}
                onFavorite={() => actions.setFavorite(card.name, !favorites.includes(card.name))}
              />
            </Fragment>
          ))}
          {!searching && <div className="muted" style={{ padding: 8 }}>{s.teacherSearchHint}</div>}
        </>
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
