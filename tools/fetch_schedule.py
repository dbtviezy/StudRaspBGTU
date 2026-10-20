# -*- coding: utf-8 -*-
"""Export full-time (очная) BGTU schedule into the Android app asset."""

import json
import re
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from html.parser import HTMLParser
from pathlib import Path

URL = "https://www.tu-bryansk.ru/education/schedule/schedule.ajax.php"
PAGE = "https://www.tu-bryansk.ru/education/schedule/"
PERIOD = "2026-2027_1_1"
PERIOD_LABEL = "2026-2027 · 1 семестр"
FORM = "очная"
OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "schedule.json"

FACULTIES = [
    ("Механико-технологический факультет", "МТФ"),
    ("Политехнический колледж", "ПК"),
    ("Учебно-научный институт транспорта", "УНИТ"),
    ("Учебно-научный технологический институт", "УНТИ"),
    ("Факультет информационных технологий", "ФИТ"),
    ("Факультет отраслевой и цифровой экономики", "ФОЦЭ"),
    ("Факультет энергетики и электроники", "ФЭЭ"),
]
LEVELS = ["колледж", "магистр", "аспирант", "специалист", "бакалавр"]

DAY_INDEX = {
    "Понедельник": 1,
    "Вторник": 2,
    "Среда": 3,
    "Четверг": 4,
    "Пятница": 5,
    "Суббота": 6,
    "Воскресенье": 7,
}

CTX = ssl.create_default_context()


def post(fields, retries=4):
    data = urllib.parse.urlencode(fields).encode("utf-8")
    last = None
    for attempt in range(retries):
        req = urllib.request.Request(
            URL,
            data=data,
            headers={
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) BGTUScheduleExport/1.0",
                "Referer": PAGE,
                "Origin": "https://www.tu-bryansk.ru",
                "X-Requested-With": "XMLHttpRequest",
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=40, context=CTX) as resp:
                return resp.read().decode("utf-8", "replace")
        except Exception as exc:  # noqa: BLE001
            last = exc
            time.sleep(0.6 * (attempt + 1))
    raise RuntimeError(f"request failed: {fields.get('namedata')} {fields.get('group', '')}: {last}")


class CellParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.rows = []
        self.row = None
        self.cell = None
        self.in_type = False

    def handle_starttag(self, tag, attrs):
        attr = {k: v for k, v in attrs}
        if tag == "tr":
            self.row = []
        elif tag == "td" and self.row is not None:
            self.cell = {
                "class": attr.get("class", ""),
                "rowspan": int(attr.get("rowspan") or 1),
                "text": [],
                "kind": [],
            }
            self.in_type = False
        elif tag == "span" and self.cell is not None and "schtype" in attr.get("class", ""):
            self.in_type = True
        elif tag == "br" and self.cell is not None:
            self.cell["text"].append(" ")

    def handle_endtag(self, tag):
        if tag == "span":
            self.in_type = False
        elif tag == "td" and self.cell is not None and self.row is not None:
            self.row.append(self.cell)
            self.cell = None
        elif tag == "tr" and self.row is not None:
            if self.row:
                self.rows.append(self.row)
            self.row = None

    def handle_data(self, data):
        if self.cell is None:
            return
        if self.in_type:
            self.cell["kind"].append(data)
        else:
            self.cell["text"].append(data)


def clean(value):
    return re.sub(r"\s+", " ", value or "").strip()


def cell_text(cell, key):
    return clean("".join(cell.get(key, [])))


def parse_groups(html):
    names = re.findall(r'<option[^>]*value="([^"]+)"', html)
    return [n for n in names if n.strip()]


def parse_slot(row):
    name = teacher = room = kind = ""
    for cell in row:
        cls = cell["class"]
        if "schname" in cls:
            name = cell_text(cell, "text")
            kind = cell_text(cell, "kind")
        elif "schteacher" in cls:
            teacher = cell_text(cell, "text")
        elif "schclass" in cls:
            room = cell_text(cell, "text")
    return name, kind, teacher, room


def split_time(raw):
    text = clean(raw).replace("—", "-").replace("–", "-")
    match = re.search(r"(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})", text)
    if not match:
        return text, "", ""
    start, end = match.group(1), match.group(2)
    if len(start) == 4:
        start = "0" + start
    if len(end) == 4:
        end = "0" + end
    return f"{start}–{end}", start, end


