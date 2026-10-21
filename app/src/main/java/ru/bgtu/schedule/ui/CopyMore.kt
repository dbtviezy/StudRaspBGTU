package ru.bgtu.schedule.ui

/** Texts added in 1.2 (accounts, chats, votes, moderation). Same wording as the web app. */
class MoreCopy(private val en: Boolean) {
    private fun t(ru: String, en: String) = if (this.en) en else ru

    val tabChat = t("Чаты", "Chats")
    val copied = t("Текст скопирован", "Text copied")
    val subjectNotesTitle = t("Заметки к предметам", "Subject notes")
    val subjectNotesEmpty = t("Заметок к предметам пока нет", "No subject notes yet")
    val yourReview = t("Ваш отзыв", "Your review")
    val pickOverall = t("Поставьте все четыре оценки", "Set all four scores")
    val ratingFailed = t("Не удалось отправить. Попробуйте позже.", "Could not send. Try again later.")

    val myTeachers = t("Мои преподаватели", "My teachers")
    val otherTeachers = t("Другие преподаватели", "Other teachers")
    val myTeachersEmpty = t("У вашей группы преподаватели не указаны", "No teachers are listed for your group")
    val teacherSearchHint = t("Введите фамилию, чтобы найти любого преподавателя университета", "Type a surname to find any teacher of the university")

    val like = t("Полезный отзыв", "Helpful review")
    val dislike = t("Бесполезный отзыв", "Not helpful")
    val report = t("Пожаловаться", "Report")
    val reportTitle = t("Жалоба", "Report")
    val reportText = t(
        "Модератор проверит и скроет, если есть нарушение. Что не так? (необязательно)",
        "A moderator will check it and hide it if it breaks the rules. What is wrong? (optional)",
    )
    val reportSent = t("Жалоба отправлена. Спасибо!", "Report sent. Thank you!")
    val ownReview = t("Свой отзыв оценить нельзя", "You cannot vote on your own review")
    val banned = t("Доступ ограничен за нарушение правил", "Access is restricted for breaking the rules")
    val announcement = t("Объявление", "Announcement")

    val account = t("Аккаунт", "Account")
    val accountHint = t(
        "Нужен для отзывов, чатов и синхронизации заметок, дел и настроек между телефоном и сайтом. Расписание работает без входа.",
        "Needed for reviews, chats and syncing notes, tasks and settings between your phone and the website. The schedule works without signing in.",
    )
    val signIn = t("Войти", "Sign in")
    val signUp = t("Создать аккаунт", "Create account")
    val signOut = t("Выйти", "Sign out")
    val signOutEverywhere = t("Выйти на всех устройствах", "Sign out on all devices")
    val haveAccount = t("Уже есть аккаунт? Войти", "Already have an account? Sign in")
    val noAccount = t("Нет аккаунта? Создать", "No account? Create one")
    val loginField = t("Логин", "Login")
    val loginRule = t("3–24 символа: латинские буквы, цифры, _ и точка", "3–24 characters: Latin letters, digits, _ and dot")
    val passwordField = t("Пароль", "Password")
    val passwordRule = t("Не короче 6 символов", "At least 6 characters")
    val newPassword = t("Новый пароль", "New password")
    val chatName = t("Имя в чатах", "Name in chats")
    val chatNameRule = t("Его видят собеседники, от 2 до 40 символов", "Others see it, 2 to 40 characters")
    val orWord = t("или", "or")
    val withTelegram = t("Войти через Telegram", "Sign in with Telegram")
    val withGoogle = t("Войти через Google", "Sign in with Google")
    val browserHint = t("Откроется браузер, после входа вы вернётесь в приложение.", "A browser opens; after signing in you return to the app.")
    val linkOnSite = t("Привязать Telegram или Google можно на сайте, войдя тем же логином.", "You can link Telegram or Google on the website after signing in with the same login.")
    val unlink = t("Отвязать", "Unlink")
    val linked = t("привязан", "linked")
    val setPassword = t("Добавить логин и пароль", "Add login and password")
    val changePassword = t("Сменить пароль", "Change password")
    val yourId = t("Ваш ID", "Your ID")
    val yourIdHint = t("Назовите его или логин другу, чтобы он написал вам лично", "Share it or your login with a friend so they can message you")
    val providerOff = t("Этот способ входа ещё не настроен на сервере", "This sign-in method is not set up on the server yet")
    val deleteAccount = t("Удалить аккаунт", "Delete account")
    val deleteAccountText = t(
        "Будут удалены аккаунт, вход на всех устройствах, синхронизированные данные и ваши сообщения в чатах. Данные на этом устройстве останутся.",
        "The account, its sign-ins on all devices, synced data and your chat messages will be deleted. Data on this device stays.",
    )
    val signedInToast = t("Вы вошли в аккаунт", "You are signed in")
    val wrongCredentials = t("Неверный логин или пароль", "Wrong login or password")
    val loginTaken = t("Этот логин уже занят", "This login is already taken")
    val badLogin = t("Логин: 3–24 символа, латиница, цифры, _ и точка", "Login: 3–24 characters, Latin letters, digits, _ and dot")
    val badPassword = t("Пароль не короче 6 символов", "Password must be at least 6 characters")
    val badName = t("Имя от 2 до 40 символов", "Name must be 2 to 40 characters")
    val alreadyLinked = t("Этот аккаунт уже привязан к другому профилю", "This account is already linked to another profile")
    val lastMethod = t("Нельзя отвязать единственный способ входа", "You cannot unlink your only sign-in method")
    val serverOff = t("Сервер недоступен. Попробуйте позже.", "The server is unavailable. Try again later.")
    val somethingWrong = t("Что-то пошло не так. Попробуйте ещё раз.", "Something went wrong. Try again.")
    val tooMany = t("Слишком много попыток. Подождите немного.", "Too many attempts. Please wait a bit.")

