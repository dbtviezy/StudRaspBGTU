"use client";

import { useEffect, useState } from "react";

export interface StoredFile {
  name: string;
  size: number;
  type: string;
  added: number;
  blob: Blob;
}

export type FileMeta = Omit<StoredFile, "blob">;

let opening: Promise<IDBDatabase> | null = null;

function db(): Promise<IDBDatabase> {
  opening ??= new Promise((resolve, reject) => {
    const request = indexedDB.open("studrasp", 1);
    request.onupgradeneeded = () => {
      const base = request.result;
      if (!base.objectStoreNames.contains("kv")) base.createObjectStore("kv");
      if (!base.objectStoreNames.contains("files")) base.createObjectStore("files", { keyPath: "name" });
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => {
      opening = null;
      reject(request.error);
    };
  });
  return opening;
}

async function run<T>(store: string, mode: IDBTransactionMode, body: (s: IDBObjectStore) => IDBRequest<T>): Promise<T> {
  const base = await db();
  return new Promise((resolve, reject) => {
    const tx = base.transaction(store, mode);
    const request = body(tx.objectStore(store));
    tx.oncomplete = () => resolve(request.result);
    tx.onerror = () => reject(tx.error);
    tx.onabort = () => reject(tx.error);
  });
}

const listeners = new Set<() => void>();
const changed = () => listeners.forEach((listener) => listener());

export async function getAvatar(): Promise<Blob | null> {
  try {
    return ((await run("kv", "readonly", (s) => s.get("avatar"))) as Blob | undefined) ?? null;
  } catch {
    return null;
  }
}

export async function setAvatar(blob: Blob | null): Promise<void> {
  await run<unknown>("kv", "readwrite", (s) => (blob ? s.put(blob, "avatar") : s.delete("avatar")) as IDBRequest<unknown>);
  changed();
}

export async function listFiles(): Promise<FileMeta[]> {
  try {
    const all = (await run("files", "readonly", (s) => s.getAll())) as StoredFile[];
    return all.map(({ name, size, type, added }) => ({ name, size, type, added })).sort((a, b) => b.added - a.added);
  } catch {
    return [];
  }
}

export async function getFile(name: string): Promise<StoredFile | null> {
  return ((await run("files", "readonly", (s) => s.get(name))) as StoredFile | undefined) ?? null;
}

function safeName(raw: string): string {
  return raw.replace(/[\\/:*?"<>|]/g, "_").trim().slice(0, 80) || "file";
}

export async function addFile(file: File): Promise<string> {
  const existing = new Set((await listFiles()).map((f) => f.name));
  const safe = safeName(file.name);
  const dot = safe.lastIndexOf(".");
  const base = dot > 0 ? safe.slice(0, dot) : safe;
  const ext = dot > 0 ? safe.slice(dot) : "";
  let name = safe;
  for (let index = 2; existing.has(name); index++) name = `${base} (${index})${ext}`;
  const blob = new Blob([await file.arrayBuffer()], { type: file.type || "application/octet-stream" });
  await run("files", "readwrite", (s) => s.put({ name, size: file.size, type: blob.type, added: Date.now(), blob } satisfies StoredFile));
  changed();
  return name;
}

export async function deleteFile(name: string): Promise<void> {
  await run("files", "readwrite", (s) => s.delete(name));
  changed();
}

export async function clearFiles(): Promise<void> {
  await run("files", "readwrite", (s) => s.clear());
  changed();
}

export async function openFile(name: string): Promise<void> {
  const stored = await getFile(name);
  if (!stored) return;
  const url = URL.createObjectURL(stored.blob);
  const opened = window.open(url, "_blank");
  if (!opened) {
    const link = document.createElement("a");
    link.href = url;
    link.download = stored.name;
    link.click();
  }
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export function useLibrary(): FileMeta[] {
  const [files, setFiles] = useState<FileMeta[]>([]);
  useEffect(() => {
    let alive = true;
    const refresh = () => listFiles().then((list) => alive && setFiles(list));
    refresh();
    listeners.add(refresh);
    return () => {
      alive = false;
      listeners.delete(refresh);
    };
  }, []);
  return files;
}

export function useAvatar(): string | null {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    let current: string | null = null;
    const refresh = () =>
      getAvatar().then((blob) => {
        if (!alive) return;
        if (current) URL.revokeObjectURL(current);
        current = blob ? URL.createObjectURL(blob) : null;
        setUrl(current);
      });
    refresh();
    listeners.add(refresh);
    return () => {
      alive = false;
      listeners.delete(refresh);
      if (current) URL.revokeObjectURL(current);
    };
  }, []);
  return url;
}
