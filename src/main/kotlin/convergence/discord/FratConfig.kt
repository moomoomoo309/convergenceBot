package convergence.discord

import com.fasterxml.jackson.module.kotlin.readValue
import convergence.Command
import convergence.objectMapper
import convergence.registerCommand
import org.apache.commons.text.similarity.LevenshteinDistance
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

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
    val crossingDate: LocalDate?,
    val bigBrother: String,
    val nickName: String?,
)

val fratConfigPath: Path = Paths.get("/", "opt", "bots", "config.json")
//val fratConfigPath: Path = Paths.get("/", "home", "nicholasdelello", "config.json")
val fratConfig: FratConfig by lazy { objectMapper.readValue(fratConfigPath.toFile()) }
val brotherInfoPath: Path = Paths.get("/", "opt", "bots", "convergence", "brotherInfo.json")
//val brotherInfoPath: Path = Paths.get("/", "home", "nicholasdelello", "brotherInfo.json")
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

fun registerFratCommands() {
    registerCommand(Command(
        DiscordProtocol,
        "brotherInfo",
        { args, _, _ ->
            val info = if (args.size == 1 && args.first().startsWith("#")) {
                brotherInfo.firstOrNull { it.rosterNumber == args.first().substring(1) }
                    ?: return@Command "No brother with roster number ${args.first().substring(1)} found."
            } else {
                val name = args.joinToString(" ")
                brotherInfo.minBy { LevenshteinDistance.getDefaultInstance().apply(name, it.firstName + " " + it.lastName) }
            }
            "Info for Brother #${info.rosterNumber} ${info.firstName} ${info.lastName}: Pledge Class ${englishToGreek.getOrDefault(info.pledgeClass, info.pledgeClass)}, Crossing date ${info.crossingDate ?: "Unknown"}, Big ${info.bigBrother}, Nickname ${if (info.nickName != null) "\"${info.nickName}\"" else "not listed in roster sheet"}"
        },
        "Gets information about a particular brother based on their name or roster number.",
        "brotherInfo (Roster or name, if it starts with # it's assumed to be a roster number)"
    ))
}
