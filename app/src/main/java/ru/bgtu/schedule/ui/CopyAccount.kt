package ru.bgtu.schedule.ui

import java.time.Instant
import java.time.ZoneId

/** Texts for account-bound reviews, restrictions, class reps and sync. Same wording as the web app. */
class AccountCopy(private val en: Boolean) {
    private fun t(ru: String, en: String) = if (this.en) en else ru

    private val months = if (en) {
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    } else {
        listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    }

    val later = t("Позже", "Later")
    val needAccountTitle = t("Нужен аккаунт", "Account needed")
    val needAccountText = t(
        "Отзывы, оценки отзывов и жалобы доступны после входа. Так мы защищаемся от накруток и можем ограничивать нарушителей.",
        "Reviews, review votes and reports need an account. This protects ratings from cheating and lets moderators restrict rule breakers.",
    )
    val socialRequired = t(
        "Отзывы можно оставлять, если вы вошли через Telegram или Google. Привяжите один из них в аккаунте.",
        "To leave reviews, sign in with Telegram or Google. Link one of them in your account.",
    )
    val deviceUsed = t(
        "С этого устройства уже оставлен отзыв этому преподавателю из другого аккаунта.",
        "A review of this teacher was already left from this device by another account.",
    )
    val muted = t("Вам запрещено писать в чатах", "You are not allowed to write in chats")
    val noCreate = t("Вам запрещено создавать чаты и писать новым людям", "You are not allowed to create chats or message new people")
    val alreadyRep = t("Вы уже староста этой группы", "You are already the class rep of this group")
    val restrictionsTitle = t("Ограничения", "Restrictions")
    val loginOfferTitle = t("Войдите в аккаунт", "Sign in")
    val loginOfferText = t(
        "С аккаунтом можно оставлять отзывы и писать в чатах, а заметки, дела, пропуски, избранное и настройки будут одинаковыми на телефоне и на сайте.",
        "With an account you can leave reviews and chat, and your notes, tasks, misses, favorites and settings stay the same on your phone and the website.",
    )
    val mineHiddenUnclaimed = t(
        "Отзыв скрыт, потому что не был привязан к аккаунту. Войдите, чтобы вернуть его.",
        "This review is hidden because it was never linked to an account. Sign in to bring it back.",
    )
    val deleteReviewsQuestion = t("Что сделать с вашими отзывами о преподавателях?", "What should happen to your teacher reviews?")
    val deleteWithReviews = t("Удалить вместе с аккаунтом", "Delete them with the account")
    val keepReviewsAnon = t("Оставить без имени", "Keep them without a name")
    val myReviews = t("Мои отзывы", "My reviews")
    val noMyReviews = t("Вы ещё не оставляли отзывов", "You haven't left any reviews yet")
    val hiddenReview = t("скрыт", "hidden")
    val devices = t("Мои устройства", "My devices")
    val thisDevice = t("это устройство", "this device")
    val unknownDevice = t("Устройство", "Device")
    val repTitle = t("Староста группы", "Class rep")
    val repHint = t(
        "Подтверждённый староста получает значок и права помощника во всех чатах своей группы. Заявку проверит модератор.",
        "A confirmed class rep gets a badge and helper rights in every chat of their group. A moderator checks the request.",
    )
    val repRequest = t("Я староста", "I'm the class rep")
    val repComment = t("Комментарий для модератора: ФИО, как проверить (необязательно)", "Note for the moderator: full name, how to check (optional)")
    val repSend = t("Отправить заявку", "Send request")
    val repRejected = t("Заявка отклонена. Можно отправить новую.", "The request was declined. You can send a new one.")
    val repCancel = t("Отозвать заявку", "Withdraw request")
    val repStepDown = t("Перестать быть старостой", "Stop being class rep")
    val repNoGroup = t("Сначала выберите группу в расписании", "Pick your group in the schedule first")
    val syncTitle = t("Синхронизация", "Sync")
    val syncHint = t(
        "Заметки, дела, пропуски, скрытые пары, избранные преподаватели, группа и настройки одинаковые на всех устройствах с этим аккаунтом.",
        "Notes, tasks, misses, hidden classes, favorite teachers, group and settings are the same on every device with this account.",
    )
    val syncNow = t("Синхронизировать", "Sync now")
    val syncFailed = t("Не удалось синхронизировать. Попробуем позже.", "Couldn't sync. We'll try again later.")
    val syncAskTitle = t("Объединить данные?", "Combine data?")
    val syncAskText = t(
        "На этом устройстве и в аккаунте разные заметки, дела или другие данные. Что оставить?",
        "This device and your account have different notes, tasks or other data. What should stay?",
    )
    val syncMerge = t("Объединить", "Combine")
    val syncMergeHint = t(
        "Сохранится всё. Если одно и то же изменено и там, и тут, останется вариант с этого устройства.",
        "Everything stays. If the same item was changed in both places, this device's version wins.",
    )
    val syncTakeAccount = t("Взять из аккаунта", "Use the account's data")
    val syncTakeAccountHint = t("Данные на этом устройстве заменятся данными из аккаунта.", "Data on this device is replaced with the account's data.")
    val syncKeepDevice = t("Оставить с устройства", "Use this device's data")
    val syncKeepDeviceHint = t("Данные в аккаунте заменятся данными с этого устройства.", "The account's data is replaced with this device's data.")

