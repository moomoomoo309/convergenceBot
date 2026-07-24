package convergence

import convergence.console.ConsoleProtocol
import convergence.discord.DiscordProtocol
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import org.koin.core.context.startKoin
import java.nio.file.Paths

object ConvergenceBot {
    @JvmStatic
    fun main(args: Array<String>) {
        // Parse command line arguments.
        val argParser = ArgumentParsers.newFor("Convergence Bot").build()
            .defaultHelp(true)
            .description("Sets the paths used by the bot.")

        val paths = argParser.addArgumentGroup("Paths")
        paths.addArgument("-c", "--convergence-path")
            .dest("convergencePath")
            .nargs(1)
            .type(String::class.java)
            .default = listOf(Paths.get(System.getProperty("user.home"), ".convergence").toString())

        val commandLineArgs = try {
            argParser.parseArgs(args)
        } catch(e: ArgumentParserException) {
            defaultLogger.error("Failed to parse command line arguments. Exception: ", e)
            return
        }

        convergencePath = Paths.get(commandLineArgs.get<List<String>>("convergencePath").first())

        // Initialize Koin DI container
        startKoin {
            modules(appModules)
        }
        defaultLogger.info("Koin DI container initialized.")

        bot.protocols.add(UniversalProtocol)
        bot.protocols.add(ConsoleProtocol)
        bot.protocols.add(DiscordProtocol)

        defaultLogger.info("Registering default commands...")
        registerDefaultCommands()

        updateChatMap()

        defaultLogger.info("Loading settings...")
        readSettings()

        loadProtocolConfig()

        defaultLogger.info("Starting command scheduler...")
        val scheduler = getKoinService<SchedulerThread>()
        scheduler.loadFromFile()
        scheduler.start()
    }
}

private fun loadProtocolConfig() {
    for (protocol in bot.protocols) {
        defaultLogger.info("Running ${protocol.name}.configLoaded...")
        try {
            protocol.configLoaded()
        } catch(e: Exception) {
            defaultLogger.error("Failed to run config callback! Exception: ", e)
        }
    }
}

private fun updateChatMap() {
    for (protocol in bot.protocols) {
        defaultLogger.info("Initializing ${protocol.name}...")
        try {
            protocol.init()
            val chats = protocol.getChats()
            for (chat in chats) {
                if (chat !in bot.reverseChatMap) {
                    val id = bot.currentChatID.getAndIncrement()
                    bot.chatMap[id] = chat
                    bot.reverseChatMap[chat] = id
                }
            }
        } catch(e: Exception) {
            defaultLogger.error("Failed to initialize protocol! Exception: ", e)
        }
    }
}
