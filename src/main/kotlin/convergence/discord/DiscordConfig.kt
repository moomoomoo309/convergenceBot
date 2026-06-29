package convergence.discord

import convergence.convergencePath
import java.nio.file.Files

val nextcloudPassword: String? by lazy {
    try {
        Files.readString(convergencePath.resolve("nextcloudPassword")).trim()
    } catch(_: Exception) {
        null
    }
}
