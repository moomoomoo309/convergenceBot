package convergence.model

import java.util.*

// If possible, the name would be an enum instead, but I want the ability for protocols to add extra formats
// beyond the defaults in the companion object, so it's an open class.
open class Format(name: String) {
    val name: String = name.lowercase(Locale.getDefault())

    companion object {
        val bold = Format("bold")
        val italics = Format("italics")
        val underline = Format("underline")
        val monospace = Format("monospace")
        val code = Format("code")
        val strikethrough = Format("strikethrough")
        val spoiler = Format("spoiler")
        val greentext = Format("greentext")
    }
}
