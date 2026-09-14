package convergence.model

import convergence.toEmoji

interface Emoji {
    fun asString(): String
}

abstract class CustomEmoji(open val name: String, open val url: String?): Emoji
class UnicodeEmoji(val emoji: com.sigpwned.emoji4j.core.grapheme.Emoji): Emoji {
    constructor(s: String): this(s.toEmoji()!!)

    override fun asString(): String = emoji.toString()
}
