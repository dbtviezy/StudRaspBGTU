import { addDays, dow, type ISODate, parts } from "./dates";

const ru = {
  tabHome: "Обзор",
  tabSchedule: "Расписание",
  tabGroups: "Группы",
  tabCalendar: "Календарь",
  tabProfile: "Профиль",
  appName: "СтудРаспБГТУ",
  brand: "ФГБОУ БГТУ Брянск",
  university: "Брянский государственный технический университет",
  fullTime: "Очная форма",
  back: "Назад",
  chooseFaculty: "Выберите факультет",
  facultyThenGroup: "Сначала факультет, потом группа.",
  chooseGroup: "Выберите группу",
  name: "Имя",
  nameHint: "Как к вам обращаться",
  findGroup: "Найти группу",
  groupMissing: "В этом факультете такой группы нет",
  openSchedule: "Открыть расписание",
  toFaculties: "К факультетам",
  groupsTitle: "Группы",
  groupsHint: "Сначала факультет, внутри него — группы",
  findGroupOrFaculty: "Найти группу или факультет",
  nothingFound: "Ничего не нашлось",
  theme: "Тема",
  reminders: "Напоминания",
  studentFallback: "Студент БГТУ",
  today: "Сегодня",
  tomorrow: "Завтра",
  upcomingLessons: "Ближайшие пары",
  freeWindow: "Свободное окно",
  freeWindowText: "На ближайшие дни по текущей неделе пар нет.",
  schedule: "Расписание",
  filterAuto: "Авто",
  filterAll: "Все",
  numerator: "числитель",
  denominator: "знаменатель",
  calendar: "Календарь",
  previousMonth: "Предыдущий месяц",
  nextMonth: "Следующий месяц",
  noLessonsThatDay: "В этот день пар нет",
  profile: "Профиль",
  changePhoto: "Сменить фото",
  removePhoto: "Убрать фото",
  photo: "Фото профиля",
  group: "Группа",
  faculty: "Факультет",
  level: "Уровень",
  cohort: "Набор",
  period: "Период",
  form: "Форма",
  fullTimeForm: "Очная",
  files: "Файлы",
  filesHint: "Копии документов остаются в приложении на этом телефоне",
  weekHelpTitle: "Как читать недели",
  weekHelp:
    "На сайте верхняя строка пары — числитель, нижняя — знаменатель. Нечётные учебные недели от 1 сентября идут числителем, чётные — знаменателем. Занятия без второй строки проходят каждую неделю.",
  fetchedLine: "Данные с tu-bryansk.ru · 11 октября 2026",
  openSite: "Открыть сайт расписания",
  contact: "Связь",
  changeGroup: "Сменить группу",
  resetData: "Сбросить данные",
  resetTitle: "Сбросить данные?",
  resetText:
    "Удалятся имя, группы, фото, заметки, пропуски, дела, избранное и изменения в расписании. Тема и язык останутся. Снова откроется выбор факультета и группы.",
  reset: "Сбросить",
  cancel: "Отмена",
  close: "Закрыть",
  remindersHint: "Собраны из вашего расписания",
  noUpcoming: "Ближайших пар нет",
  filesBody: "Файлы копируются в хранилище приложения и остаются на этом устройстве. В интернет они не отправляются.",
  copying: "Копируем…",
  addFiles: "Добавить файлы",
  copyingFiles: "Копируем файлы…",
  empty: "Пока пусто",
  open: "Открыть",
  toLesson: "К паре",
  delete: "Удалить",
  attachToLesson: "Прикрепить к паре",
  subjectOrDay: "Предмет или день",
  attached: "есть",
  done: "Готово",
  copyFailed: "Не удалось скопировать файл.",
  ok: "Хорошо",
  time: "Время",
  pair: "Пара",
  type: "Тип",
  room: "Аудитория",
  teacher: "Преподаватель",
  day: "День",
  nothingAttached: "К этой паре пока ничего не прикреплено",
  attachFile: "Прикрепить файл",
  note: "Заметка",
  noteHint: "Видна под парой в этот день",
  noLessons: "Пар нет",
  noLessonsHint: "В этот день по выбранной неделе занятий нет.",
  hasFile: "Есть файл",
  cropTitle: "Кадрирование",
  cropHint: "Сдвиньте фото и измените масштаб. В профиль попадёт квадрат.",
  loadFailedTitle: "Не удалось открыть расписание",
  loadFailedText: "Файл с занятиями не загрузился. Проверьте интернет и откройте приложение снова.",
  retry: "Повторить",
  language: "Язык",
  tasks: "Дела",
  tasksHint: "Дедлайн можно поставить, пару прикрепить по желанию.",
  tasksEmpty: "Пока нет дел",
  expandTasks: "Развернуть",
  collapseTasks: "Свернуть",
  addTask: "Добавить дело",
  newTask: "Новое дело",
  editTask: "Изменить дело",
  taskTitle: "Что сделать",
  taskTitleHint: "Например, сдать отчёт",
  deadline: "Дедлайн",
  noDeadline: "Без дедлайна",
  pickDate: "Выбрать дату",
  clearDeadline: "Убрать дедлайн",
  timeOptional: "Время, если нужно",
  timeHint: "18:00",
  badTime: "Время в формате 18:00",
  attachLessonOptional: "Пара, если нужно",
  notAttached: "Без пары",
  removeLesson: "Убрать пару",
  chooseLesson: "Выберите пару",
  save: "Сохранить",
  overdue: "Просрочено",
  completed: "Сделано",
  openTasks: "Открытые",
  tasksForLesson: "Дела к этой паре",
  noTasksForLesson: "К этой паре дел нет",
  hasTask: "Есть дело",
  allTasks: "Все дела",
  lessonFallback: "Пара",
  liveInClass: "Сейчас идёт",
  liveSoon: "До пары",
  liveWindow: "Окно",
  liveDone: "Сегодня пары закончились",
  liveNone: "Сегодня пар нет",
  liveNext: "Дальше",
  exportWeek: "В календарь",
  exportEmpty: "На этой неделе добавлять нечего",
  shareDay: "Поделиться",
  copied: "Текст скопирован",
  teachersMode: "Преподаватели",
  groupsMode: "Группы",
  findTeacher: "Фамилия преподавателя",
  teacherHint: "Введите хотя бы две буквы фамилии",
  favoriteTeachers: "Избранные преподаватели",
  noTeacherSoon: "На ближайшие дни пар нет",
  teacherSoon: "Все пары на ближайшие две недели",
  addFavorite: "В избранное",
  removeFavorite: "Убрать из избранных",
  wasAbsent: "Не был",
  clearAbsence: "Убрать пропуск",
  missedBadge: "Пропуск",
  absences: "Пропуски",
  absencesEmpty: "Пропусков пока нет",
  hideLesson: "Не хожу",
  attendLesson: "Снова хожу",
  hiddenTitle: "Скрытые пары",
  hiddenEmpty: "Скрытых пар нет",
  restore: "Вернуть",
  subjectNote: "Заметка к предмету",
  subjectNoteHint: "Видна на каждой паре этого предмета",
  subjectNotesTitle: "Заметки к предметам",
  subjectNotesEmpty: "Заметок к предметам пока нет",
  bothWeeks: "Обе",
  numeratorColumn: "Числитель",
  denominatorColumn: "Знаменатель",
  backupSave: "Сохранить копию",
  backupLoad: "Загрузить копию",
  backupHint: "Имя, группа, заметки, дела и список файлов сохраняются в один файл. Его можно открыть и в Android-приложении.",
  backupDone: "Копия сохранена",
  backupRestored: "Копия загружена",
  backupFailed: "Не удалось прочитать файл",
  holidayQuiet: "Выходной. Пар в этот день нет.",
  telegram: "Telegram",
  secondGroup: "Вторая группа",
  secondGroupHint: "Одногруппник, электив или сосед по комнате. На обзоре видно, когда вы оба свободны.",
  pickSecond: "Выбрать",
  clearSecond: "Убрать",
  bothFreeTitle: "Оба свободны",
  noSharedWindow: "В ближайшие дни общего окна нет",
  todayChange: "Сегодня иначе",
  todayOnly: "Только на сегодня. Завтра снова расписание с сайта.",
  todayRoom: "Аудитория на сегодня",
  roomExample: "Например, 312",
  saveRoom: "Сохранить аудиторию",
  cancelToday: "Отменить пару на сегодня",
  goneToday: "Сегодня нет",
  officialAgain: "Как на сайте",
  setLimit: "Порог",
  clearLimit: "Убрать порог",
  limitUsed: "Запас кончился",
  welcomeTitle: "Привет! Это СтудРаспБГТУ",
  welcomeText: "Просматривай расписание занятий без ошибок",
  offlinePageTitle: "Работает без интернета",
  offlinePageText: "Добавьте приложение на экран «Домой» — расписание откроется даже без сети",
  searchPageTitle: "Быстрый поиск по университету",
  searchPageText: "Узнавайте информацию о группах, преподавателях и свободных аудиториях",
  nextPage: "Далее",
  begin: "Начать",
  themeLight: "Светлая",
  themeDark: "Тёмная",
  langRu: "Русский",
  langEn: "English",
  localData: "Все данные хранятся только на этом устройстве. У нас нет к ним доступа. Оценки преподавателей отправляются анонимно.",
  updatesInTelegram: "Обновления приложения выходят в Telegram.",
  installTitle: "Установите приложение",
  installText: "Нажмите «Поделиться» внизу экрана, затем «На экран Домой».",
  rating: "Оценка",
  rateTeacher: "Оценить",
  editReview: "Изменить отзыв",
  deleteReview: "Удалить отзыв",
  overall: "Общая оценка",
  delivery: "Подача",
  strictness: "Строгость",
  loyalty: "Лояльность",
  reviewText: "Отзыв",
  reviewHint: "Что стоит знать о преподавателе (необязательно)",
  reviews: "Отзывы",
  noReviews: "Отзывов пока нет",
  noRatings: "Оценок пока нет",
  yourReview: "Ваш отзыв",
  send: "Отправить",
  ratingAnon: "Отзыв анонимный. С одного устройства — один отзыв, его можно изменить.",
  ratingOffline: "Нет связи с сервером. Оценки появятся, когда интернет вернётся.",
  ratingFailed: "Не удалось отправить. Попробуйте позже.",
  rateLimited: "Слишком много изменений. Попробуйте через час.",
  textTooLong: "Отзыв длиннее 300 символов",
  unknownTeacher: "Этого преподавателя нет в расписании",
  pickOverall: "Поставьте все четыре оценки",
  weekdays: ["понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"],
  monthsGenitive: ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"],
  monthsNominative: ["Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"],
  dayShort: ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"],
};

