import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const TEACHER_NAME = /([А-ЯЁA-Z][а-яёa-z-]+)((?:\s+[А-ЯЁA-Z]?\.){1,2})/g;

// Same rules as splitTeachers() in src/lib/schedule.ts and Android StudentExtras.kt.
function splitTeachers(raw) {
  const found = [...raw.matchAll(TEACHER_NAME)].map((match) => {
    const initials = match[2].split(".").map((part) => part.trim()).filter(Boolean);
    return [match[1], ...initials.map((letter) => `${letter}.`)].join(" ");
  });
  if (found.length) return found;
  const trimmed = raw.trim();
  return trimmed ? [trimmed] : [];
}

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const schedule = JSON.parse(readFileSync(join(root, "public", "schedule.json"), "utf8"));
const names = new Set();
for (const faculty of schedule.faculties) {
  for (const group of faculty.groups) {
    for (const lesson of group.lessons) {
      for (const name of splitTeachers(String(lesson.teacher ?? ""))) names.add(name);
    }
  }
}
const out = join(root, "src", "data", "teachers.json");
mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, JSON.stringify([...names].sort(), null, 0) + "\n");
console.log(`teachers.json: ${names.size} names`);