    val chatsTitle = t("Чаты", "Chats")
    val chatsSignIn = t("Войдите, чтобы переписываться с группой и лично с однокурсниками.", "Sign in to chat with your group and message classmates directly.")
    val chatsOff = t("Чаты ещё не подключены на сервере. Загляните позже.", "Chats are not connected on the server yet. Check back later.")
    val noChats = t(
        "Чатов пока нет. Создайте чат своей группы или войдите по коду от старосты.",
        "No chats yet. Create a chat for your group or join with a code from your class rep.",
    )
    val createGroupChat = t("Создать чат группы", "Create group chat")
    val chatTitleField = t("Название", "Title")
    val joinByCode = t("Войти по коду", "Join with a code")
    val inviteCode = t("Код приглашения", "Invite code")
    val inviteCodeHint = t("8 символов, например K7M2QX9P", "8 characters, e.g. K7M2QX9P")
    val inviteShare = t("Раздайте код одногруппникам — без него в чат не попасть.", "Share the code with your groupmates — the chat is closed without it.")
    val newInviteCode = t("Новый код", "New code")
    val copyCode = t("Скопировать код", "Copy code")
    val codeCopied = t("Код скопирован", "Code copied")
    val newDirect = t("Написать лично", "Message someone")
    val directHint = t("Логин или ID собеседника", "Their login or ID")
    val find = t("Найти", "Find")
    val chatRequest = t("хочет начать переписку", "wants to start a conversation")
    val waitingAccept = t("Ждём, когда собеседник примет приглашение", "Waiting for them to accept the invitation")
    val accept = t("Принять", "Accept")
    val decline = t("Отклонить", "Decline")
    val leaveChat = t("Покинуть чат", "Leave chat")
    val leaveChatText = t("Чат исчезнет из списка. Вернуться в чат группы можно снова по коду.", "The chat will disappear from the list. You can rejoin a group chat with its code.")
    val messagePlaceholder = t("Сообщение", "Message")
    val sendMessage = t("Отправить", "Send")
    val pendingMessage = t("Отправится, когда появится сеть", "Will be sent when you are back online")
    val failedMessage = t("Не отправлено", "Not sent")
    val offlineBanner = t("Нет сети. Сообщения сохранятся и отправятся позже.", "You are offline. Messages are saved and will be sent later.")
    val noMessages = t("Сообщений пока нет. Напишите первым!", "No messages yet. Say hi!")
    val chatRetention = t("Сообщения группы хранятся 7 дней, личные — 3 дня.", "Group messages are kept for 7 days, direct ones for 3 days.")
    val copyText = t("Скопировать", "Copy")
    val invalidCode = t("Код не найден", "Code not found")
    val userNotFound = t("Никого не нашли", "Nobody found")
    val forbidden = t("Нет доступа", "Access denied")
    val groupChat = t("Чат группы", "Group chat")
    val directChat = t("Личный чат", "Direct chat")
    val you = t("Вы", "You")
    val chatOnline = t("Онлайн", "Online")
    val chatNearby = t("Рядом", "Nearby")

