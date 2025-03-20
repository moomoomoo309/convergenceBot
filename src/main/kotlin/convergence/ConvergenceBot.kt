@file:Suppress("unused")

package convergence

import convergence.console.ConsoleProtocol
import convergence.discord.DiscordProtocol
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import java.nio.file.Paths
import kotlin.collections.set

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
            defaultLogger.error("Failed to parse command line arguments. Printing stack trace:")
            defaultLogger.error(getStackTraceText(e))
            return
        }

        protocols.add(UniversalProtocol)
        protocols.add(ConsoleProtocol)
        protocols.add(DiscordProtocol)

        convergencePath = Paths.get(commandLineArgs.get<List<String>>("convergencePath").first())

        defaultLogger.info("Registering default commands...")
        registerDefaultCommands()

        // Update the chat map
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
                e.printStackTrace()
            }
        }

        readSettings()

        defaultLogger.info("Starting command scheduler...")
        CommandScheduler.loadFromFile()
        CommandScheduler.start()
    }
}
