"use client";

import { useEffect, useState } from "react";
import { useAccount } from "@/lib/account";
import {
  addGroupTask,
  cachedChats,
  cachedGroupTasks,
  cachedPolls,
  type ChatInfo,
  closePoll,
  createPoll,
  deleteGroupTask,
  fetchMembers,
  type GroupTask,
  isStaff,
  type Member,
  type Poll,
  refreshGroupTasks,
  refreshPolls,
  setMemberRole,
  useChatVersion,
  votePoll,
} from "@/lib/chat";
import type { Copy } from "@/lib/i18n";
import { maskIf } from "@/lib/profanity";
import { actions } from "@/lib/store";
import { useShell } from "../context";
import { Icon } from "../Icons";
import { Sheet } from "../ui";

const errorOf = (s: Copy, e: unknown) => s.failureText(e);

export function PinnedBar({ chat, onUnpin }: { chat: ChatInfo; onUnpin: (() => void) | null }) {
  const { s, data } = useShell();
  const [open, setOpen] = useState(false);
  if (!chat.pinnedBody) return null;
  return (
    <div className="pinned-bar">
      <div className="inner">
        <span className="blue">
          <Icon name="pin" size={18} />
        </span>
        <button className="grow" style={{ textAlign: "left", minWidth: 0 }} onClick={() => setOpen(!open)}>
          <div className="blue xsmall semibold">{`${s.pinned}${chat.pinnedAuthor ? ` · ${chat.pinnedAuthor}` : ""}`}</div>
          <div className={`small${open ? " pre" : " ellipsis"}`} style={{ overflowWrap: "anywhere" }}>{maskIf(data.hideProfanity, chat.pinnedBody)}</div>
        </button>
        {onUnpin && (
          <button className="icon-btn muted" aria-label={s.unpin} onClick={onUnpin}>
            <Icon name="close" size={18} />
          </button>
        )}
      </div>
    </div>
  );
}

