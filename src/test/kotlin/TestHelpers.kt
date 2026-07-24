
import convergence.UniversalProtocol
import convergence.appModules
import convergence.bot
import convergence.settings
import org.koin.core.context.startKoin

fun resetGlobalState() {
    settings.aliases.clear()
    settings.timers.clear()
    settings.linkedChats.clear()
    settings.serializedCommands.clear()
    settings.commandDelimiters.clear()
    settings.syncedCalendars.clear()
    settings.notificationChannels.clear()
    settings.imageUploadChannels.clear()
    settings.reactServers.clear()
    settings.mentionChats.clear()
    settings.debugMode = false
    bot.commands.remove(TestProtocol)
    bot.commands.remove(UniversalProtocol)
    bot.chatMap.clear()
    bot.reverseChatMap.clear()
}

/**
 * Ensures Koin is started for tests. Idempotent — safe to call multiple times.
 */
fun ensureKoinStarted() {
    try {
        startKoin {
            modules(appModules)
        }
    } catch (_: Exception) {
        // Koin already started
    }
}