type Strings = typeof ru;

const en: Strings = {
  tabHome: "Home",
  tabSchedule: "Schedule",
  tabGroups: "Groups",
  tabCalendar: "Calendar",
  tabProfile: "Profile",
  appName: "StudRaspBGTU",
  brand: "BSTU Bryansk",
  university: "Bryansk State Technical University",
  fullTime: "Full-time",
  back: "Back",
  chooseFaculty: "Choose a faculty",
  facultyThenGroup: "Faculty first, then a group.",
  chooseGroup: "Choose a group",
  name: "Name",
  nameHint: "What should we call you",
  findGroup: "Find a group",
  groupMissing: "This faculty has no such group",
  openSchedule: "Open schedule",
  toFaculties: "Back to faculties",
  groupsTitle: "Groups",
  groupsHint: "Faculty first, then the groups inside it",
  findGroupOrFaculty: "Find a group or faculty",
  nothingFound: "Nothing found",
  theme: "Theme",
  reminders: "Reminders",
  studentFallback: "BSTU student",
  today: "Today",
  tomorrow: "Tomorrow",
  upcomingLessons: "Coming up",
  freeWindow: "Free window",
  freeWindowText: "No classes in the next few days of this week.",
  schedule: "Schedule",
  filterAuto: "Auto",
  filterAll: "All",
  numerator: "Numerator",
  denominator: "Denominator",
  calendar: "Calendar",
  previousMonth: "Previous month",
  nextMonth: "Next month",
  noLessonsThatDay: "No classes this day",
  profile: "Profile",
  changePhoto: "Change photo",
  removePhoto: "Remove photo",
  photo: "Profile photo",
  group: "Group",
  faculty: "Faculty",
  level: "Level",
  cohort: "Intake",
  period: "Period",
  form: "Form",
  fullTimeForm: "Full-time",
  files: "Files",
  filesHint: "Document copies stay in the app on this phone",
  weekHelpTitle: "How weeks work",
  weekHelp:
    "On the website the upper row of a class is the numerator, the lower row is the denominator. Odd academic weeks from 1 September are numerator weeks, even weeks are denominator weeks. A class with no second row runs every week.",
  fetchedLine: "Data from tu-bryansk.ru · 11 October 2026",
  openSite: "Open the schedule website",
  contact: "Contact",
  changeGroup: "Change group",
  resetData: "Reset data",
  resetTitle: "Reset data?",
  resetText:
    "Your name, groups, photo, notes, absences, tasks, favorites and schedule changes will be removed. Theme and language stay. Faculty and group selection will open again.",
  reset: "Reset",
  cancel: "Cancel",
  close: "Close",
  remindersHint: "Taken from your schedule",
  noUpcoming: "No upcoming classes",
  filesBody: "Files are copied into the app storage and stay on this device. They are not uploaded.",
  copying: "Copying…",
  addFiles: "Add files",
  copyingFiles: "Copying files…",
  empty: "Nothing here yet",
  open: "Open",
  toLesson: "To a class",
  delete: "Delete",
  attachToLesson: "Attach to a class",
  subjectOrDay: "Subject or day",
  attached: "added",
  done: "Done",
  copyFailed: "Could not copy the file.",
  ok: "OK",
  time: "Time",
  pair: "Class",
  type: "Type",
  room: "Room",
  teacher: "Teacher",
  day: "Day",
  nothingAttached: "Nothing is attached to this class yet",
  attachFile: "Attach a file",
  note: "Note",
  noteHint: "Shown under the class on this day",
  noLessons: "No classes",
  noLessonsHint: "Nothing is scheduled this day for the selected week.",
  hasFile: "Has a file",
  cropTitle: "Crop",
  cropHint: "Move the photo and change the zoom. A square goes to your profile.",
  loadFailedTitle: "Could not open the schedule",
  loadFailedText: "The class file did not load. Check your connection and open the app again.",
  retry: "Retry",
  language: "Language",
  tasks: "Tasks",
  tasksHint: "Set a deadline, and attach a class if you want.",
  tasksEmpty: "No tasks yet",
  expandTasks: "Expand",
  collapseTasks: "Collapse",
  addTask: "Add a task",
  newTask: "New task",
  editTask: "Edit task",
  taskTitle: "What to do",
  taskTitleHint: "For example, submit a report",
  deadline: "Deadline",
  noDeadline: "No deadline",
  pickDate: "Choose a date",
  clearDeadline: "Clear deadline",
  timeOptional: "Time, if you need it",
  timeHint: "18:00",
  badTime: "Use the 18:00 format",
  attachLessonOptional: "Class, if you want",
  notAttached: "No class",
  removeLesson: "Remove class",
  chooseLesson: "Choose a class",
  save: "Save",
  overdue: "Overdue",
  completed: "Done",
  openTasks: "Open",
  tasksForLesson: "Tasks for this class",
  noTasksForLesson: "No tasks for this class",
  hasTask: "Has a task",
  allTasks: "All tasks",
  lessonFallback: "Class",
  liveInClass: "Now",
  liveSoon: "Until class",
  liveWindow: "Break",
  liveDone: "Classes are over today",
  liveNone: "No classes today",
  liveNext: "Next",
  exportWeek: "To calendar",
  exportEmpty: "Nothing to add this week",
  shareDay: "Share",
  copied: "Text copied",
  teachersMode: "Teachers",
  groupsMode: "Groups",
  findTeacher: "Teacher's surname",
  teacherHint: "Type at least two letters",
  favoriteTeachers: "Favorite teachers",
  noTeacherSoon: "No classes in the next few days",
  teacherSoon: "Every class in the next two weeks",
  addFavorite: "Add to favorites",
  removeFavorite: "Remove from favorites",
  wasAbsent: "Missed",
  clearAbsence: "Clear miss",
  missedBadge: "Missed",
  absences: "Absences",
  absencesEmpty: "No absences yet",
  hideLesson: "Skip this class",
  attendLesson: "Show again",
  hiddenTitle: "Hidden classes",
  hiddenEmpty: "No hidden classes",
  restore: "Restore",
  subjectNote: "Subject note",
  subjectNoteHint: "Shown on every class of this subject",
  subjectNotesTitle: "Subject notes",
  subjectNotesEmpty: "No subject notes yet",
  bothWeeks: "Both",
  numeratorColumn: "Numerator",
  denominatorColumn: "Denominator",
  backupSave: "Save a copy",
  backupLoad: "Load a copy",
  backupHint: "Name, group, notes, tasks and the file list go into one file. The Android app can open it too.",
  backupDone: "Copy saved",
  backupRestored: "Copy loaded",
  backupFailed: "Could not read the file",
  holidayQuiet: "Day off. No classes this day.",
  telegram: "Telegram",
  secondGroup: "Second group",
  secondGroupHint: "A classmate, an elective or a roommate. The overview shows when you are both free.",
  pickSecond: "Choose",
  clearSecond: "Remove",
  bothFreeTitle: "Both free",
  noSharedWindow: "No shared free slot in the next few days",
  todayChange: "Different today",
  todayOnly: "Only for today. Tomorrow the site schedule is back.",
  todayRoom: "Room for today",
  roomExample: "For example, 312",
  saveRoom: "Save room",
  cancelToday: "Cancel this class today",
  goneToday: "Not today",
  officialAgain: "As on the site",
  setLimit: "Limit",
  clearLimit: "Remove limit",
  limitUsed: "No skips left",
  welcomeTitle: "Hi! This is StudRaspBGTU",
  welcomeText: "See the class schedule without the mix-ups",
  offlinePageTitle: "Works offline",
  offlinePageText: "Add the app to your Home Screen and the schedule opens even without a connection",
  searchPageTitle: "Quick search across the university",
  searchPageText: "Look up groups, teachers and free rooms",
  nextPage: "Next",
  begin: "Get started",
  themeLight: "Light",
  themeDark: "Dark",
  langRu: "Русский",
  langEn: "English",
  localData: "Everything stays on this device. We cannot see it. Teacher ratings are sent anonymously.",
  updatesInTelegram: "App updates are posted in Telegram.",
  installTitle: "Install the app",
  installText: "Tap Share at the bottom of the screen, then “Add to Home Screen”.",
  rating: "Rating",
  rateTeacher: "Rate",
  editReview: "Edit review",
  deleteReview: "Delete review",
  overall: "Overall",
  delivery: "Delivery",
  strictness: "Strictness",
  loyalty: "Loyalty",
  reviewText: "Review",
  reviewHint: "What is worth knowing about this teacher (optional)",
  reviews: "Reviews",
  noReviews: "No reviews yet",
  noRatings: "No ratings yet",
  yourReview: "Your review",
  send: "Send",
  ratingAnon: "Reviews are anonymous. One review per device, you can edit it.",
  ratingOffline: "No connection to the server. Ratings will appear when you are back online.",
  ratingFailed: "Could not send. Try again later.",
  rateLimited: "Too many changes. Try again in an hour.",
  textTooLong: "The review is longer than 300 characters",
  unknownTeacher: "This teacher is not in the schedule",
  pickOverall: "Set all four scores",
  weekdays: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
  monthsGenitive: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
  monthsNominative: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
  dayShort: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
};