    /** "5 ноября" or "5 ноября, 14:30" from an ISO timestamp, in local time. */
    fun moment(iso: String, withTime: Boolean = false): String {
        val at = runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()) }.getOrNull() ?: return iso
        val day = if (en) "${months[at.monthValue - 1]} ${at.dayOfMonth}" else "${at.dayOfMonth} ${months[at.monthValue - 1]}"
        if (!withTime) return day
        return "$day, %02d:%02d".format(at.hour, at.minute)
    }

    fun scopeAction(scope: String): String = when (scope) {
        "reviews" -> t("оставлять отзывы", "leave reviews")
        "votes" -> t("оценивать отзывы", "vote on reviews")
        "reports" -> t("отправлять жалобы", "send reports")
        "chat" -> t("писать в чатах", "write in chats")
        "chat_create" -> t("создавать чаты и писать новым людям", "create chats or message new people")
        else -> t("пользоваться аккаунтом", "use your account")
    }

    /** "Вам запрещено оставлять отзывы до 5 ноября, 14:30. Причина: …" */
    fun restrictionText(scope: String, until: String?, reason: String, banned: String): String {
        val head = if (scope == "all") banned else t("Вам запрещено ${scopeAction(scope)}", "You are not allowed to ${scopeAction(scope)}")
        val whenText = if (until != null) t(" до ${moment(until, true)}", " until ${moment(until, true)}") else t(" навсегда", " permanently")
        val why = if (reason.isNotBlank()) t(". Причина: $reason", ". Reason: $reason") else ""
        return "$head$whenText$why"
    }

    fun accountTooNew(until: String) = t(
        "Аккаунт новый: отзывы можно оставлять с ${moment(until, true)}",
        "Your account is new: you can leave reviews from ${moment(until, true)}",
    )

    fun legacyWarn(until: String?): String {
        val tail = until?.let { t(" Иначе ${moment(it)} их скроют.", " Otherwise they will be hidden on ${moment(it)}.") }.orEmpty()
        return t(
            "С этого устройства оставлены отзывы без аккаунта. Войдите, чтобы они остались за вами.$tail",
            "Reviews were left from this device without an account. Sign in to keep them yours.$tail",
        )
    }

    fun legacyMine(until: String?): String {
        val tail = until?.let { t(", иначе ${moment(it)} его скроют", ", otherwise it will be hidden on ${moment(it)}") }.orEmpty()
        return t("Этот отзыв ещё не привязан к аккаунту. Войдите$tail.", "This review isn't linked to an account yet. Sign in$tail.")
    }

    val banWhat = t("Что запретить", "What to restrict")
    fun scopeLabel(scope: String): String = when (scope) {
        "all" -> t("Всё (полный бан)", "Everything (full ban)")
        "reviews" -> t("Отзывы", "Reviews")
        "votes" -> t("Оценки отзывов", "Review votes")
        "reports" -> t("Жалобы", "Reports")
        "chat" -> t("Писать в чатах", "Writing in chats")
        "chat_create" -> t("Новые чаты", "New chats")
        else -> scope
    }
    val restrict = t("Ограничить", "Restrict")
    val restrictedMark = t("ограничен", "restricted")
    val adminReps = t("Старосты", "Class reps")
    val repApprove = t("Подтвердить", "Approve")
    val repReject = t("Отклонить", "Decline")
    val repRevoke = t("Снять", "Revoke")
    val repPendingMark = t("заявка", "request")
    val statPendingReps = t("Заявок на старосту", "Class rep requests")
    val personDevices = t("Входов на устройствах", "Signed-in devices")
    val personReportsSent = t("Отправил жалоб", "Reports sent")
    val personReviews = t("Отзывы", "Reviews")
    val personMessages = t("Сообщения", "Messages")
    val personBans = t("Ограничения и баны", "Restrictions and bans")
    val personOpen = t("Карточка", "Profile")
    val inactiveMark = t("истёк", "expired")

    fun repPending(group: String) = t("Заявка на старосту группы $group на проверке", "Your class rep request for group $group is being checked")
    fun repApproved(group: String) = t("Вы староста группы $group", "You are the class rep of group $group")
    fun deviceSeen(iso: String) = t("был активен ${moment(iso, true)}", "active ${moment(iso, true)}")
    fun syncDone(iso: String) = t("Синхронизировано ${moment(iso, true)}", "Synced ${moment(iso, true)}")
}

val AccountRu = AccountCopy(false)
val AccountEn = AccountCopy(true)
