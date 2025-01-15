package convergence.discord

import convergence.objectMapper
import convergence.readValue
import java.nio.file.Path
import java.nio.file.Paths

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

//val fratConfigPath: Path = Paths.get("/", "opt", "bots", "config.json")
val fratConfigPath: Path = Paths.get("/", "home", "nicholasdelello", "config.json")
val fratConfig: FratConfig by lazy { objectMapper.readValue<FratConfig>(fratConfigPath.toFile()) }
