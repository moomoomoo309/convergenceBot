package convergence.discord.calendar

import com.github.caldav4j.CalDAVCollection
import com.github.caldav4j.CalDAVConstants
import com.github.caldav4j.exceptions.CalDAV4JException
import com.github.caldav4j.methods.HttpPropFindMethod
import com.github.caldav4j.model.request.CalendarQuery
import com.github.caldav4j.model.request.CompFilter
import com.github.caldav4j.model.request.TimeRange
import com.github.caldav4j.util.GenerateQuery
import convergence.*
import convergence.discord.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.ScheduledEvent
import net.dv8tion.jda.api.requests.restaction.ScheduledEventAction
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Status
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.jackrabbit.webdav.property.DavPropertyName
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture

val base64Encoder: Base64.Encoder = Base64.getEncoder()

const val UID_LENGTH = 36
const val DAYS = 30L

internal val uidRegex = Regex("[a-z0-9-]{36}")

data class EventInstance(
    val vevent: VEvent,
    val start: Date,
    val end: Date
) {
    val uid: String? get() = vevent.uid?.value
    val summary: String? get() = vevent.summary?.value
}

interface CalendarEvent {
    val name: String
    val description: String
    val start: Instant
    val end: Instant?
    val uid: String?
    val location: String
}

data class VEventWrapper(val eventInstance: EventInstance): CalendarEvent {
    override val name: String get() = eventInstance.summary?.trim() ?: "Unnamed event"
    override val description: String get() = eventInstance.vevent.description?.value ?: ""
    override val start: Instant get() = eventInstance.start.toInstant()
    override val end: Instant get() = eventInstance.end.toInstant()
    override val uid: String? get() = eventInstance.uid
    override val location: String
        get() = eventInstance.vevent.location?.value?.takeUnless { it.isBlank() } ?: "No Location"

    override fun equals(other: Any?): Boolean {
        return this === other || when(other) {
            is VEventWrapper -> name == other.name &&
                    start == other.start &&
                    end == other.end &&
                    description == other.description

            is CalendarEvent -> name == other.name &&
                    start == other.start &&
                    end == other.end

            else -> false
        }
    }

    override fun hashCode(): Int = Objects.hash(name, start, end)
}

data class DiscordEventWrapper(val event: ScheduledEvent): CalendarEvent {
    override val name: String get() = event.name
    override val description: String get() = event.description ?: ""
    override val start: Instant get() = event.startTime.toInstant()
    override val end: Instant? get() = event.endTime?.toInstant()
    override val uid: String?
        get() = event.description?.takeLast(UID_LENGTH)?.trim()?.takeIf { uidRegex.matches(it) }
    override val location: String get() = event.location

    override fun equals(other: Any?): Boolean {
        return this === other || when(other) {
            is DiscordEventWrapper -> name == other.name && description == other.description &&
                    start == other.start && end == other.end

            is VEventWrapper -> name == other.name && start == other.start && end == other.end
            else -> false
        }
    }

    override fun hashCode(): Int = Objects.hash(name, start, end)
}

