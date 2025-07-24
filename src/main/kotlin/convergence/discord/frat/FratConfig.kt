package convergence.discord.frat

import com.fasterxml.jackson.module.kotlin.readValue
import convergence.objectMapper
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("unused")
class CurrentSemester(val year: String, val season: String)

@Suppress("unused")
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
    val debugMode: Boolean,
    val rosterURL: String
)

val fratConfigPath: Path = Paths.get("/", "opt", "bots", "config.json")
val fratConfig: FratConfig by lazy { objectMapper.readValue(fratConfigPath.toFile()) }
val brotherInfoPath: Path = Paths.get("/", "opt", "bots", "convergence", "brotherInfo.json")
val brotherInfo: MutableList<BrotherInfo> by lazy { objectMapper.readValue(brotherInfoPath.toFile()) }
