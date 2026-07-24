package convergence.model

import com.sigpwned.emoji4j.core.grapheme.Emoji
import convergence.toEmoji

interface IEmoji {
    fun asString(): String
}

abstract class CustomEmoji(open val name: String, open val url: String?): IEmoji
class UnicodeEmoji(val emoji: Emoji): IEmoji {
    constructor(s: String): this(s.toEmoji()!!)

    override fun asString(): String = emoji.toString()
}