    val nearbyTitle = t("Рядом без интернета", "Nearby, no internet")
    val nearbyHint = t(
        "Переписка с телефонами поблизости через Bluetooth и Wi-Fi, даже без сети. Нужен этот же раздел, открытый на другом телефоне. Сообщения нигде не сохраняются.",
        "Chat with phones around you over Bluetooth and Wi-Fi, even without a network. The other phone needs this screen open too. Messages are not saved anywhere.",
    )
    val nearbyStart = t("Включить", "Turn on")
    val nearbyStop = t("Выключить", "Turn off")
    val nearbySearching = t("Ищем телефоны рядом…", "Looking for phones nearby…")
    val nearbyNoPermission = t("Нужно разрешение на поиск устройств поблизости", "Permission to find nearby devices is needed")
    val nearbyFailed = t("Не удалось включить. Проверьте Bluetooth, Wi-Fi и геолокацию.", "Could not start. Check Bluetooth, Wi-Fi and location.")
    val nearbyEmpty = t("Сообщений пока нет", "No messages yet")

    fun nearbyPeers(count: Int): String = if (en) {
        if (count == 1) "1 phone connected" else "$count phones connected"
    } else {
        "Подключено телефонов: $count"
    }

    val adminPanel = t("Модерация", "Moderation")
    val adminHint = t("Вход по паролю администратора с сервера.", "Sign in with the admin password set on the server.")
    val adminPassword = t("Пароль администратора", "Admin password")
    val adminSignOut = t("Выйти из модерации", "Sign out of moderation")
    val adminStats = t("Статистика", "Stats")
    val adminReports = t("Жалобы", "Reports")
    val adminReviews = t("Отзывы", "Reviews")
    val adminMessages = t("Сообщения", "Messages")
    val adminBans = t("Баны", "Bans")
    val adminAnnouncements = t("Объявления", "Announcements")
    val adminUsers = t("Люди", "Users")
    val adminSearch = t("Поиск", "Search")
    val adminEmpty = t("Пусто", "Nothing here")
    val adminDone = t("Готово", "Done")
    val hide = t("Скрыть", "Hide")
    val unhide = t("Показать", "Unhide")
    val deleteForever = t("Удалить", "Delete")
    val ban = t("Заблокировать", "Ban")
    val unban = t("Разблокировать", "Unban")
    val dismissReport = t("Отклонить", "Dismiss")
    val acceptReport = t("Принять меры", "Take action")
    val banDays = t("Срок в днях (0 — навсегда)", "Days (0 — forever)")
    val banReason = t("Причина", "Reason")
    val hideAllReviews = t("Скрыть все отзывы с этого телефона", "Hide every review from this phone")
    val deleteMessageToo = t("Удалить и само сообщение", "Delete the message too")
    val announceText = t("Текст объявления", "Announcement text")
    val announceDays = t("Сколько дней показывать (0 — пока не сниму)", "Days to show (0 — until removed)")
    val publish = t("Опубликовать", "Publish")
    val takeDown = t("Снять", "Take down")
    val hiddenMark = t("скрыт", "hidden")
    val activeMark = t("показывается", "shown")
    val deviceBan = t("устройство", "device")
    val forever = t("навсегда", "forever")
    val until = t("до", "until")
    val confirmDelete = t("Удалить безвозвратно?", "Delete permanently?")
    val reportReview = t("Отзыв", "Review")
    val reportMessage = t("Сообщение", "Message")
    val reportsLabel = t("жалоб", "reports")
    val chatsNotConnected = t("Чаты не подключены на сервере", "Chats are not connected on the server")
    val statUsers = t("Аккаунтов", "Accounts")
    val statActive = t("Заходили за 7 дней", "Active in 7 days")
    val statReviews = t("Отзывов", "Reviews")
    val statHidden = t("Скрытых отзывов", "Hidden reviews")
    val statNewReviews = t("Новых отзывов за 7 дней", "New reviews in 7 days")
    val statTeachers = t("Оценённых преподавателей", "Rated teachers")
    val statVotes = t("Лайков и дизлайков", "Votes")
    val statReports = t("Открытых жалоб", "Open reports")
    val statBans = t("Активных банов", "Active bans")
    val statAnnouncements = t("Объявлений", "Announcements")
    val statChats = t("Чатов", "Chats")
    val statMessages = t("Сообщений", "Messages")
    val moderation = t("Модерация", "Moderation")

