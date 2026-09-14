package convergence.discord

import convergence.convergencePath
import org.slf4j.LoggerFactory
import java.nio.file.Files

private val configLogger = LoggerFactory.getLogger("convergence.discord.config")

val nextcloudPassword: String? by lazy {
    // Try fratConfig.botPassword first (via reflection to avoid circular dependency)
    val fratPassword = try {
        val clazz = Class.forName("convergence.discord.frat.FratConfigKt")
        val config = clazz.getMethod("getFratConfig").invoke(null)
        if (config != null) {
            config::class.java.getMethod("getBotPassword").invoke(config) as? String
        } else null
    } catch(_: ClassNotFoundException) {
        null
    } catch(e: Exception) {
        configLogger.debug("Could not read fratConfig.botPassword: ", e)
        null
    }

    if (fratPassword != null) {
        configLogger.info("Using Nextcloud password from fratConfig")
        fratPassword
    } else {
        // Fall back to nextcloudPassword file
        try {
            Files.readString(convergencePath.resolve("nextcloudPassword")).trim()
        } catch(_: Exception) {
            null
        }
    }
}
