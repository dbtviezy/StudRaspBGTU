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
    "Удалятся имя, группы, фото, заметки, пропуски, дела, избранное и изменения в расписании, а на этом устройстве — вход в аккаунт и сохранённая переписка. Тема и язык останутся. Снова откроется выбор факультета и группы.",
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
  localData: "Расписание, заметки, дела и фото хранятся на этом устройстве. Если вы вошли в аккаунт, заметки, дела, пропуски, избранное и настройки синхронизируются через сервер. Отзывы и сообщения чатов тоже хранятся на сервере.",
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
  ratingAnon: "Отзыв виден всем без имени. Один отзыв с аккаунта и с устройства, его можно изменить.",
  ratingOffline: "Нет связи с сервером. Оценки появятся, когда интернет вернётся.",
  ratingFailed: "Не удалось отправить. Попробуйте позже.",
  rateLimited: "Слишком много изменений. Попробуйте через час.",
  textTooLong: "Отзыв длиннее 300 символов",
  unknownTeacher: "Этого преподавателя нет в расписании",
  pickOverall: "Поставьте все четыре оценки",
  tabChat: "Чаты",
  myTeachers: "Мои преподаватели",
  otherTeachers: "Другие преподаватели",
  myTeachersEmpty: "У вашей группы преподаватели не указаны",
  teacherSearchHint: "Введите фамилию, чтобы найти любого преподавателя университета",
  like: "Полезный отзыв",
  dislike: "Бесполезный отзыв",
  report: "Пожаловаться",
  reportTitle: "Жалоба",
  reportText: "Модератор проверит и скроет, если есть нарушение. Что не так? (необязательно)",
  reportSent: "Жалоба отправлена. Спасибо!",
  ownReview: "Свой отзыв оценить нельзя",
  banned: "Доступ ограничен за нарушение правил",
  announcement: "Объявление",
  account: "Аккаунт",
  accountHint: "Нужен для отзывов, чатов и синхронизации заметок, дел и настроек между телефоном и сайтом. Расписание работает без входа.",
  signIn: "Войти",
  signUp: "Создать аккаунт",
  signOut: "Выйти",
  signOutEverywhere: "Выйти на всех устройствах",
  haveAccount: "Уже есть аккаунт? Войти",
  noAccount: "Нет аккаунта? Создать",
  loginField: "Логин",
  loginRule: "3–24 символа: латинские буквы, цифры, _ и точка",
  passwordField: "Пароль",
  passwordRule: "Не короче 6 символов",
  newPassword: "Новый пароль",
  chatName: "Имя в чатах",
  chatNameRule: "Его видят собеседники, от 2 до 40 символов",
  orWord: "или",
  withTelegram: "Войти через Telegram",
  withGoogle: "Войти через Google",
  linkTelegram: "Привязать Telegram",
  linkGoogle: "Привязать Google",
  unlink: "Отвязать",
  linked: "привязан",
  setPassword: "Добавить логин и пароль",
  changePassword: "Сменить пароль",
  yourId: "Ваш ID",
  yourIdHint: "Назовите его или логин другу, чтобы он написал вам лично",
  providerOff: "Этот способ входа ещё не настроен на сервере",
  deleteAccount: "Удалить аккаунт",
  deleteAccountText: "Будут удалены аккаунт, вход на всех устройствах, синхронизированные данные и ваши сообщения в чатах. Данные на этом устройстве останутся.",
  wrongCredentials: "Неверный логин или пароль",
  loginTaken: "Этот логин уже занят",
  badLogin: "Логин: 3–24 символа, латиница, цифры, _ и точка",
  badPassword: "Пароль не короче 6 символов",
  badName: "Имя от 2 до 40 символов",
  alreadyLinked: "Этот аккаунт уже привязан к другому профилю",
  lastMethod: "Нельзя отвязать единственный способ входа",
  serverOff: "Сервер недоступен. Попробуйте позже.",
  somethingWrong: "Что-то пошло не так. Попробуйте ещё раз.",
  tooMany: "Слишком много попыток. Подождите немного.",
  chatsTitle: "Чаты",
  chatsSignIn: "Войдите, чтобы переписываться с группой и лично с однокурсниками.",
  chatsOff: "Чаты ещё не подключены на сервере. Загляните позже.",
  noChats: "Чатов пока нет. Создайте чат своей группы или войдите по коду от старосты.",
  createGroupChat: "Создать чат группы",
  chatTitleField: "Название",
  joinByCode: "Войти по коду",
  inviteCode: "Код приглашения",
  inviteCodeHint: "8 символов, например K7M2QX9P",
  inviteShare: "Раздайте код одногруппникам — без него в чат не попасть.",
  newInviteCode: "Новый код",
  copyCode: "Скопировать код",
  codeCopied: "Код скопирован",
  newDirect: "Написать лично",
  directHint: "Логин или ID собеседника",
  find: "Найти",
  chatRequest: "хочет начать переписку",
  waitingAccept: "Ждём, когда собеседник примет приглашение",
  accept: "Принять",
  decline: "Отклонить",
  leaveChat: "Покинуть чат",
  leaveChatText: "Чат исчезнет из списка. Вернуться в чат группы можно снова по коду.",
  messagePlaceholder: "Сообщение",
  sendMessage: "Отправить",
  pendingMessage: "Отправится, когда появится сеть",
  failedMessage: "Не отправлено",
  offlineBanner: "Нет сети. Сообщения сохранятся и отправятся позже.",
  noMessages: "Сообщений пока нет. Напишите первым!",
  chatRetention: "Сообщения группы хранятся 7 дней, личные — 3 дня.",
  copyText: "Скопировать",
  invalidCode: "Код не найден",
  userNotFound: "Никого не нашли",
  forbidden: "Нет доступа",
  nearbyAndroidOnly: "Чат с соседями без интернета работает только в Android-приложении.",
  groupChat: "Чат группы",
  directChat: "Личный чат",
  members: "Участники",
  you: "Вы",
  moreActions: "Ещё",
  settingsTitle: "Настройки",
  themeSystem: "Тема как в системе",
  themeSystemHint: "Светлая или тёмная — как на устройстве",
  hideProfanity: "Скрывать мат",
  hideProfanityHint: "В чатах и отзывах плохие слова заменятся звёздочками",
  chatNotify: "Уведомления о сообщениях",
  chatNotifyHint: "Придут, даже когда сайт закрыт",
  chatNotifySignIn: "Сначала войдите в аккаунт",
  pushUnsupported: "Этот браузер не умеет присылать уведомления. На iPhone сначала добавьте сайт на экран «Домой».",
  pushDenied: "Уведомления запрещены в настройках браузера",
  calendarSubTitle: "Расписание в календаре",
  calendarSubHint: "Подписка обновляется сама, если расписание изменится. Заметки и скрытые пары туда не попадут.",
  calendarSubscribe: "Подписаться (iPhone, Mac, Outlook)",
  calendarGoogle: "Google Календарь",
  copyLink: "Скопировать ссылку",
  linkCopied: "Ссылка скопирована",
  whatsNewLink: "Что нового",
  whatsNewOk: "Понятно",
  whatsNewItems: [
    ["Чаты", "Чат группы по коду приглашения и личные сообщения. Написанное без сети отправится позже."],
    ["Закреп, опросы и дела группы", "Староста и помощники могут закрепить сообщение, провести опрос и выложить дедлайны для всей группы."],
    ["Аккаунт", "Отзывы, оценки отзывов и жалобы теперь с аккаунтом. Заметки, дела, пропуски, избранное и настройки одинаковые на телефоне и на сайте."],
    ["Староста", "Подтверждённый модератором староста получает значок и права помощника во всех чатах своей группы. Заявка — в профиле."],
    ["Отзывы", "Лайки и дизлайки к чужим отзывам и жалобы на нарушителей."],
    ["Где преподаватель", "На странице преподавателя видно, на какой он паре и в какой аудитории, или когда следующая."],
    ["Уведомления", "Уведомления о новых сообщениях приходят, даже когда сайт закрыт."],
    ["Календарь", "Подписка на расписание в календаре телефона — обновляется сама."],
    ["Настройки", "Тема как в системе, фильтр мата и полный английский перевод."],
  ] as [string, string][],
  whereTitle: "Где сейчас",
  whereInClass: "Сейчас на паре",
  whereFree: "Сейчас без пар",
  whereNext: "Ближайшая пара",
  whereNone: "В ближайшие две недели пар нет",
  pinned: "Закреплено",
  pin: "Закрепить",
  unpin: "Открепить",
  roleOwner: "создатель",
  roleRep: "староста",
  roleHelper: "помощник",
  makeHelper: "Сделать помощником",
  removeHelper: "Убрать из помощников",
  helpersHint: "Создатель чата, помощники и подтверждённый староста группы могут закреплять сообщения, создавать опросы и дела группы.",
  polls: "Опросы",
  newPoll: "Новый опрос",
  pollQuestion: "Вопрос",
  addOption: "Ещё вариант",
  pollMultiple: "Можно выбрать несколько",
  pollCreate: "Создать опрос",
  pollNeedOptions: "Нужен вопрос и хотя бы два варианта",
  pollClose: "Завершить",
  pollClosed: "Опрос завершён",
  pollDelete: "Удалить опрос",
  vote: "Голосовать",
  noPolls: "Опросов пока нет",
  groupTasks: "Дела группы",
  groupTasksHint: "Дедлайны от старосты. Добавьте дело к себе, чтобы получить напоминание.",
  newGroupTask: "Новое дело группы",
  groupTaskTitle: "Что сделать",
  groupTaskNote: "Подробности (необязательно)",
  dueField: "Срок",
  pickTime: "Время",
  addToMine: "В мои дела",
  addedToMine: "Добавлено в ваши дела",
  inMine: "Уже в ваших делах",
  noGroupTasks: "Дел группы пока нет",
  add: "Добавить",
  later: "Позже",
  needAccountTitle: "Нужен аккаунт",
  needAccountText: "Отзывы, оценки отзывов и жалобы доступны после входа. Так мы защищаемся от накруток и можем ограничивать нарушителей.",
  socialRequired: "Отзывы можно оставлять, если вы вошли через Telegram или Google. Привяжите один из них в аккаунте.",
  deviceUsed: "С этого устройства уже оставлен отзыв этому преподавателю из другого аккаунта.",
  muted: "Вам запрещено писать в чатах",
  noCreate: "Вам запрещено создавать чаты и писать новым людям",
  restrictionsTitle: "Ограничения",
  loginOfferTitle: "Войдите в аккаунт",
  loginOfferText: "С аккаунтом можно оставлять отзывы и писать в чатах, а заметки, дела, пропуски, избранное и настройки будут одинаковыми на телефоне и на сайте.",
  mineHiddenUnclaimed: "Отзыв скрыт, потому что не был привязан к аккаунту. Войдите, чтобы вернуть его.",
  deleteReviewsQuestion: "Что сделать с вашими отзывами о преподавателях?",
  deleteWithReviews: "Удалить вместе с аккаунтом",
  keepReviewsAnon: "Оставить без имени",
  myReviews: "Мои отзывы",
  noMyReviews: "Вы ещё не оставляли отзывов",
  hiddenReview: "скрыт",
  devices: "Мои устройства",
  thisDevice: "это устройство",
  unknownDevice: "Устройство",
  repTitle: "Староста группы",
  repHint: "Подтверждённый староста получает значок и права помощника во всех чатах своей группы. Заявку проверит модератор.",
  repRequest: "Я староста",
  repComment: "Комментарий для модератора: ФИО, как проверить (необязательно)",
  repSend: "Отправить заявку",
  repRejected: "Заявка отклонена. Можно отправить новую.",
  repCancel: "Отозвать заявку",
  repStepDown: "Перестать быть старостой",
  repNoGroup: "Сначала выберите группу в расписании",
  syncTitle: "Синхронизация",
  syncHint: "Заметки, дела, пропуски, скрытые пары, избранные преподаватели, группа и настройки одинаковые на всех устройствах с этим аккаунтом.",
  syncNow: "Синхронизировать",
  syncFailed: "Не удалось синхронизировать. Попробуем позже.",
  syncAskTitle: "Объединить данные?",
  syncAskText: "На этом устройстве и в аккаунте разные заметки, дела или другие данные. Что оставить?",
  syncMerge: "Объединить",
  syncMergeHint: "Сохранится всё. Если одно и то же изменено и там, и тут, останется вариант с этого устройства.",
  syncTakeAccount: "Взять из аккаунта",
  syncTakeAccountHint: "Данные на этом устройстве заменятся данными из аккаунта.",
  syncKeepDevice: "Оставить с устройства",
  syncKeepDeviceHint: "Данные в аккаунте заменятся данными с этого устройства.",
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
    "Your name, groups, photo, notes, absences, tasks, favorites and schedule changes will be removed, and this device will be signed out with its saved chats cleared. Theme and language stay. Faculty and group selection will open again.",
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
  localData: "Schedule, notes, tasks and photo are stored on this device. When you sign in, notes, tasks, misses, favorites and settings sync through the server. Reviews and chat messages are stored on the server too.",
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
  ratingAnon: "Everyone sees the review without your name. One review per account and device, you can edit it.",
  ratingOffline: "No connection to the server. Ratings will appear when you are back online.",
  ratingFailed: "Could not send. Try again later.",
  rateLimited: "Too many changes. Try again in an hour.",
  textTooLong: "The review is longer than 300 characters",
  unknownTeacher: "This teacher is not in the schedule",
  pickOverall: "Set all four scores",
  tabChat: "Chats",
  myTeachers: "My teachers",
  otherTeachers: "Other teachers",
  myTeachersEmpty: "No teachers are listed for your group",
  teacherSearchHint: "Type a surname to find any teacher of the university",
  like: "Helpful review",
  dislike: "Not helpful",
  report: "Report",
  reportTitle: "Report",
  reportText: "A moderator will check it and hide it if it breaks the rules. What is wrong? (optional)",
  reportSent: "Report sent. Thank you!",
  ownReview: "You cannot vote on your own review",
  banned: "Access is restricted for breaking the rules",
  announcement: "Announcement",
  account: "Account",
  accountHint: "Needed for reviews, chats and syncing notes, tasks and settings between your phone and the website. The schedule works without signing in.",
  signIn: "Sign in",
  signUp: "Create account",
  signOut: "Sign out",
  signOutEverywhere: "Sign out on all devices",
  haveAccount: "Already have an account? Sign in",
  noAccount: "No account? Create one",
  loginField: "Login",
  loginRule: "3–24 characters: Latin letters, digits, _ and dot",
  passwordField: "Password",
  passwordRule: "At least 6 characters",
  newPassword: "New password",
  chatName: "Name in chats",
  chatNameRule: "Others see it, 2 to 40 characters",
  orWord: "or",
  withTelegram: "Sign in with Telegram",
  withGoogle: "Sign in with Google",
  linkTelegram: "Link Telegram",
  linkGoogle: "Link Google",
  unlink: "Unlink",
  linked: "linked",
  setPassword: "Add login and password",
  changePassword: "Change password",
  yourId: "Your ID",
  yourIdHint: "Share it or your login with a friend so they can message you",
  providerOff: "This sign-in method is not set up on the server yet",
  deleteAccount: "Delete account",
  deleteAccountText: "The account, its sign-ins on all devices, synced data and your chat messages will be deleted. Data on this device stays.",
  wrongCredentials: "Wrong login or password",
  loginTaken: "This login is already taken",
  badLogin: "Login: 3–24 characters, Latin letters, digits, _ and dot",
  badPassword: "Password must be at least 6 characters",
  badName: "Name must be 2 to 40 characters",
  alreadyLinked: "This account is already linked to another profile",
  lastMethod: "You cannot unlink your only sign-in method",
  serverOff: "The server is unavailable. Try again later.",
  somethingWrong: "Something went wrong. Try again.",
  tooMany: "Too many attempts. Please wait a bit.",
  chatsTitle: "Chats",
  chatsSignIn: "Sign in to chat with your group and message classmates directly.",
  chatsOff: "Chats are not connected on the server yet. Check back later.",
  noChats: "No chats yet. Create a chat for your group or join with a code from your class rep.",
  createGroupChat: "Create group chat",
  chatTitleField: "Title",
  joinByCode: "Join with a code",
  inviteCode: "Invite code",
  inviteCodeHint: "8 characters, e.g. K7M2QX9P",
  inviteShare: "Share the code with your groupmates — the chat is closed without it.",
  newInviteCode: "New code",
  copyCode: "Copy code",
  codeCopied: "Code copied",
  newDirect: "Message someone",
  directHint: "Their login or ID",
  find: "Find",
  chatRequest: "wants to start a conversation",
  waitingAccept: "Waiting for them to accept the invitation",
  accept: "Accept",
  decline: "Decline",
  leaveChat: "Leave chat",
  leaveChatText: "The chat will disappear from the list. You can rejoin a group chat with its code.",
  messagePlaceholder: "Message",
  sendMessage: "Send",
  pendingMessage: "Will be sent when you are back online",
  failedMessage: "Not sent",
  offlineBanner: "You are offline. Messages are saved and will be sent later.",
  noMessages: "No messages yet. Say hi!",
  chatRetention: "Group messages are kept for 7 days, direct ones for 3 days.",
  copyText: "Copy",
  invalidCode: "Code not found",
  userNotFound: "Nobody found",
  forbidden: "Access denied",
  nearbyAndroidOnly: "Offline chat with people nearby works only in the Android app.",
  groupChat: "Group chat",
  directChat: "Direct chat",
  members: "Members",
  you: "You",
  moreActions: "More",
  settingsTitle: "Settings",
  themeSystem: "Match system theme",
  themeSystemHint: "Light or dark, same as your device",
  hideProfanity: "Hide swear words",
  hideProfanityHint: "Swear words in chats and reviews are replaced with asterisks",
  chatNotify: "Message notifications",
  chatNotifyHint: "They arrive even when the site is closed",
  chatNotifySignIn: "Sign in first",
  pushUnsupported: "This browser cannot show notifications. On iPhone, add the site to the Home Screen first.",
  pushDenied: "Notifications are blocked in the browser settings",
  calendarSubTitle: "Schedule in your calendar",
  calendarSubHint: "The subscription updates itself when the schedule changes. Notes and hidden classes are not included.",
  calendarSubscribe: "Subscribe (iPhone, Mac, Outlook)",
  calendarGoogle: "Google Calendar",
  copyLink: "Copy link",
  linkCopied: "Link copied",
  whatsNewLink: "What's new",
  whatsNewOk: "Got it",
  whatsNewItems: [
    ["Chats", "Group chat by invite code and direct messages. Messages written offline are sent later."],
    ["Pins, polls and group tasks", "The class rep and helpers can pin a message, run a poll and post deadlines for the whole group."],
    ["Account", "Reviews, review votes and reports now need an account. Notes, tasks, misses, favorites and settings are the same on your phone and the website."],
    ["Class rep", "A class rep confirmed by a moderator gets a badge and helper rights in every chat of their group. Send a request in your profile."],
    ["Reviews", "Like or dislike other people's reviews and report rule-breakers."],
    ["Where is the teacher", "The teacher page shows their current class and room, or the next one."],
    ["Notifications", "New message notifications arrive even when the site is closed."],
    ["Calendar", "Subscribe to your schedule in your phone's calendar — it updates itself."],
    ["Settings", "Theme that follows the system, a swear-word filter and full English."],
  ],
  whereTitle: "Where now",
  whereInClass: "In class now",
  whereFree: "No class right now",
  whereNext: "Next class",
  whereNone: "No classes in the next two weeks",
  pinned: "Pinned",
  pin: "Pin",
  unpin: "Unpin",
  roleOwner: "creator",
  roleRep: "class rep",
  roleHelper: "helper",
  makeHelper: "Make helper",
  removeHelper: "Remove helper",
  helpersHint: "The chat creator, helpers and the confirmed class rep of the group can pin messages and create polls and group tasks.",
  polls: "Polls",
  newPoll: "New poll",
  pollQuestion: "Question",
  addOption: "Add option",
  pollMultiple: "Allow several answers",
  pollCreate: "Create poll",
  pollNeedOptions: "Add a question and at least two options",
  pollClose: "End poll",
  pollClosed: "Poll ended",
  pollDelete: "Delete poll",
  vote: "Vote",
  noPolls: "No polls yet",
  groupTasks: "Group tasks",
  groupTasksHint: "Deadlines from the class rep. Add one to your tasks to get a reminder.",
  newGroupTask: "New group task",
  groupTaskTitle: "What to do",
  groupTaskNote: "Details (optional)",
  dueField: "Due date",
  pickTime: "Time",
  addToMine: "Add to my tasks",
  addedToMine: "Added to your tasks",
  inMine: "Already in your tasks",
  noGroupTasks: "No group tasks yet",
  add: "Add",
  later: "Later",
  needAccountTitle: "Account needed",
  needAccountText: "Reviews, review votes and reports need an account. This protects ratings from cheating and lets moderators restrict rule breakers.",
  socialRequired: "To leave reviews, sign in with Telegram or Google. Link one of them in your account.",
  deviceUsed: "A review of this teacher was already left from this device by another account.",
  muted: "You are not allowed to write in chats",
  noCreate: "You are not allowed to create chats or message new people",
  restrictionsTitle: "Restrictions",
  loginOfferTitle: "Sign in",
  loginOfferText: "With an account you can leave reviews and chat, and your notes, tasks, misses, favorites and settings stay the same on your phone and the website.",
  mineHiddenUnclaimed: "This review is hidden because it was never linked to an account. Sign in to bring it back.",
  deleteReviewsQuestion: "What should happen to your teacher reviews?",
  deleteWithReviews: "Delete them with the account",
  keepReviewsAnon: "Keep them without a name",
  myReviews: "My reviews",
  noMyReviews: "You haven't left any reviews yet",
  hiddenReview: "hidden",
  devices: "My devices",
  thisDevice: "this device",
  unknownDevice: "Device",
  repTitle: "Class rep",
  repHint: "A confirmed class rep gets a badge and helper rights in every chat of their group. A moderator checks the request.",
  repRequest: "I'm the class rep",
  repComment: "Note for the moderator: full name, how to check (optional)",
  repSend: "Send request",
  repRejected: "The request was declined. You can send a new one.",
  repCancel: "Withdraw request",
  repStepDown: "Stop being class rep",
  repNoGroup: "Pick your group in the schedule first",
  syncTitle: "Sync",
  syncHint: "Notes, tasks, misses, hidden classes, favorite teachers, group and settings are the same on every device with this account.",
  syncNow: "Sync now",
  syncFailed: "Couldn't sync. We'll try again later.",
  syncAskTitle: "Combine data?",
  syncAskText: "This device and your account have different notes, tasks or other data. What should stay?",
  syncMerge: "Combine",
  syncMergeHint: "Everything stays. If the same item was changed in both places, this device's version wins.",
  syncTakeAccount: "Use the account's data",
  syncTakeAccountHint: "Data on this device is replaced with the account's data.",
  syncKeepDevice: "Use this device's data",
  syncKeepDeviceHint: "The account's data is replaced with this device's data.",
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

