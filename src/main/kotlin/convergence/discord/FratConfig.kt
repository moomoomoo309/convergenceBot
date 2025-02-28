package convergence.discord

import com.fasterxml.jackson.module.kotlin.readValue
import convergence.Command
import convergence.objectMapper
import convergence.registerCommand
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

class CurrentSemester(val year: String, val season: String)

class FratConfig(
    val currentSemester: CurrentSemester,
    val houseResidents: Map<String, String>,
    val botPassword: String,
    val nextcloudURL: String,
    val dutySheetDiscordURL: String,
    val agendaDiscordURL: String,
    val minutesAADiscordURL: String,
    val minutesAAAnnouncementDiscordURL: String,
    val minutesDiscordURL: String,
    val houseDutyReminderURL: String,
    val attendanceDiscordURL: String,
    val weeklyDutiesPath: String,
    val agendaFolder: String,
    val minutesFolder: String,
    val aaMinutesFolder: String,
    val aaFolder: String,
    val attendanceFolder: String,
    val debugMode: Boolean
)

data class BrotherInfo(
    val rosterNumber: String,
    val firstName: String,
    val lastName: String,
    val pledgeClass: String,
    val crossingDate: String,
    val bigBrother: String,
    val nickName: String,
    val major: String
)

val fratConfigPath: Path = Paths.get("/", "opt", "bots", "config.json")
val fratConfig: FratConfig by lazy { objectMapper.readValue(fratConfigPath.toFile()) }
val brotherInfoPath: Path = Paths.get("/", "opt", "bots", "convergence", "brotherInfo.json")
val brotherInfo: List<BrotherInfo> by lazy { objectMapper.readValue(brotherInfoPath.toFile()) }

val englishToGreek = mapOf(
    "A" to "Α",
    "B" to "Β",
    "G" to "Γ",
    "D" to "Δ",
    "E" to "Ε",
    "Z" to "Ζ",
    "H" to "Η",
    "Q" to "Θ",
    "I" to "Ι",
    "K" to "Κ",
    "L" to "Λ",
    "M" to "Μ",
    "N" to "Ν",
    "X" to "Ξ",
    "O" to "Ο",
    "P" to "Π",
    "R" to "Ρ",
    "S" to "Σ",
    "T" to "Τ",
    "U" to "Υ",
    "F" to "Φ",
    "C" to "Χ",
    "Y" to "Ψ",
)

private val dateFormatter = DateTimeFormatter.ofPattern("LLLL dd yyyy")
fun brotherInfo(args: List<String>, searchCriteria: (BrotherInfo) -> String?): String {
    val name = args.joinToString(" ").lowercase()
    val info = brotherInfo.firstOrNull { searchCriteria(it)?.lowercase() == name }
        ?: brotherInfo.firstOrNull { searchCriteria(it)?.lowercase()?.startsWith(name) == true }
        ?: brotherInfo.firstOrNull { searchCriteria(it)?.lowercase()?.contains(name) == true }
        ?: return "No brothers found searching for \"$name\"."
    return "Info for Brother #${info.rosterNumber} ${info.firstName} ${info.lastName}:" +
            "\nPledge Class ${info.pledgeClass.map { englishToGreek[it.toString()] }.joinToString("").ifEmpty { "not listed" }}" +
            "\nCrossing date ${info.crossingDate.format(dateFormatter).ifBlank { "not listed" }}" +
            "\nBig ${info.bigBrother.ifBlank { "not listed" }}" +
            "\nNickname ${if (info.nickName.isNotBlank()) "\"${info.nickName}\"" else "not listed"}" +
            "\nMajor ${info.major.ifBlank { "not listed" }}"
}

fun registerFratCommands() {
    registerCommand(Command(
        DiscordProtocol,
        "brotherbyroster",
        { args, _, _ -> brotherInfo(args) { it.rosterNumber } },
        "Gets information about a particular brother based on their roster number.",
        "brotherbyroster (roster number)"
    ))
    registerCommand(Command(
        DiscordProtocol,
        "brotherbyname",
        { args, _, _ -> brotherInfo(args) { it.firstName + " " + it.lastName } },
        "Gets information about a particular brother based on their first and last name.",
        "brotherbyname (name)"
    ))
    registerCommand(Command(
        DiscordProtocol,
        "brotherbynickname",
        { args, _, _ -> brotherInfo(args) { it.nickName } },
        "Gets information about a particular brother based on their nickname.",
        "brotherbynickname (nickname)"
    ))
}
