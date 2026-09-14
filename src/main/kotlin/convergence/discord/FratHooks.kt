package convergence.discord

private val fratLogger = org.slf4j.LoggerFactory.getLogger("convergence.discord.frat")

fun tryRegisterFratCommands() {
    try {
        val clazz = Class.forName("convergence.discord.frat.FratCommandsKt")
        clazz.getMethod("registerFratCommands").invoke(null)
    } catch(_: ClassNotFoundException) {
        fratLogger.info("Frat module not found — skipping frat command registration.")
    } catch(e: Exception) {
        fratLogger.error("Failed to register frat commands", e)
    }
}
