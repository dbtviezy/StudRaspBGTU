"use client";

import { useState } from "react";
import type { Copy } from "@/lib/i18n";
import type { Bundle } from "@/lib/schedule";
import { Icon } from "../Icons";
import { Mark } from "../ui";
import { FacultyCard, GroupRow } from "./Groups";

interface Props {
  s: Copy;
  bundle: Bundle;
  initialName: string;
  selectedId: string | null;
  askName?: boolean;
  onBack: (() => void) | null;
  onConfirm: (id: string, name: string) => void;
}

export default function GroupPicker({ s, bundle, initialName, selectedId, askName = true, onBack, onConfirm }: Props) {
  const initialFaculty = bundle.find(selectedId)?.faculty.short ?? null;
  const [facultyShort, setFacultyShort] = useState<string | null>(initialFaculty);
  const [picked, setPicked] = useState<string | null>(selectedId);
  const [name, setName] = useState(initialName);
  const [query, setQuery] = useState("");
  const faculty = bundle.faculties.find((f) => f.short === facultyShort) ?? null;
  const q = query.trim().toLowerCase();
  const groups = faculty ? faculty.groups.filter((g) => !q || g.id.toLowerCase().includes(q)) : [];

  const back = () => {
    if (faculty) {
      setFacultyShort(null);
      setQuery("");
    } else onBack?.();
  };

  return (
    <div className="fullscreen">
      <div style={{ padding: "12px 16px 0" }}>
        <div className="row" style={{ gap: 4 }}>
          {(faculty || onBack) && (
            <button className="icon-btn" style={{ marginLeft: -10 }} aria-label={s.back} onClick={back}>
              <Icon name="arrowBack" />
            </button>
          )}
          <Mark size={40} />
          <div className="grow" style={{ marginLeft: 6 }}>
            <div className="semibold ellipsis" style={{ fontSize: 15 }}>{s.appName}</div>
            <div className="muted xsmall">{s.fullTime}</div>
          </div>
        </div>
        <h1 className="h1 mt16">{faculty ? faculty.short : s.chooseFaculty}</h1>
        <div className="muted small">{faculty ? s.facultyName(faculty.name) : s.facultyThenGroup}</div>
      </div>

      <div className="stack tight" style={{ padding: "16px 16px 8px", flex: 1 }}>
        {!faculty ? (
          bundle.faculties.map((f) => <FacultyCard key={f.short} faculty={f} s={s} onClick={() => setFacultyShort(f.short)} />)
        ) : (
          <>
            {askName && (
              <label className="field">
                <span>{s.name}</span>
                <input
                  className="input"
                  value={name}
                  maxLength={60}
                  placeholder={s.nameHint}
                  autoComplete="given-name"
                  onChange={(e) => setName(e.target.value)}
                />
              </label>
            )}
            <h3 className="h3 mt4">{s.chooseGroup}</h3>
            <label className="search" style={{ display: "block" }}>
              <Icon name="search" />
              <input
                className="input"
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={s.findGroup}
                autoComplete="off"
                enterKeyHint="search"
              />
            </label>
            {groups.map((group) => (
              <GroupRow key={group.id} group={group} faculty={faculty} selected={group.id === picked} s={s} onClick={() => setPicked(group.id)} />
            ))}
            {groups.length === 0 && <div className="muted" style={{ padding: 8 }}>{s.groupMissing}</div>}
          </>
        )}
      </div>

      {faculty && (
        <div className="picker-foot">
          <button className="btn" disabled={!picked} onClick={() => picked && onConfirm(picked, name)}>
            {askName ? s.openSchedule : s.done}
          </button>
        </div>
      )}
    </div>
  );
}