    val settingsTitle = t("Настройки", "Settings")
    val themeSystem = t("Тема как в системе", "Match system theme")
    val themeSystemHint = t("Светлая или тёмная — как на телефоне", "Light or dark, same as your device")
    val hideProfanity = t("Скрывать мат", "Hide swear words")
    val hideProfanityHint = t("В чатах и отзывах плохие слова заменятся звёздочками", "Swear words in chats and reviews are replaced with asterisks")
    val notifyProfilesTitle = t("Уведомления", "Notifications")
    val notifyProfilesHint = t(
        "Один режим на всё: пары, дайджест, дедлайны и чаты. Потом можно будет добавить новые режимы.",
        "One mode for everything: classes, digest, deadlines and chats. New modes can be added later.",
    )
    fun notifyProfileLabel(id: String): String = when (id) {
        "normal" -> t("Обычный", "Normal")
        "quiet" -> t("Тихий", "Quiet")
        "session" -> t("Сессия", "Exam period")
        "off" -> t("Выкл", "Off")
        else -> id
    }
    fun notifyProfileHint(id: String): String = when (id) {
        "normal" -> t("Пары, дайджест, дедлайны, чаты и объявления", "Classes, digest, deadlines, chats and announcements")
        "quiet" -> t("Только дедлайны, чаты и объявления — без напоминаний о парах", "Only deadlines, chats and announcements — no class reminders")
        "session" -> t("Только дедлайны", "Deadlines only")
        "off" -> t("Все уведомления приложения выключены", "All app notifications are off")
        else -> ""
    }
    val digestTitle = t("Завтрашнее расписание", "Tomorrow's schedule")
    val digestSwitch = t("Вечерний дайджест", "Evening digest")
    val digestSwitchHint = t(
        "Вечером одно сообщение: сколько пар завтра и где первая",
        "One evening message: how many classes tomorrow and where the first is",
    )
    val digestHour = t("Время дайджеста", "Digest time")
    fun digestBody(count: Int, time: String, room: String): String {
        val rooms = room.trim()
        val where = if (rooms.isBlank()) time else "$time, $rooms"
        return if (en) {
            val word = if (count == 1) "class" else "classes"
            "Tomorrow: $count $word. First at $where."
        } else {
            val word = when {
                count % 100 in 11..14 -> "пар"
                count % 10 == 1 -> "пара"
                count % 10 in 2..4 -> "пары"
                else -> "пар"
            }
            "Завтра $count $word. Первая в $where."
        }
    }
    val channelDigest = t("Дайджест", "Digest")
    val channelDigestHint = t("Кратко про пары на завтра", "A short note about tomorrow's classes")
    val otherBuildingHint = t("другой корпус, выйти заранее", "different building — leave early")
    val guestTitle = t("Вы без аккаунта", "You're a guest")
    val guestHint = t(
        "Без входа работают расписание, заметки, дела, пропуски и файлы. Вход нужен для чатов, отзывов и синхронизации между телефоном и сайтом.",
        "Without signing in you still get the schedule, notes, tasks, misses and files. Sign in for chats, reviews and syncing between your phone and the website.",
    )
    val guestSignIn = t("Войти", "Sign in")
    val exportData = t("Скачать мои данные", "Download my data")
    val exportDataHint = t(
        "Заметки, дела, пропуски, скрытые пары, избранное и настройки — один JSON-файл. Без рекламы в расписании: так и держим главный экран чистым; если появятся платные опции, они не попадут в ленту пар.",
        "Notes, tasks, misses, hidden classes, favorites and settings in one JSON file. No ads in the schedule: we keep the main screen clean; if paid options appear later, they will not mix into the class list.",
    )
    val noAdsNote = t(
        "Сейчас в ленте расписания нет рекламы и чужих вакансий. Если когда‑нибудь появятся платные опции — не вперемешку с парами.",
        "Right now the schedule feed has no ads or third-party jobs. If paid options appear later, they will not mix into your classes.",
    )
    val chatNotify = t("Уведомления о сообщениях", "Message notifications")
    val chatNotifyHint = t("Новые сообщения проверяются примерно раз в 15 минут", "New messages are checked about every 15 minutes")
    val chatNotifySignIn = t("Сначала войдите в аккаунт", "Sign in first")
    val ongoingLesson = t("Текущая пара в шторке", "Current class in notifications")
    val ongoingLessonHint = t(
        "Во время пары в уведомлениях видно предмет, аудиторию и сколько осталось",
        "During a class the notification shows the subject, room and time left",
    )
    val channelMessages = t("Сообщения", "Messages")
    val channelOngoing = t("Текущая пара", "Current class")
    fun newMessages(count: Int): String = if (en) {
        if (count == 1) "1 new message" else "$count new messages"
    } else {
        val word = when {
            count % 100 in 11..14 -> "новых сообщений"
            count % 10 == 1 -> "новое сообщение"
            count % 10 in 2..4 -> "новых сообщения"
            else -> "новых сообщений"
        }
        "$count $word"
    }
    fun untilTime(time: String): String = t("до $time", "until $time")

