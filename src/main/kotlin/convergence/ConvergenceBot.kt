@file:Suppress("unused")

package convergence

import convergence.console.ConsoleProtocol
import convergence.discord.DiscordProtocol
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
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

        protocols.add(UniversalProtocol)
        protocols.add(ConsoleProtocol)
        protocols.add(DiscordProtocol)

        convergencePath = Paths.get(commandLineArgs.get<List<String>>("convergencePath").first())

        defaultLogger.info("Registering default commands...")
        registerDefaultCommands()

        defaultLogger.info("Registering addons...")
        updateChatMap()

        readSettings()

        loadProtocolConfig()

        defaultLogger.info("Starting command scheduler...")
        CommandScheduler.loadFromFile()
        CommandScheduler.start()
    }
}

private fun loadProtocolConfig() {
    for (protocol in protocols) {
        defaultLogger.info("Running ${protocol.name}.configLoaded...")
        try {
            protocol.configLoaded()
        } catch(e: Exception) {
            defaultLogger.error("Failed to run config callback! Exception: ", e)
        }
    }
}

private fun updateChatMap() {
    for (protocol in protocols) {
        defaultLogger.info("Initializing ${protocol.name}...")
        try {
            protocol.init()
            val chats = protocol.getChats()
            for (chat in chats) {
                if (chat !in reverseChatMap) {
                    while (currentChatID in chatMap)
                        currentChatID++
                    chatMap[currentChatID] = chat
                    reverseChatMap[chat] = currentChatID
                }
            }
        } catch(e: Exception) {
            defaultLogger.error("Failed to initialize protocol! Exception: ", e)
        }
    }
}