def parse_schedule(html):
    parser = CellParser()
    parser.feed(html)
    lessons = []
    day = None
    rows = parser.rows
    i = 0
    while i < len(rows):
        row = rows[i]
        day_cell = next((c for c in row if "daeweek" in c["class"]), None)
        if day_cell:
            day = cell_text(day_cell, "text")
            i += 1
            continue
        time_cell = next((c for c in row if "schtime" in c["class"]), None)
        if time_cell is None or not day:
            i += 1
            continue
        span = max(1, time_cell["rowspan"])
        label, start, end = split_time(cell_text(time_cell, "text"))
        weeks = ["all"] if span == 1 else ["num", "den"]
        for offset in range(span):
            if i + offset >= len(rows):
                break
            subject, kind, teacher, room = parse_slot(rows[i + offset])
            if not subject:
                continue
            lessons.append(
                {
                    "day": DAY_INDEX.get(day, 0),
                    "dayName": day,
                    "time": label,
                    "start": start,
                    "end": end,
                    "subject": subject,
                    "type": kind,
                    "teacher": teacher,
                    "room": room,
                    "week": weeks[offset] if offset < len(weeks) else "all",
                }
            )
        i += span
    return lessons


def year_of(group):
    match = re.search(r"-(\d{2})-", group)
    if not match:
        return None
    yy = int(match.group(1))
    return 2000 + yy


def main():
    print("loading directories...")
    faculty_groups = {}
    for name, short in FACULTIES:
        html = post(
            {
                "namedata": "group",
                "faculty": name,
                "level": "",
                "period": PERIOD,
                "form": FORM,
            }
        )
        groups = parse_groups(html)
        faculty_groups[name] = groups
        print(f"  {short}: {len(groups)}")

    level_of = {}
    for level in LEVELS:
        html = post(
            {
                "namedata": "group",
                "faculty": "",
                "level": level,
                "period": PERIOD,
                "form": FORM,
            }
        )
        groups = parse_groups(html)
        print(f"  level {level}: {len(groups)}")
        for group in groups:
            level_of.setdefault(group, level)

    jobs = []
    for name, _short in FACULTIES:
        for group in faculty_groups.get(name, []):
            jobs.append((name, group))

    print(f"fetching {len(jobs)} schedules...")
    results = {}

    def load(item):
        faculty, group = item
        html = post(
            {
                "namedata": "schedule",
                "group": group,
                "period": PERIOD,
                "form": FORM,
            }
        )
        return faculty, group, parse_schedule(html)

    done = 0
    with ThreadPoolExecutor(max_workers=6) as pool:
        futures = [pool.submit(load, item) for item in jobs]
        for future in as_completed(futures):
            faculty, group, lessons = future.result()
            results[(faculty, group)] = lessons
            done += 1
            if done % 25 == 0 or done == len(jobs):
                print(f"  {done}/{len(jobs)}")

    faculties = []
    lesson_count = 0
    empty = 0
    for name, short in FACULTIES:
        groups = []
        for group in faculty_groups.get(name, []):
            lessons = results.get((name, group), [])
            lesson_count += len(lessons)
            if not lessons:
                empty += 1
            groups.append(
                {
                    "id": group,
                    "level": level_of.get(group, ""),
                    "year": year_of(group),
                    "lessons": lessons,
                }
            )
        if not groups:
            continue
        faculties.append({"name": name, "short": short, "groups": groups})

    payload = {
        "source": PAGE,
        "form": FORM,
        "periodId": PERIOD,
        "periodLabel": PERIOD_LABEL,
        "fetchedAt": "2026-10-11",
        "semesterStart": "2026-09-01",
        "weekLegend": {
            "num": "Числитель — верхняя строка на сайте, нечётные учебные недели",
            "den": "Знаменатель — нижняя строка на сайте, чётные учебные недели",
            "all": "Каждую неделю",
        },
        "faculties": faculties,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"groups={len(jobs)} lessons={lesson_count} empty={empty} bytes={OUT.stat().st_size}")
    print(OUT)


if __name__ == "__main__":
    main()