class CalendarProcessorService(
    private val messaging: MessagingService,
    private val commandRegistry: CommandRegistryService,
    private val notificationProcessor: CalendarNotificationProcessorService
) {
    init {
        System.setProperty(
            "net.fortuna.ical4j.timezone.cache.impl",
            "net.fortuna.ical4j.util.MapTimeZoneCache"
        )
    }

    private val calendarCache = hashMapOf<String, CalDAVCollection>()
    val calendarNames = mutableMapOf<CalDAVCollection, String>()
    private val httpClient: CloseableHttpClient by lazy {
        HttpClients.custom().setDefaultHeaders(
            listOf(
                BasicHeader(
                    "Authorization",
                    "Basic ${base64Encoder.encodeToString(("bot:" + (nextcloudPassword ?: "")).toByteArray())}"
                )
            )
        ).build()
    }
    private var lastCalendarUpdateTime = Instant.now().plusSeconds(10)
    private val calendarUpdateFrequency = java.time.Duration.ofMinutes(15L)

    fun getAndCacheCalendar(url: String): CalDAVCollection? {
        calendarCache[url]?.let { return it }
        val cal = CalDAVCollection(url)
        try {
            cal.testConnection(httpClient)
        } catch(e: CalDAV4JException) {
            discordLogger.error("Could not connect to calendar! Exception: ", e)
            return null
        }
        calendarCache[url] = cal
        try {
            val propNameSet = DavPropertyNameSet()
            propNameSet.add(DavPropertyName.DISPLAYNAME)
            val propFindMethod = HttpPropFindMethod(url, propNameSet, CalDAVConstants.DEPTH_0)
            val response = httpClient.execute(propFindMethod)
            val displayName = propFindMethod.getDisplayName(response, url)
            if (displayName.isNotBlank()) {
                calendarNames[cal] = displayName
            }
        } catch(e: Exception) {
            discordLogger.warn("Could not retrieve display name for calendar at $url: ${e.message}")
        }
        return cal
    }

    fun validateCalDAVUrl(url: String): String? {
        if (url.contains("/public-calendars/")) {
            return "This is a public calendar URL. Public calendars strip alarms (VALARM) and other data. " +
                    "Please use an authenticated CalDAV URL instead (e.g. /remote.php/dav/calendars/bot/<calname>/)."
        }
        return null
    }

    fun buildCalendarQuery(now: Instant): CalendarQuery {
        val calendarQuery = GenerateQuery().generate()
        val calFilter = CompFilter("VCALENDAR")
        val eventFilter = CompFilter("VEVENT")
        eventFilter.timeRange = TimeRange(now.toIDate(), now.plus(DAYS, ChronoUnit.DAYS).toIDate())
        calFilter.addCompFilter(eventFilter)
        calendarQuery.compFilter = calFilter
        return calendarQuery
    }

    fun getCalDAVEventsNextDays(calendars: List<Calendar>): List<EventInstance> {
        val now = Instant.now()
        val periodWindow = Period(now.toIDate(), now.plus(DAYS, ChronoUnit.DAYS).toIDate())

        val masterEvents = mutableMapOf<String, VEvent>()
        val exceptions = mutableMapOf<String, MutableList<VEvent>>()

        // Separate master events and exceptions
        for (cal in calendars) {
            val events = cal.getComponents<VEvent>("VEVENT")
            for (event in events) {
                val uid = event.uid.value
                if (event.recurrenceId == null) {
                    masterEvents[uid] = event
                } else {
                    exceptions.computeIfAbsent(uid) { mutableListOf() }.add(event)
                }
            }
        }

        val events = mutableListOf<EventInstance>()

        for ((uid, master) in masterEvents) {
            val occurrences = master.calculateRecurrenceSet(periodWindow)

            val exList = exceptions[uid] ?: emptyList()
            for (ex in exList) {
                val rid = DateTime(ex.recurrenceId.date)

                val status = ex.getProperty<Status>("STATUS")?.value
                if (status == "CANCELLED") {
                    // Remove canceled occurrence
                    occurrences.removeIf { it.start == rid }
                } else {
                    // Override: replace original occurrence with this one
                    occurrences.removeIf { it.start == rid }
                    occurrences.add(Period(DateTime(ex.startDate.date), DateTime(ex.endDate.date)))
                }
            }

            for (period in occurrences) {
                events.add(
                    EventInstance(
                        vevent = master,
                        start = period.start,
                        end = period.end
                    )
                )
            }
        }

        return events.sortedBy { it.start }
    }

    private fun getDiscordEventsNextDays(jda: JDA, guildId: Long): List<DiscordEventWrapper> {
        return jda.scheduledEvents.filter {
            it.startTime.isAfter(OffsetDateTime.now())
                    && it.startTime.isBefore(OffsetDateTime.now().plus(DAYS, ChronoUnit.DAYS))
                    && it.guild.idLong == guildId
        }.sortedBy { it.startTime }.map { DiscordEventWrapper(it) }
    }

    fun syncCommand(args: List<String>, chat: Chat): String {
        if (args.size != 1)
            return "Expected 1 argument, got ${args.size}."
        if (chat !is DiscordChat)
            return "Can only be ran on a Discord chat. How did you run this on a different chat?"
        if (getAndCacheCalendar(args[0]) == null)
            return "No calendar found at URL \"${args[0]}\"."
        val syncedCalendar = SyncedCalendar(chat.channel.guild.idLong, args[0])
        if (syncedCalendar in settings.syncedCalendars)
            return "That calendar is already synced!"
        settings.syncedCalendars.add(syncedCalendar)
        updateSettings()
        return syncToDiscord(settings.syncedCalendars.filter { it.guildId == chat.channel.guild.idLong })
    }

    private fun queryCalendarWithRetry(calCollection: CalDAVCollection): List<Calendar> {
        var queriedCals = calCollection.queryCalendars(httpClient, buildCalendarQuery(Instant.now()))
        // ical4j's timezone registry may not be fully populated on the first query, causing some
        // calendar objects to fail parsing (returning null). If that happens, retry once — the first
        // query will have warmed up the registry so the second succeeds.
        if (queriedCals.any { it == null })
            queriedCals = calCollection.queryCalendars(httpClient, buildCalendarQuery(Instant.now()))
        return queriedCals.filterNotNull()
    }

    fun syncToDiscord(cals: List<SyncedCalendar>, dry: Boolean = false): String {
        val calendarEvents = mutableListOf<VEventWrapper>()
        val firstGuildId = cals.first().guildId
        for ((currentGuildId, calURL) in cals) {
            val calCollection = getAndCacheCalendar(calURL) ?: return "No calendar found at URL \"$calURL\"."
            calendarEvents.addAll(getCalDAVEventsNextDays(queryCalendarWithRetry(calCollection))
                .map { VEventWrapper(it) })
            if (currentGuildId != firstGuildId)
                return "The guild IDs of each calendar don't match!"
        }
        return syncToDiscord(
            jda.getGuildById(firstGuildId) ?: return "No guild found with ID $firstGuildId.",
            calendarEvents,
            getDiscordEventsNextDays(jda, firstGuildId),
            dry
        )
    }

    private fun syncToDiscord(
        guild: Guild,
        calEvents: List<VEventWrapper>,
        discordEvents: List<DiscordEventWrapper>,
        dry: Boolean = false
    ): String {
        val futures = getFutures(calEvents, discordEvents, guild)

        // Do not add calendar events which already exist on discord
        removeDuplicateEvents(futures, discordEvents)

        // Remove invalid events
        val invalidEvents = removeInvalidEvents(discordEvents, calEvents, dry)

        // Remove duplicate discord events
        val duplicateEvents = removeDuplicateDiscordEvents(discordEvents, dry)

        // Add all the events simultaneously, adding them to the uidMap after, then wait for all of them to complete.
        if (!dry) {
            CompletableFuture.allOf(*futures.map {
                it.second.submit()
            }.toTypedArray()).join()
        }

        return "Added: ${futures.joinToString(", ") { it.first.name }}\n" +
                "Invalid: ${invalidEvents.joinToString(", ") { it.name }}\n" +
                "Duplicates: ${duplicateEvents.joinToString(", ") { it.name }}"
    }

    private fun getFutures(
        calEvents: List<VEventWrapper>,
        discordEvents: List<DiscordEventWrapper>,
        guild: Guild
    ): MutableList<Pair<VEventWrapper, ScheduledEventAction>> {
        val futures = mutableListOf<Pair<VEventWrapper, ScheduledEventAction>>()
        outer@ for (wrapper in calEvents) {
            for (discordEvent in discordEvents)
                if (wrapper == discordEvent)
                    continue@outer
            addCalendarEventToDiscord(guild, wrapper)?.let {
                futures.add(it)
            }
        }
        return futures
    }

    private fun removeInvalidEvents(
        discordEvents: List<DiscordEventWrapper>,
        calEvents: List<VEventWrapper>,
        dry: Boolean
    ): List<DiscordEventWrapper> {
        val removed = mutableListOf<DiscordEventWrapper>()
        outer@ for (discordWrapper in discordEvents) {
            val uid = discordWrapper.uid ?: continue
            for (calEvent in calEvents.filter { it.uid == uid })
                if (calEvent == discordWrapper)
                    continue@outer
            removed.add(discordWrapper)
            if (!dry)
                discordWrapper.event.delete().queue()
        }
        return removed
    }

    private fun removeDuplicateEvents(
        futures: MutableList<Pair<VEventWrapper, ScheduledEventAction>>,
        discordEvents: List<DiscordEventWrapper>
    ) {
        val futureIter = futures.iterator()
        while (futureIter.hasNext()) {
            val (wrapper, _) = futureIter.next()
            if (discordEvents.any { it == wrapper }) {
                futureIter.remove()
            }
        }
    }

    private fun removeDuplicateDiscordEvents(
        discordEvents: List<DiscordEventWrapper>,
        dry: Boolean
    ): List<DiscordEventWrapper> {
        val toRemove = mutableListOf<DiscordEventWrapper>()
        for (i in discordEvents.indices) {
            for (i2 in discordEvents.indices) {
                if (i2 == i)
                    continue
                if (discordEvents[i] == discordEvents[i2]) {
                    toRemove.add(discordEvents[i])
                    break
                }
            }
        }
        for ((event) in toRemove) {
            if (!dry)
                event.delete().queue()
        }
        return toRemove
    }

    private fun addCalendarEventToDiscord(
        guild: Guild,
        wrapper: VEventWrapper
    ): Pair<VEventWrapper, ScheduledEventAction>? {
        return if (wrapper.start < Instant.now())
            null
        else
            Pair(
                wrapper,
                guild.createScheduledEvent(
                    wrapper.name,
                    wrapper.location,
                    wrapper.start.toOffsetDateTime(),
                    wrapper.end.toOffsetDateTime()
                ).setDescription(addUIDToDescription(wrapper.description, wrapper.uid ?: ""))
            )
    }

    internal fun addUIDToDescription(description: String, uid: String): String {
        if (description.isEmpty()) {
            return uid
        } else if (description.endsWith(uid)) {
            return description
        }
        return description + "\n" + uid
    }

    fun onUpdate() {
        if ((lastCalendarUpdateTime + calendarUpdateFrequency).isBefore(Instant.now())) {
            syncAllCalendars()
            scheduleAllNotifications()
        }
    }

    /**
     * Schedules notifications for all registered notification channels, independent of Discord sync.
     * This ensures notifications work even for calendars not synced to Discord events.
     */
    fun scheduleAllNotifications() {
        val calURLs = settings.notificationChannels.map { it.calURL }.toSet()
        for (calURL in calURLs) {
            val calCollection = getAndCacheCalendar(calURL) ?: continue
            val eventInstances = getCalDAVEventsNextDays(queryCalendarWithRetry(calCollection))
            val channelsForCalendar = settings.notificationChannels.filter { it.calURL == calURL }
            notificationProcessor.scheduleNotificationsForEvents(eventInstances, channelsForCalendar)
        }
    }

    fun syncCalendars(chat: DiscordChat, dry: Boolean = false): String {
        Thread {
            messaging.sendMessage(
                chat,
                syncToDiscord(settings.syncedCalendars.filter { it.guildId == chat.server.guild.idLong }, dry)
            )
        }.start()
        lastCalendarUpdateTime = Instant.now()
        return "Resyncing calendars..."
    }

    fun syncAllCalendars(chat: DiscordChat? = null, dry: Boolean = false): String {
        Thread {
            val serverIDs = settings.syncedCalendars.map { it.guildId }.toSet()
            for (serverId in serverIDs)
                syncToDiscord(settings.syncedCalendars.filter { it.guildId == serverId }, dry)
            if (chat != null)
                messaging.sendMessage(
                    chat,
                    syncToDiscord(settings.syncedCalendars.filter { it.guildId == chat.server.guild.idLong }, dry)
                )
        }.start()
        lastCalendarUpdateTime = Instant.now()
        return "Resyncing calendars..."
    }
    @SuppressWarnings("LongMethod")
    fun registerCalendarCommands() {
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "syncCalendar",
                listOf(ArgumentSpec("URL", ArgumentType.STRING)),
                ::syncCommand,
                "Sync a CalDAV calendar to discord events.",
                "syncCalendar (URL)"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "resyncCalendar",
                listOf(),
                { _, chat -> syncCalendars(chat as DiscordChat) },
                "Resyncs all calendars in this server.",
                "resyncCalendar (Takes no parameters)"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "dryResyncCalendar",
                listOf(),
                { _, chat -> syncCalendars(chat as DiscordChat, true) },
                "Tries to resyncs all calendars in this server, but does not actually add or remove anything.",
                "dryResyncCalendar (Takes no parameters)"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "resyncAllCalendars",
                listOf(),
                { _, chat -> syncAllCalendars(chat as DiscordChat) },
                "Resyncs all calendars in all servers.",
                "resyncAllCalendars (Takes no parameters)"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "dryResyncAllCalendars",
                listOf(),
                { _, chat -> syncAllCalendars(chat as DiscordChat, true) },
                "Tries to resync all calendars in all servers, but does not actually add or remove anything.",
                "dryResyncAllCalendars (Takes no parameters)"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "unsyncCalendar",
                listOf(ArgumentSpec("URL", ArgumentType.STRING)),
                { args, chat ->
                    val removed = settings.syncedCalendars.remove(settings.syncedCalendars.firstOrNull {
                        it.guildId == (chat as DiscordChat).server.guild.idLong && it.calURL == args[0]
                    })
                    if (removed) "Calendar removed." else "No calendar with URL ${args[0]} found."
                },
                "Removes a synced calendar.",
                "unsyncCalendar (URL)"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "syncedCalendars",
                listOf(),
                { _, chat ->
                    settings.syncedCalendars.filter {
                        it.guildId == (chat as DiscordChat).server.guild.idLong
                    }.joinToString(", ").ifEmpty { "No calendars are synced in this chat." }
                },
                "Prints out all the synced calendars in this chat.",
                "syncedCalendars (Takes no parameters)"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "addCalendarNotification",
                listOf(
                    ArgumentSpec("URL", ArgumentType.STRING),
                    ArgumentSpec("mention", ArgumentType.STRING, optional = true),
                    ArgumentSpec("pattern", ArgumentType.STRING, optional = true)
                ),
                ::addCalendarNotificationCommand,
                "Registers this channel to receive event notifications from a CalDAV calendar.",
                "addCalendarNotification (URL) [mention @user] [regex pattern to filter events to be mentioned for]"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "removeCalendarNotification",
                listOf(
                    ArgumentSpec("URL", ArgumentType.STRING),
                    ArgumentSpec("mention", ArgumentType.STRING, optional = true)
                ),
                ::removeCalendarNotificationCommand,
                "Removes notification registration for a CalDAV calendar.",
                "removeCalendarNotification (URL) [mention]"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "listCalendarNotifications",
                listOf(),
                { _, chat ->
                    settings.notificationChannels.filter {
                        it.guildId == (chat as DiscordChat).server.guild.idLong
                    }.joinToString("\n").ifEmpty { "No calendar notifications are registered in this server." }
                },
                "Lists all registered calendar notification channels in this server.",
                "listCalendarNotifications (Takes no arguments)"
            )
        )
        commandRegistry.registerCommand(
            Command.of(
                DiscordProtocol,
                "syncCalendarNotifications",
                listOf(),
                { ->
                    scheduleAllNotifications()
                    "Notifications synced."
                },
                "Manually syncs calendar notifications.",
                "syncCalendarNotifications (Takes no arguments)"
            )
        )
    }

    private fun addCalendarNotificationCommand(args: List<String>, chat: Chat): String {
        if (chat !is DiscordChat)
            return "This command can only be run on Discord."
        if (args.isEmpty())
            return "Please provide a CalDAV URL."

        val calURL = args[0]
        validateCalDAVUrl(calURL)?.let { return it }
        val mentionStr = args.getOrNull(1)
        val pattern = args.getOrNull(2)

        // Validate the calendar URL exists
        val cal = getAndCacheCalendar(calURL) ?: return "No calendar found at URL \"$calURL\"."
        val calName = calendarNames[cal]!!

        val guildId = chat.server.guild.idLong
        val channelId = chat.channel.idLong

        // Check if already registered
        val existing = settings.notificationChannels.firstOrNull {
            it.guildId == guildId && it.calURL == calURL && it.channelId == channelId
        }

        if (mentionStr == null) {
            // Error if it's already registered
            if (existing != null)
                return "This channel is already registered for notifications from that calendar."
            settings.notificationChannels.add(CalendarNotificationChannel(guildId, channelId, calURL, mutableMapOf()))
            return "Registered this channel to receive notifications from calendar \"$calName\"."
        }

        val mentionUserId = getIdFromMention(mentionStr) ?: return "Invalid @ for mentions."
        // Add a new channel, or add the mention to the existing one
        if (existing == null) {
            val mentions = mutableMapOf(mentionUserId to (pattern ?: ""))
            settings.notificationChannels.add(CalendarNotificationChannel(guildId, channelId, calURL, mentions))
        } else
            existing.mentions[mentionUserId] = pattern ?: ""
        updateSettings()
        // Format the response text
        val user = DiscordProtocol.getUser(mentionUserId)
        val mentionText = " (mentioning ${user?.getNickname(chat)}"
        val filterText = if (pattern != null)
            " on messages that match the regular expression `$pattern`"
        else
            ""
        return "Registered this channel to receive notifications from calendar \"$calName\"$mentionText$filterText)."
    }

    private fun removeCalendarNotificationCommand(args: List<String>, chat: Chat): String {
        if (chat !is DiscordChat)
            return "This command can only be run on Discord."
        if (args.isEmpty())
            return "Please provide a CalDAV URL."

        val calURL = args[0]
        val mentionStr = args.getOrNull(1)
        val guildId = chat.server.guild.idLong
        val channelId = chat.channel.idLong

        // Parse mention if provided
        val mentionUserId = getIdFromMention(mentionStr)

        val channel = settings.notificationChannels.firstOrNull {
            it.guildId == guildId && it.calURL == calURL && it.channelId == channelId
        }

        return if (mentionUserId == null)
        // Remove the channel notification entirely
            if (settings.notificationChannels.remove(channel)) {
                updateSettings()
                "Notification registration removed."
            } else
                "No notification registration found for URL: $calURL"
        else
        // Remove the user being mentioned from the notification
            if (channel?.mentions?.remove(mentionUserId) != null) {
                updateSettings()
                "Notification mention removed."
            } else
                "That user is not mentioned in this channel!"
    }
}

val defaultZoneOffset: ZoneOffset = OffsetDateTime.now().offset

fun Instant.toOffsetDateTime(): OffsetDateTime = this.atOffset(defaultZoneOffset)
fun Instant.toIDate(): DateTime {
    return DateTime(Date.from(this))
}

fun getIdFromMention(mentionStr: String?): Long? {
    val mentionUserId = mentionStr?.let { str ->
        val match = mentionRegex.find(str)
        match?.groupValues?.get(1)?.toLongOrNull()
    }
    return mentionUserId
}
