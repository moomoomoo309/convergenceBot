import convergence.*

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
