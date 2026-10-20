import { sql } from "./db";
import { chatModerationEnabled, supabaseAdmin } from "./supabase";

type Row = Record<string, unknown>;

export interface RepRequest {
  id: number;
  groupId: string;
  groupName: string;
  comment: string;
  status: "pending" | "approved" | "rejected" | "revoked";
  createdAt: string;
  decidedAt: string | null;
}

export function toRepRequest(row: Row): RepRequest {
  return {
    id: Number(row.id),
    groupId: String(row.group_id),
    groupName: String(row.group_name ?? ""),
    comment: String(row.comment ?? ""),
    status: String(row.status) as RepRequest["status"],
    createdAt: new Date(row.created_at as string).toISOString(),
    decidedAt: row.decided_at ? new Date(row.decided_at as string).toISOString() : null,
  };
}

export async function latestRepRequest(userId: string): Promise<RepRequest | null> {
  const rows = await sql()`
    SELECT * FROM rep_requests WHERE user_id = ${userId}::uuid ORDER BY created_at DESC LIMIT 1`;
  return rows.length ? toRepRequest(rows[0]) : null;
}

/** Mirrors an approved class rep into Supabase, where it grants helper rights in chats of that group. */
export async function syncClassRep(userId: string, groupId: string | null, name = ""): Promise<void> {
  if (!chatModerationEnabled()) return;
  if (groupId == null) {
    await supabaseAdmin(`class_reps?user_id=eq.${userId}`, { method: "DELETE" });
    return;
  }
  await supabaseAdmin("class_reps?on_conflict=user_id", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
    body: JSON.stringify({ user_id: userId, group_id: groupId, name }),
  });
}