export function MembersSheet({ chat, onClose }: { chat: ChatInfo; onClose: () => void }) {
  const { s, toast } = useShell();
  const { user } = useAccount();
  const [members, setMembers] = useState<Member[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const owner = chat.role === "owner";

  useEffect(() => {
    let alive = true;
    fetchMembers(chat.id, chat.groupId)
      .then((list) => alive && setMembers(list))
      .catch((e) => alive && setError(errorOf(s, e)));
    return () => {
      alive = false;
    };
  }, [chat.id, chat.groupId, attempt, s]);

  const change = (member: Member, role: "helper" | "member") => {
    setMemberRole(chat.id, member.userId, role)
      .then(() => setAttempt((n) => n + 1))
      .catch((e) => toast(errorOf(s, e)));
  };

  return (
    <Sheet onClose={onClose}>
      <h2 className="h2" style={{ fontSize: 20 }}>{s.members}</h2>
      <div className="muted small mt4">{s.helpersHint}</div>
      {error && <div className="red small mt8">{error}</div>}
      {members?.map((member) => (
        <div key={member.userId} className="row mt12" style={{ gap: 10 }}>
          <span className="avatar" style={{ width: 36, height: 36, fontSize: 14 }}>{member.name[0]?.toUpperCase()}</span>
          <div className="grow">
            <div className="semibold small">
              {member.userId === user?.id ? `${member.name} (${s.you})` : member.name}
              {member.rep && <span className="badge-rep">{s.roleRep}</span>}
            </div>
            {member.role !== "member" && <div className="blue xsmall">{member.role === "owner" ? s.roleOwner : s.roleHelper}</div>}
          </div>
          {owner && member.role === "member" && (
            <button className="tbtn" onClick={() => change(member, "helper")}>{s.makeHelper}</button>
          )}
          {owner && member.role === "helper" && (
            <button className="tbtn muted" onClick={() => change(member, "member")}>{s.removeHelper}</button>
          )}
        </div>
      ))}
    </Sheet>
  );
}

function PollCard({ chat, poll }: { chat: ChatInfo; poll: Poll }) {
  const { s, data, toast } = useShell();
  const [picked, setPicked] = useState<number[]>(poll.mine);
  const [busy, setBusy] = useState(false);
  const staff = isStaff(chat);
  const total = poll.counts.reduce((sum, n) => sum + n, 0);
  const voted = poll.mine.length > 0;

  const run = (body: () => Promise<unknown>) => {
    setBusy(true);
    body()
      .catch((e) => toast(errorOf(s, e)))
      .finally(() => setBusy(false));
  };

  const tap = (index: number) => {
    if (poll.closed || busy) return;
    if (poll.multiple) {
      setPicked((current) => (current.includes(index) ? current.filter((i) => i !== index) : [...current, index]));
      return;
    }
    const next = poll.mine.includes(index) ? [] : [index];
    setPicked(next);
    run(() => votePoll(chat.id, poll.id, next));
  };

  const changed = poll.multiple && [...picked].sort().join(",") !== [...poll.mine].sort().join(",");

  return (
    <div className="card mt12">
      <div className="semibold" style={{ overflowWrap: "anywhere" }}>{maskIf(data.hideProfanity, poll.question)}</div>
      <div className="muted xsmall mt2">
        {[poll.author, s.pollVotes(poll.voters), poll.closed ? s.pollClosed : poll.multiple ? s.pollMultiple : ""].filter(Boolean).join(" · ")}
      </div>
      {poll.options.map((option, index) => {
        const count = poll.counts[index] ?? 0;
        const share = total ? Math.round((count / total) * 100) : 0;
        const on = picked.includes(index);
        return (
          <button key={index} className={`poll-option${on ? " on" : ""}`} disabled={poll.closed} onClick={() => tap(index)}>
            {(voted || poll.closed) && <span className="fill" style={{ width: `${share}%` }} />}
            {on && <Icon name="check" size={16} />}
            <span className="grow" style={{ overflowWrap: "anywhere" }}>{maskIf(data.hideProfanity, option)}</span>
            {(voted || poll.closed) && <span className="xsmall semibold">{`${share}%`}</span>}
          </button>
        );
      })}
      <div className="row mt8" style={{ gap: 4, flexWrap: "wrap" }}>
        {changed && !poll.closed && (
          <button className="tbtn" disabled={busy} onClick={() => run(() => votePoll(chat.id, poll.id, picked))}>{s.vote}</button>
        )}
        <span className="grow" />
        {staff && !poll.closed && (
          <button className="tbtn muted" disabled={busy} onClick={() => run(() => closePoll(chat.id, poll.id, false))}>{s.pollClose}</button>
        )}
        {staff && (
          <button className="tbtn danger" disabled={busy} onClick={() => run(() => closePoll(chat.id, poll.id, true))}>{s.pollDelete}</button>
        )}
      </div>
    </div>
  );
}

function NewPollForm({ chat, onDone }: { chat: ChatInfo; onDone: () => void }) {
  const { s } = useShell();
  const [question, setQuestion] = useState("");
  const [options, setOptions] = useState(["", ""]);
  const [multiple, setMultiple] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const filled = options.map((o) => o.trim()).filter(Boolean);

  const submit = async () => {
    if (!question.trim() || filled.length < 2) return setError(s.pollNeedOptions);
    setBusy(true);
    setError(null);
    try {
      await createPoll(chat.id, question.trim(), filled, multiple);
      onDone();
    } catch (e) {
      setError(errorOf(s, e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card mt12">
      <label className="field">
        <span>{s.pollQuestion}</span>
        <input className="input" value={question} maxLength={200} onChange={(e) => setQuestion(e.target.value)} autoFocus />
      </label>
      {options.map((option, index) => (
        <label key={index} className="field mt8">
          <span>{s.pollOption(index + 1)}</span>
          <input
            className="input"
            value={option}
            maxLength={100}
            onChange={(e) => setOptions((current) => current.map((o, i) => (i === index ? e.target.value : o)))}
          />
        </label>
      ))}
      {options.length < 8 && (
        <button className="tbtn flush mt4" onClick={() => setOptions([...options, ""])}>{s.addOption}</button>
      )}
      <label className="checkbox mt4">
        <input type="checkbox" checked={multiple} onChange={(e) => setMultiple(e.target.checked)} />
        <span>{s.pollMultiple}</span>
      </label>
      {error && <div className="red small mt4">{error}</div>}
      <div className="row mt8" style={{ justifyContent: "flex-end", gap: 4 }}>
        <button className="tbtn muted" onClick={onDone}>{s.cancel}</button>
        <button className="tbtn" disabled={busy} onClick={submit}>{s.pollCreate}</button>
      </div>
    </div>
  );
}

export function PollsSheet({ chat, onClose }: { chat: ChatInfo; onClose: () => void }) {
  const { s } = useShell();
  useChatVersion();
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const polls = cachedPolls(chat.id);

  useEffect(() => {
    refreshPolls(chat.id)
      .then(() => setError(null))
      .catch((e) => setError(errorOf(s, e)));
  }, [chat.id, s]);

  return (
    <Sheet onClose={onClose}>
      <div className="row">
        <h2 className="h2 grow" style={{ fontSize: 20 }}>{s.polls}</h2>
        {isStaff(chat) && !creating && <button className="tbtn" onClick={() => setCreating(true)}>{s.newPoll}</button>}
      </div>
      {error && <div className="red small mt8">{error}</div>}
      {creating && <NewPollForm chat={chat} onDone={() => setCreating(false)} />}
      {polls.length === 0 && !creating && <div className="muted small mt8">{s.noPolls}</div>}
      {polls.map((poll) => (
        <PollCard key={`${poll.id}-${poll.mine.join(",")}`} chat={chat} poll={poll} />
      ))}
    </Sheet>
  );
}

function dueText(s: Copy, task: GroupTask): string {
  if (!task.dueDate) return s.noDeadline;
  const base = s.formatLongDateCap(task.dueDate);
  return task.dueTime ? `${base} · ${task.dueTime}` : base;
}

export const groupTodoId = (task: GroupTask) => `group-${task.id}`;

export function GroupTaskRow({ task, chatTitle }: { task: GroupTask; chatTitle?: string }) {
  const { s, data, toast, today } = useShell();
  const chat = cachedChats().find((c) => c.id === task.chatId);
  const added = data.todos.some((t) => t.id === groupTodoId(task));
  const overdue = task.dueDate != null && task.dueDate < today;
  return (
    <div className="card mt8" style={{ padding: "10px 14px" }}>
      <div className="semibold" style={{ overflowWrap: "anywhere" }}>{maskIf(data.hideProfanity, task.title)}</div>
      <div className={`xsmall mt2 ${overdue ? "red" : "muted"}`}>{dueText(s, task)}</div>
      {task.note && <div className="small mt4 pre" style={{ overflowWrap: "anywhere" }}>{maskIf(data.hideProfanity, task.note)}</div>}
      <div className="muted xsmall mt4">{[task.author, chatTitle ? s.fromChat(chatTitle) : ""].filter(Boolean).join(" · ")}</div>
      <div className="row mt4" style={{ gap: 4, marginLeft: -8 }}>
        {added ? (
          <span className="muted xsmall" style={{ padding: "0 8px" }}>{s.inMine}</span>
        ) : (
          <button
            className="tbtn"
            onClick={() => {
              actions.upsertTodo({
                id: groupTodoId(task),
                title: task.title,
                deadlineDate: task.dueDate,
                deadlineTime: task.dueDate ? task.dueTime : null,
                lessonKey: null,
                lessonLabel: null,
                done: false,
                remind: "",
              });
              toast(s.addedToMine);
            }}
          >
            {s.addToMine}
          </button>
        )}
        <span className="grow" />
        {chat && isStaff(chat) && (
          <button className="icon-btn muted" aria-label={s.delete} onClick={() => deleteGroupTask(task.id).catch((e) => toast(errorOf(s, e)))}>
            <Icon name="delete" size={18} />
          </button>
        )}
      </div>
    </div>
  );
}

function NewTaskForm({ chat, onDone }: { chat: ChatInfo; onDone: () => void }) {
  const { s, today } = useShell();
  const [title, setTitle] = useState("");
  const [note, setNote] = useState("");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!title.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await addGroupTask(chat.id, title.trim(), note.trim(), date || null, date && time ? time : null);
      onDone();
    } catch (e) {
      setError(errorOf(s, e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card mt12">
      <label className="field">
        <span>{s.groupTaskTitle}</span>
        <input className="input" value={title} maxLength={200} onChange={(e) => setTitle(e.target.value)} autoFocus />
      </label>
      <label className="field mt8">
        <span>{s.groupTaskNote}</span>
        <textarea className="input" rows={2} value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} />
      </label>
      <div className="row mt8" style={{ gap: 8 }}>
        <label className="field grow">
          <span>{s.dueField}</span>
          <input className="input" type="date" value={date} min={today} onChange={(e) => setDate(e.target.value)} />
        </label>
        {date && (
          <label className="field" style={{ width: 120 }}>
            <span>{s.pickTime}</span>
            <input className="input" type="time" value={time} onChange={(e) => setTime(e.target.value)} />
          </label>
        )}
      </div>
      {error && <div className="red small mt4">{error}</div>}
      <div className="row mt8" style={{ justifyContent: "flex-end", gap: 4 }}>
        <button className="tbtn muted" onClick={onDone}>{s.cancel}</button>
        <button className="tbtn" disabled={busy || !title.trim()} onClick={submit}>{s.add}</button>
      </div>
    </div>
  );
}

export function GroupTasksSheet({ chat, onClose }: { chat: ChatInfo; onClose: () => void }) {
  const { s } = useShell();
  useChatVersion();
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const tasks = cachedGroupTasks().filter((t) => t.chatId === chat.id);

  useEffect(() => {
    refreshGroupTasks()
      .then(() => setError(null))
      .catch((e) => setError(errorOf(s, e)));
  }, [s]);

  return (
    <Sheet onClose={onClose}>
      <div className="row">
        <h2 className="h2 grow" style={{ fontSize: 20 }}>{s.groupTasks}</h2>
        {isStaff(chat) && !creating && <button className="tbtn" onClick={() => setCreating(true)}>{s.newGroupTask}</button>}
      </div>
      <div className="muted small mt4">{s.groupTasksHint}</div>
      {error && <div className="red small mt8">{error}</div>}
      {creating && <NewTaskForm chat={chat} onDone={() => setCreating(false)} />}
      {tasks.length === 0 && !creating && <div className="muted small mt8">{s.noGroupTasks}</div>}
      {tasks.map((task) => (
        <GroupTaskRow key={task.id} task={task} />
      ))}
    </Sheet>
  );
}