const FACULTIES_EN: Record<string, string> = {
  "Механико-технологический факультет": "Faculty of Mechanical Engineering and Technology",
  "Учебно-научный институт транспорта": "Institute of Transport",
  "Учебно-научный технологический институт": "Institute of Technology",
  "Факультет информационных технологий": "Faculty of Information Technology",
  "Факультет отраслевой и цифровой экономики": "Faculty of Industrial and Digital Economics",
  "Факультет энергетики и электроники": "Faculty of Power Engineering and Electronics",
};

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
      return isEn ? raw.replace(/(\d+)\s*семестр/, "semester $1") : raw;
    },
    facultyName(raw: string) {
      if (!isEn) return raw;
      return FACULTIES_EN[raw.trim()] ?? raw;
    },
    roomName(raw: string) {
      if (!isEn) return raw;
      return raw.replace("Читальный зал", "Reading room").replace("Спортивный зал", "Gym").replace("Лаборатория", "Lab");
    },
    teacherGroups(count: number) {
      return isEn ? (count === 1 ? "teaches 1 group" : `teaches ${count} groups`) : `ведёт у ${count} ${ruPlural(count, "группы", "групп", "групп")}`;
    },
    unreadCount(count: number) {
      return count > 99 ? "99+" : String(count);
    },
    /** Human text for an error code returned by our API or the chat server. */
    errorText(code: string) {
      switch (code) {
        case "offline":
        case "db_unavailable":
        case "not_configured":
        case "server_error":
        case "chat_unavailable":
        case "provider_unavailable": return t.serverOff;
        case "chat_not_configured": return t.chatsOff;
        case "provider_disabled": return t.providerOff;
        case "rate_limited": return t.tooMany;
        case "wrong_credentials": return t.wrongCredentials;
        case "login_taken": return t.loginTaken;
        case "invalid_login": return t.badLogin;
        case "invalid_password": return t.badPassword;
        case "invalid_name": return t.badName;
        case "already_linked": return t.alreadyLinked;
        case "last_method": return t.lastMethod;
        case "banned": return t.banned;
        case "restricted": return t.forbidden;
        case "muted": return t.muted;
        case "no_create": return t.noCreate;
        case "social_required": return t.socialRequired;
        case "account_too_new": return t.socialRequired;
        case "device_used": return t.deviceUsed;
        case "own_review": return t.ownReview;
        case "invalid_code": return t.invalidCode;
        case "user_not_found": return t.userNotFound;
        case "forbidden":
        case "unauthorized": return t.forbidden;
        case "text_too_long": return t.textTooLong;
        default: return t.somethingWrong;
      }
    },
    /** Like errorText, but spells out restrictions (what, until when, why) when the server sent them. */
    failureText(error: unknown) {
      const code = error instanceof Error ? error.message : "";
      const extra = (error as { extra?: Record<string, unknown> } | null)?.extra ?? {};
      if ((code === "restricted" || code === "banned") && typeof extra.scope === "string") {
        return self.restrictionText(extra.scope, (extra.until as string | null) ?? null, String(extra.reason ?? ""));
      }
      if (code === "account_too_new" && typeof extra.until === "string") return self.accountTooNew(extra.until);
      return self.errorText(code);
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
        .replaceAll("Электронные лекции", "Online lectures")
        .replaceAll("Лабораторные работы", "Lab work")
        .replaceAll("Практические занятия", "Practice")
        .replaceAll("Лекции", "Lectures");
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
    whatsNewTitle(version: string) {
      return isEn ? `What's new in ${version}` : `Что нового в ${version}`;
    },
    groupsLine(groups: string[]) {
      const label = groups.length === 1 ? (isEn ? "Group" : "Группа") : isEn ? "Groups" : "Группы";
      return `${label} ${groups.join(", ")}`;
    },
    untilTime(time: string) {
      return isEn ? `until ${time}` : `до ${time}`;
    },
    pollOption(n: number) {
      return isEn ? `Option ${n}` : `Вариант ${n}`;
    },
    pollVotes(count: number) {
      return isEn ? (count === 1 ? "1 vote" : `${count} votes`) : `${count} ${ruPlural(count, "голос", "голоса", "голосов")}`;
    },
    newMessages(count: number) {
      return isEn
        ? count === 1 ? "1 new message" : `${count} new messages`
        : `${count} ${ruPlural(count, "новое сообщение", "новых сообщения", "новых сообщений")}`;
    },
    fromChat(title: string) {
      return isEn ? `From “${title}”` : `Из чата «${title}»`;
    },
    /** "5 ноября" or "5 ноября, 14:30" from an ISO timestamp, in local time. */
    moment(iso: string, withTime = false) {
      const date = new Date(iso);
      const day = isEn
        ? `${t.monthsGenitive[date.getMonth()]} ${date.getDate()}`
        : `${date.getDate()} ${t.monthsGenitive[date.getMonth()]}`;
      if (!withTime) return day;
      const time = `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
      return `${day}, ${time}`;
    },
    scopeAction(scope: string) {
      const ru: Record<string, string> = {
        reviews: "оставлять отзывы",
        votes: "оценивать отзывы",
        reports: "отправлять жалобы",
        chat: "писать в чатах",
        chat_create: "создавать чаты и писать новым людям",
      };
      const en: Record<string, string> = {
        reviews: "leave reviews",
        votes: "vote on reviews",
        reports: "send reports",
        chat: "write in chats",
        chat_create: "create chats or message new people",
      };
      return (isEn ? en : ru)[scope] ?? (isEn ? "use your account" : "пользоваться аккаунтом");
    },
    /** "Вам запрещено оставлять отзывы до 5 ноября. Причина: …" */
    restrictionText(scope: string, until: string | null, reason: string) {
      const head = scope === "all"
        ? t.banned
        : isEn ? `You are not allowed to ${self.scopeAction(scope)}` : `Вам запрещено ${self.scopeAction(scope)}`;
      const when = until ? (isEn ? ` until ${self.moment(until, true)}` : ` до ${self.moment(until, true)}`) : isEn ? " permanently" : " навсегда";
      const why = reason ? (isEn ? `. Reason: ${reason}` : `. Причина: ${reason}`) : "";
      return `${head}${when}${why}`;
    },
    accountTooNew(until: string) {
      return isEn
        ? `Your account is new: you can leave reviews from ${self.moment(until, true)}`
        : `Аккаунт новый: отзывы можно оставлять с ${self.moment(until, true)}`;
    },
    legacyWarn(until: string | null) {
      const tail = until ? (isEn ? ` Otherwise they will be hidden on ${self.moment(until)}.` : ` Иначе ${self.moment(until)} их скроют.`) : "";
      return isEn
        ? `Reviews were left from this device without an account. Sign in to keep them yours.${tail}`
        : `С этого устройства оставлены отзывы без аккаунта. Войдите, чтобы они остались за вами.${tail}`;
    },
    legacyMine(until: string | null) {
      const tail = until ? (isEn ? `, otherwise it will be hidden on ${self.moment(until)}` : `, иначе ${self.moment(until)} его скроют`) : "";
      return isEn
        ? `This review isn't linked to an account yet. Sign in${tail}.`
        : `Этот отзыв ещё не привязан к аккаунту. Войдите${tail}.`;
    },
    repPending(group: string) {
      return isEn ? `Your class rep request for group ${group} is being checked` : `Заявка на старосту группы ${group} на проверке`;
    },
    repApproved(group: string) {
      return isEn ? `You are the class rep of group ${group}` : `Вы староста группы ${group}`;
    },
    deviceSeen(iso: string) {
      return isEn ? `active ${self.moment(iso, true)}` : `был активен ${self.moment(iso, true)}`;
    },
    syncDone(iso: string) {
      return isEn ? `Synced ${self.moment(iso, true)}` : `Синхронизировано ${self.moment(iso, true)}`;
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

export const APP_VERSION = "v1.2";