    val calendarSubTitle = t("Расписание в календаре", "Schedule in your calendar")
    val calendarSubHint = t(
        "Подписка обновляется сама, если расписание изменится. Заметки и скрытые пары туда не попадут.",
        "The subscription updates itself when the schedule changes. Notes and hidden classes are not included.",
    )
    val calendarGoogle = t("Google Календарь", "Google Calendar")
    val calendarOther = t("Другой календарь", "Other calendar")
    val copyLink = t("Скопировать ссылку", "Copy link")
    val linkCopied = t("Ссылка скопирована", "Link copied")

    val whatsNewLink = t("Что нового", "What's new")
    val whatsNewOk = t("Понятно", "Got it")
    fun whatsNewTitle(version: String) = t("Что нового в $version", "What's new in $version")
    val whatsNewItems: List<Pair<String, String>> = if (en) {
        listOf(
            "Chats" to "Group chat by invite code and direct messages. Messages written offline are sent later. Phones nearby can chat even without the internet.",
            "Pins, polls and group tasks" to "The class rep and helpers can pin a message, run a poll and post deadlines for the whole group.",
            "Account" to "Reviews, review votes and reports now need an account. Notes, tasks, misses, favorites and settings are the same on your phone and the website.",
            "Class rep" to "A class rep confirmed by a moderator gets a badge and helper rights in every chat of their group. Send a request in your profile.",
            "Reviews" to "Like or dislike other people's reviews and report rule-breakers.",
            "Where is the teacher" to "The teacher page shows their current class and room, or the next one.",
            "Notifications" to "Message notifications and the current class with a countdown in the notification shade.",
            "Calendar and widget" to "Subscribe to your schedule in Google Calendar and add the new week widget.",
            "Settings" to "Theme that follows the system, a swear-word filter and full English.",
        )
    } else {
        listOf(
            "Чаты" to "Чат группы по коду приглашения и личные сообщения. Написанное без сети отправится позже. Рядом стоящие телефоны могут переписываться даже без интернета.",
            "Закреп, опросы и дела группы" to "Староста и помощники могут закрепить сообщение, провести опрос и выложить дедлайны для всей группы.",
            "Аккаунт" to "Отзывы, оценки отзывов и жалобы теперь с аккаунтом. Заметки, дела, пропуски, избранное и настройки одинаковые на телефоне и на сайте.",
            "Староста" to "Подтверждённый модератором староста получает значок и права помощника во всех чатах своей группы. Заявка — в профиле.",
            "Отзывы" to "Лайки и дизлайки к чужим отзывам и жалобы на нарушителей.",
            "Где преподаватель" to "На странице преподавателя видно, на какой он паре и в какой аудитории, или когда следующая.",
            "Уведомления" to "Уведомления о сообщениях и текущая пара с обратным отсчётом в шторке.",
            "Календарь и виджет" to "Подписка на расписание в Google Календаре и новый виджет на всю неделю.",
            "Настройки" to "Тема как в системе, фильтр мата и полный английский перевод.",
        )
    }

    fun updateTitle(version: String) = t("Вышла версия $version", "Version $version is out")
    val updateText = t("Скачайте и установите поверх — все данные сохранятся.", "Download and install it over this one — your data stays.")
    val updateDownload = t("Скачать", "Download")
    val updateLater = t("Позже", "Later")

    val whereTitle = t("Где сейчас", "Where now")
    val whereInClass = t("Сейчас на паре", "In class now")
    val whereFree = t("Сейчас без пар", "No class right now")
    val whereNext = t("Ближайшая пара", "Next class")
    val whereNone = t("В ближайшие две недели пар нет", "No classes in the next two weeks")
    fun groupsLine(groups: List<String>) = (if (groups.size == 1) t("Группа ", "Group ") else t("Группы ", "Groups ")) + groups.joinToString(", ")

