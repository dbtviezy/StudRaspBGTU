"use client";

import { useRef, useState } from "react";
import { addFile, deleteFile, openFile, useLibrary } from "@/lib/idb";
import { actions } from "@/lib/store";
import { useShell } from "../context";
import { Alert, Overlay, OverlayHeader } from "../ui";
import { LessonChooser, lessonLabelOf } from "./Tasks";

export default function FilesOverlay({ onClose }: { onClose: () => void }) {
  const { s, data, group } = useShell();
  const library = useLibrary();
  const input = useRef<HTMLInputElement>(null);
  const [copying, setCopying] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [attaching, setAttaching] = useState<string | null>(null);
  const [removing, setRemoving] = useState<string | null>(null);

  const add = async (list: FileList | null) => {
    if (!list?.length) return;
    setCopying(true);
    const names: string[] = [];
    try {
      for (const file of Array.from(list)) names.push(await addFile(file));
      setMessage(s.filesCopied(names.join(", ")));
    } catch {
      setMessage(s.copyFailed);
    } finally {
      setCopying(false);
    }
  };

  const labelOf = (key: string) => {
    const lesson = group.lessons.find((l) => l.key === key);
    return lesson ? lessonLabelOf(s, lesson) : null;
  };

  return (
    <Overlay onClose={onClose}>
      <OverlayHeader title={s.files} onClose={onClose} closeLabel={s.close} />
      <div className="muted small">{s.filesBody}</div>
      <button className="btn compact mt12" disabled={copying} onClick={() => input.current?.click()}>
        {copying ? s.copyingFiles : s.addFiles}
      </button>
      <input
        ref={input}
        type="file"
        multiple
        hidden
        onChange={(e) => {
          add(e.target.files).finally(() => {
            e.target.value = "";
          });
        }}
      />
      {library.length === 0 && <div className="muted mt16" style={{ textAlign: "center" }}>{s.empty}</div>}
      <div className="stack tight mt12">
        {library.map((file) => {
          const lessons = (data.fileTags[file.name] ?? []).map((key) => ({ key, label: labelOf(key) })).filter((l) => l.label);
          return (
            <div key={file.name} className="card" style={{ padding: "12px 14px" }}>
              <div className="semibold" style={{ overflowWrap: "anywhere" }}>{file.name}</div>
              <div className="muted xsmall mt2">{s.formatBytes(file.size)}</div>
              {lessons.map(({ key, label }) => (
                <div key={key} className="row mt4">
                  <span className="grow blue xsmall ellipsis">{label}</span>
                  <button className="tbtn muted" style={{ padding: "4px 6px", fontSize: 12 }} onClick={() => actions.linkFile(file.name, key, false)}>
                    {s.removeLesson}
                  </button>
                </div>
              ))}
              <div className="row mt4" style={{ marginLeft: -8, flexWrap: "wrap", gap: 0 }}>
                <button className="tbtn" onClick={() => openFile(file.name)}>{s.open}</button>
                <button className="tbtn" onClick={() => setAttaching(file.name)}>{s.attachToLesson}</button>
                <button className="tbtn danger" onClick={() => setRemoving(file.name)}>{s.delete}</button>
              </div>
            </div>
          );
        })}
      </div>

      {attaching && (
        <LessonChooser
          onClose={() => setAttaching(null)}
          onPick={(lesson) => {
            actions.linkFile(attaching, lesson.key, true);
            setAttaching(null);
          }}
        />
      )}
      {removing && (
        <Alert title={s.delete} text={removing} onClose={() => setRemoving(null)}>
          <button className="tbtn muted" onClick={() => setRemoving(null)}>{s.cancel}</button>
          <button
            className="tbtn danger"
            onClick={() => {
              const name = removing;
              setRemoving(null);
              deleteFile(name)
                .then(() => actions.forgetFile(name))
                .catch(() => undefined);
            }}
          >
            {s.delete}
          </button>
        </Alert>
      )}
      {message && (
        <Alert title={s.files} text={message} onClose={() => setMessage(null)}>
          <button className="tbtn" onClick={() => setMessage(null)}>{s.ok}</button>
        </Alert>
      )}
    </Overlay>
  );
}