function ruPlural(count: number, one: string, few: string, many: string): string {
  const mod100 = count % 100;
  const mod10 = count % 10;
  if (mod100 >= 11 && mod100 <= 14) return many;
  if (mod10 === 1) return one;
  if (mod10 >= 2 && mod10 <= 4) return few;
  return many;
}

const capitalize = (text: string) => (text ? text[0].toUpperCase() + text.slice(1) : text);

function makeCopy(code: "ru" | "en", t: Strings) {
  const isEn = code === "en";
  const self = {
    ...t,
    code,
    formatLongDate(date: ISODate) {
      const { m, d } = parts(date);
      return `${t.weekdays[dow(date) - 1]}, ${d} ${t.monthsGenitive[m - 1]}`;
    },
    formatLongDateCap(date: ISODate) {
      return capitalize(self.formatLongDate(date));
    },
    formatMonth(date: ISODate) {
      const { y, m } = parts(date);
      return `${t.monthsNominative[m - 1]} ${y}`;
    },
    greeting(hour: number) {
      if (isEn) return hour >= 5 && hour <= 11 ? "Good morning" : hour >= 12 && hour <= 17 ? "Good afternoon" : hour >= 18 && hour <= 22 ? "Good evening" : "Good night";
      return hour >= 5 && hour <= 11 ? "Доброе утро" : hour >= 12 && hour <= 17 ? "Добрый день" : hour >= 18 && hour <= 22 ? "Добрый вечер" : "Доброй ночи";
    },
    pairsWord(count: number) {
      return isEn ? (count === 1 ? "class" : "classes") : ruPlural(count, "пара", "пары", "пар");
    },
    todayPill(count: number) {
      return isEn ? `${count} ${self.pairsWord(count)} today` : `${count} ${self.pairsWord(count)} сегодня`;
    },
    groupsCount(count: number) {
      return isEn ? (count === 1 ? "1 group" : `${count} groups`) : `${count} ${ruPlural(count, "группа", "группы", "групп")}`;
    },
    evenWeekTitle(even: boolean) {
      return isEn ? (even ? "Even week" : "Odd week") : even ? "чётная неделя" : "нечётная неделя";
    },
    kindTitle(even: boolean) {
      return even ? t.denominator : t.numerator;
    },
    studyWeekLine(number: number, even: boolean) {
      return isEn ? `Academic week ${number} · ${self.kindTitle(even)}` : `${number}-я учебная неделя · ${self.kindTitle(even)}`;
    },
    weekNow(number: number, even: boolean) {
      return isEn ? `Now ${self.kindTitle(even)}. Semester week ${number}.` : `Сейчас ${self.kindTitle(even)}. ${number}-я неделя семестра.`;
    },
    weekBadge(week: string) {
      if (week === "num") return t.numerator;
      if (week === "den") return t.denominator;
      return isEn ? "Every week" : "каждую неделю";
    },
    pairLabel(n: number) {
      return isEn ? `Class ${n}` : `${n} пара`;
    },
    cohortValue(year: number) {
      return isEn ? `Class of ${year}` : `набор ${year}`;
    },
    whenLabel(day: ISODate, today: ISODate) {
      if (day === today) return t.today.toLowerCase();
      if (day === addDays(today, 1)) return t.tomorrow.toLowerCase();
      return self.formatLongDate(day);
    },
    bothFreeLine(day: ISODate, today: ISODate, time: string) {
      const tomorrow = addDays(today, 1);
      if (isEn) {
        if (day === today) return `Both free at ${time}`;
        if (day === tomorrow) return `Both free tomorrow at ${time}`;
        return `Both free on ${self.formatLongDate(day)} at ${time}`;
      }
      if (day === today) return `Оба свободны в ${time}`;
      if (day === tomorrow) return `Оба свободны завтра в ${time}`;
      return `Оба свободны ${self.formatLongDate(day)} в ${time}`;
    },
    skipsLeft(left: number) {
      return isEn ? (left === 1 ? "1 left" : `${left} left`) : `осталось ${left}`;
    },
    openTasksLine(count: number) {
      return isEn ? (count === 1 ? "1 open task" : `${count} open tasks`) : `${count} ${ruPlural(count, "дело", "дела", "дел")}`;
    },
    filesCopied(names: string) {
      return isEn
        ? `Copied into the app: ${names}. This is a separate copy on the device. The original can be deleted.`
        : `Скопировано в память приложения: ${names}. Это отдельная копия на устройстве, оригинал можно удалить.`;
    },
    periodLabel(raw: string) {
      return isEn ? raw.replace("семестр", "semester") : raw;
    },
    levelName(raw: string) {
      const key = raw.trim().toLowerCase();
      if (key === "бакалавр") return isEn ? "Bachelor" : "Бакалавр";
      if (key === "магистр") return isEn ? "Master" : "Магистр";
      if (key === "специалист") return isEn ? "Specialist" : "Специалист";
      return capitalize(raw) || "—";
    },
    dayName(raw: string) {
      if (!isEn) return raw;
      const map: Record<string, string> = {
        Понедельник: "Monday",
        Вторник: "Tuesday",
        Среда: "Wednesday",
        Четверг: "Thursday",
        Пятница: "Friday",
        Суббота: "Saturday",
        Воскресенье: "Sunday",
      };
      return map[raw] ?? raw;
    },
    lessonType(raw: string) {
      if (!isEn || !raw) return raw;
      return raw
        .replace("Электронные лекции", "Online lectures")
        .replace("Лабораторные работы", "Lab work")
        .replace("Практические занятия", "Practice")
        .replace("Лекции", "Lectures");
    },
    span(minutes: number) {
      const hours = Math.floor(minutes / 60);
      const rest = minutes % 60;
      if (isEn) {
        const h = hours === 0 ? "" : hours === 1 ? "1 hour" : `${hours} hours`;
        const m = rest === 0 ? "" : rest === 1 ? "1 minute" : `${rest} minutes`;
        return [h, m].filter(Boolean).join(" ") || "0 minutes";
      }
      const h = hours === 0 ? "" : hours === 1 ? "1 час" : hours <= 4 ? `${hours} часа` : `${hours} часов`;
      const m =
        rest === 0
          ? ""
          : rest % 10 === 1 && rest % 100 !== 11
            ? `${rest} минута`
            : rest % 10 >= 2 && rest % 10 <= 4 && !(rest % 100 >= 12 && rest % 100 <= 14)
              ? `${rest} минуты`
              : `${rest} минут`;
      return [h, m].filter(Boolean).join(" ") || "0 минут";
    },
    leftPhrase(minutes: number) {
      return isEn ? `${self.span(minutes)} left` : `ещё ${self.span(minutes)}`;
    },
    untilPhrase(minutes: number) {
      return isEn ? `in ${self.span(minutes)}` : `через ${self.span(minutes)}`;
    },
    absenceCount(count: number) {
      return isEn ? (count === 1 ? "1 miss" : `${count} misses`) : `${count} ${ruPlural(count, "пропуск", "пропуска", "пропусков")}`;
    },
    hiddenSection(count: number) {
      return isEn ? `Hidden (${count})` : `Скрыто (${count})`;
    },
    resetFiles(count: number, size: string) {
      return isEn
        ? `Also delete files copied into the app (${count} · ${size})`
        : `Также удалить файлы, скопированные в приложение (${count} · ${size})`;
    },
    icsReady(count: number) {
      return isEn
        ? `Calendar file with ${count} ${count === 1 ? "class" : "classes"} is ready. Open it to add the week to your calendar.`
        : `Файл календаря готов, пар в нём: ${count}. Откройте его, чтобы добавить неделю в календарь.`;
    },
    ratingsCount(count: number) {
      return isEn ? (count === 1 ? "1 rating" : `${count} ratings`) : `${count} ${ruPlural(count, "оценка", "оценки", "оценок")}`;
    },
    formatBytes(size: number) {
      const [b, kb, mb] = isEn ? ["B", "KB", "MB"] : ["Б", "КБ", "МБ"];
      if (size < 1024) return `${size} ${b}`;
      if (size < 1024 * 1024) return `${Math.floor(size / 1024)} ${kb}`;
      const value = (size / 1048576).toFixed(1);
      return `${isEn ? value : value.replace(".", ",")} ${mb}`;
    },
  };
  return self;
}

export type Copy = ReturnType<typeof makeCopy>;

const RU = makeCopy("ru", ru);
const EN = makeCopy("en", en);

export const copyOf = (code: string | null | undefined): Copy => (code === "en" ? EN : RU);

export const APP_VERSION = "v1.1";