    val moreActions = t("Ещё", "More")
    val pinned = t("Закреплено", "Pinned")
    val pin = t("Закрепить", "Pin")
    val unpin = t("Открепить", "Unpin")
    val members = t("Участники", "Members")
    val roleOwner = t("создатель", "creator")
    val roleRep = t("староста", "class rep")
    val roleHelper = t("помощник", "helper")
    val makeHelper = t("Сделать помощником", "Make helper")
    val removeHelper = t("Убрать из помощников", "Remove helper")
    val helpersHint = t(
        "Создатель чата, помощники и подтверждённый староста группы могут закреплять сообщения, создавать опросы и дела группы.",
        "The chat creator, helpers and the confirmed class rep can pin messages and create polls and group tasks.",
    )
    val polls = t("Опросы", "Polls")
    val newPoll = t("Новый опрос", "New poll")
    val pollQuestion = t("Вопрос", "Question")
    fun pollOption(n: Int) = t("Вариант $n", "Option $n")
    val addOption = t("Ещё вариант", "Add option")
    val pollMultiple = t("Можно выбрать несколько", "Allow several answers")
    val pollCreate = t("Создать опрос", "Create poll")
    val pollNeedOptions = t("Нужен вопрос и хотя бы два варианта", "Add a question and at least two options")
    val pollClose = t("Завершить", "End poll")
    val pollClosed = t("Опрос завершён", "Poll ended")
    val pollDelete = t("Удалить опрос", "Delete poll")
    val vote = t("Голосовать", "Vote")
    val noPolls = t("Опросов пока нет", "No polls yet")
    fun pollVotes(count: Int): String = if (en) {
        if (count == 1) "1 vote" else "$count votes"
    } else {
        val word = when {
            count % 100 in 11..14 -> "голосов"
            count % 10 == 1 -> "голос"
            count % 10 in 2..4 -> "голоса"
            else -> "голосов"
        }
        "$count $word"
    }
    val groupTasks = t("Дела группы", "Group tasks")
    val groupTasksHint = t(
        "Дедлайны от старосты. Добавьте дело к себе, чтобы получить напоминание.",
        "Deadlines from the class rep. Add one to your tasks to get a reminder.",
    )
    val newGroupTask = t("Новое дело группы", "New group task")
    val groupTaskTitle = t("Что сделать", "What to do")
    val groupTaskNote = t("Подробности (необязательно)", "Details (optional)")
    val pickDate = t("Срок", "Due date")
    val pickTime = t("Время", "Time")
    val noDate = t("Без срока", "No due date")
    val addToMine = t("В мои дела", "Add to my tasks")
    val addedToMine = t("Добавлено в ваши дела", "Added to your tasks")
    val inMine = t("Уже в ваших делах", "Already in your tasks")
    val noGroupTasks = t("Дел группы пока нет", "No group tasks yet")
    val add = t("Добавить", "Add")
    fun fromChat(title: String) = t("Из чата «$title»", "From “$title”")

    val adminLog = t("Журнал", "Log")
    fun adminAction(action: String): String = when (action) {
        "review.hide" -> t("Скрыт отзыв", "Review hidden")
        "review.unhide" -> t("Возвращён отзыв", "Review restored")
        "review.delete" -> t("Удалён отзыв", "Review deleted")
        "ban.review" -> t("Бан по отзыву", "Ban for a review")
        "ban.message" -> t("Бан по сообщению", "Ban for a message")
        "ban.user" -> t("Ограничение аккаунта", "Account restricted")
        "ban.remove" -> t("Снят бан", "Ban lifted")
        "message.delete" -> t("Удалено сообщение", "Message deleted")
        "report.dismiss" -> t("Отклонена жалоба", "Report dismissed")
        "report.accept" -> t("Приняты меры по жалобе", "Action taken on a report")
        "announce.create" -> t("Опубликовано объявление", "Announcement published")
        "announce.remove" -> t("Снято объявление", "Announcement removed")
        "user.delete" -> t("Удалён аккаунт", "Account deleted")
        "rep.approve" -> t("Подтверждён староста", "Class rep approved")
        "rep.reject" -> t("Отклонена заявка на старосту", "Class rep request declined")
        "rep.revoke" -> t("Снят староста", "Class rep revoked")
        else -> action
    }
}

val MoreRu = MoreCopy(false)
val MoreEn = MoreCopy(true)
