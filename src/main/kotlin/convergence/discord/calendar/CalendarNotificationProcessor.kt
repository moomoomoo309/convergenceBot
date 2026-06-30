package convergence.discord.calendar

import convergence.*
import convergence.discord.DiscordChat
import net.fortuna.ical4j.model.component.VEvent
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

object CalendarNotificationProcessor {

    /**
     * Extracts VALARM components from a VEvent, returning pairs of (triggerDuration, description).
     * Only DISPLAY-type alarms are included.
     * The returned duration is always positive (representing time before the event).
     */
    fun extractAlarms(event: VEvent): List<Pair<Duration, String>> {
        val alarms = event.alarms
        val result = mutableListOf<Pair<Duration, String>>()

        for (alarm in alarms) {
            val action = alarm.action ?: continue
            if (action.value != "DISPLAY") continue

            val trigger = alarm.trigger ?: continue
            val dur = trigger.duration ?: continue

            val description = alarm.description?.value ?: "Reminder"

            // Convert ical4j Dur to java.time.Duration
            // Note: ical4j Dur stores absolute values; isNegative indicates the sign
            var javaDuration = Duration.ofDays(dur.days.toLong())
                .plusHours(dur.hours.toLong())
                .plusMinutes(dur.minutes.toLong())
                .plusSeconds(dur.seconds.toLong())

            // Negate if the trigger is negative (before event)
            if (dur.isNegative) {
                javaDuration = javaDuration.negated()
            }

            result.add(Pair(javaDuration, description))
        }

        return result
    }

    /**
     * Calculates the notification time for a given event start and trigger duration.
     * The trigger duration is typically negative (e.g., -PT15M for 15 minutes before).
     */
    fun calculateNotificationTime(eventStart: Instant, triggerDuration: Duration): Instant {
        return eventStart.plus(triggerDuration)
    }

    /**
     * Schedules notifications for events with alarms.
     * Called after calendar sync to set up reminders.
     */
    fun scheduleNotificationsForEvents(
        events: List<EventInstance>,
        notificationChannels: List<CalendarNotificationChannel>
    ) {
        if (notificationChannels.isEmpty())
            return

        for (event in events) {
            val alarms = extractAlarms(event.vevent)
            if (alarms.isEmpty())
                continue

            val eventSummary = event.summary ?: "Unnamed event"
            val eventStart = event.start.toInstant()

            for ((duration, description) in alarms) {
                val notifyAt = calculateNotificationTime(eventStart, duration)

                // Only schedule if notification time is in the future
                if (notifyAt.isBefore(Instant.now())) continue

                // Find which channels should receive this notification
                for (channel in notificationChannels) {
                    scheduleNotification(
                        eventSummary = eventSummary,
                        eventStart = eventStart,
                        channelId = channel.channelId,
                        notifyAt = notifyAt,
                        description = description,
                        mentionUserId = channel.mentionUserId
                    )
                }
            }
        }
    }

    /**
     * Schedules a single notification to be sent at the specified time.
     */
    private fun scheduleNotification(
        eventSummary: String,
        eventStart: Instant,
        channelId: Long,
        notifyAt: Instant,
        description: String,
        mentionUserId: Long?
    ) {
        val notifyAtOffset = notifyAt.atOffset(ZoneOffset.UTC)
        val eventStartOffset = eventStart.atOffset(ZoneOffset.UTC)

        CommandScheduler.taskList.add(
            ScheduledTask(notifyAtOffset) {
                sendNotification(
                    channelId = channelId,
                    eventSummary = eventSummary,
                    eventStart = eventStartOffset,
                    description = description,
                    mentionUserId = mentionUserId
                )
            }
        )
    }

    /**
     * Sends a notification message to the specified Discord channel.
     */
    private fun sendNotification(
        channelId: Long,
        eventSummary: String,
        eventStart: OffsetDateTime,
        description: String,
        mentionUserId: Long?
    ) {
        val chat = DiscordChat(channelId)
        val timeUntil = formatTime(eventStart)
        val mentionStr = mentionUserId?.let { "<@$it> " } ?: ""

        val message = buildString {
            append(mentionStr)
            append("Reminder: ")
            append(eventSummary)
            append(" starts in ")
            append(timeUntil)
            append("\nEvent time: ")
            append(eventStart)
            if (description.isNotBlank() && description != "Reminder") {
                append("\n")
                append(description)
            }
        }

        sendMessage(chat, message)
    }
}
