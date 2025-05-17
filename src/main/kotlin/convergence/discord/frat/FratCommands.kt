package convergence.discord.frat

import convergence.*
import convergence.discord.DiscordOutgoingMessage
import convergence.discord.DiscordProtocol
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.nio.file.Files

val englishToGreek = mapOf(
    'A' to 'Α',
    'B' to 'Β',
    'G' to 'Γ',
    'D' to 'Δ',
    'E' to 'Ε',
    'Z' to 'Ζ',
    'H' to 'Η',
    'Q' to 'Θ',
    'I' to 'Ι',
    'K' to 'Κ',
    'L' to 'Λ',
    'M' to 'Μ',
    'N' to 'Ν',
    'X' to 'Ξ',
    'O' to 'Ο',
    'P' to 'Π',
    'R' to 'Ρ',
    'S' to 'Σ',
    'T' to 'Τ',
    'U' to 'Υ',
    'F' to 'Φ',
    'C' to 'Χ',
    'Y' to 'Ψ',
)

fun getBrotherInfo(name: String, searchCriteria: (BrotherInfo) -> String?): BrotherInfo? {
    return brotherInfo.firstOrNull { searchCriteria(it)?.lowercase() == name }
        ?: brotherInfo.firstOrNull { searchCriteria(it)?.lowercase()?.startsWith(name) == true }
        ?: brotherInfo.firstOrNull { searchCriteria(it)?.lowercase()?.contains(name) == true }
}

fun brotherLineRec(name: String, searchCriteria: (BrotherInfo) -> String?, depth: Int = 25): MutableList<BrotherInfo> {
    val info = getBrotherInfo(name.lowercase(), searchCriteria)
        ?: return mutableListOf()

    if (depth == 0) {
        return mutableListOf()
    }

    val line = brotherLineRec(info.bigBrother.lowercase().ifBlank { "N/A" }, searchCriteria, depth - 1)
    line.add(info)
    return line
}

fun brotherLine(args: List<String>): DiscordOutgoingMessage {
    val name = args.joinToString(" ").lowercase()
    val startInfo = getBrotherInfo(name) { it.firstName + " " + it.lastName }
        ?: return DiscordOutgoingMessage("No brothers found searching for \"$name\".")

    val line = brotherLineRec(name, { it.firstName + " " + it.lastName })

    val msg = MessageCreateBuilder()
    val embeds = EmbedBuilder()
        .setTitle("Line for Brother #${startInfo.rosterNumber} ${startInfo.firstName} ${startInfo.lastName}")

    for (brother in line) {
        embeds.addField("#" + brother.rosterNumber, "${brother.firstName} \"${brother.nickName}\" ${brother.lastName}", true)
    }
    return DiscordOutgoingMessage(msg.addEmbeds(embeds.build()).build())
}

fun brotherInfo(args: List<String>, searchCriteria: (BrotherInfo) -> String?): DiscordOutgoingMessage {
    val name = args.joinToString(" ").lowercase()
    val info = getBrotherInfo(name, searchCriteria)
        ?: return DiscordOutgoingMessage("No brothers found searching for \"$name\".")
    return DiscordOutgoingMessage(
        MessageCreateBuilder()
            .addEmbeds(
                EmbedBuilder()
                    .setTitle("Info for Brother #${info.rosterNumber} ${info.firstName} ${info.lastName}")
                    .addField(
                        "Pledge Class",
                        info.pledgeClass.map { englishToGreek[it] }.joinToString("").ifBlank { "not listed" }, true
                    )
                    .addField("Crossing date", info.crossingDate.ifBlank { "not listed" }, true)
                    .addField("Big brother", info.bigBrother.ifBlank { "not listed" }, true)
                    .addField(
                        "Nickname",
                        if (info.nickName.isNotBlank()) "\"${info.nickName}\"" else "not listed",
                        true
                    )
                    .addField("Major", info.major.ifBlank { "not listed" }, true)
                    .build()
            ).build()
    )
}

fun registerFratCommands() {
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbyroster",
            listOf(ArgumentSpec("Roster", ArgumentType.STRING)),
            { args: List<String> -> brotherInfo(args) { it.rosterNumber } },
            "Gets information about a particular brother based on their roster number.",
            "brotherbyroster (roster number)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbyname",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> brotherInfo(args) { it.firstName + " " + it.lastName } },
            "Gets information about a particular brother based on their first and last name.",
            "brotherbyname (name)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherbynickname",
            listOf(ArgumentSpec("Nickname", ArgumentType.STRING)),
            { args -> brotherInfo(args) { it.nickName } },
            "Gets information about a particular brother based on their nickname.",
            "brotherbynickname (nickname)"
        )
    )
    registerCommand(
        Command(
            DiscordProtocol,
            "brotherline",
            listOf(ArgumentSpec("Name", ArgumentType.STRING)),
            { args -> brotherLine(args) },
            "Gets information about a particular brother's line going up.",
            "brotherline (name)"
        )
    )
    registerCommand(
        Command.of(
            DiscordProtocol,
            "updateRoster",
            listOf(),
            { ->
                val newRoster = getNewRoster()
                brotherInfo.clear()
                brotherInfo.addAll(newRoster)
                Files.write(brotherInfoPath, objectMapper.writeValueAsBytes(newRoster))
                "Roster updated."
            },
            "Updates the brother roster list.",
            "updateRoster (takes no arguments)"
        )
    )
}
