package convergence.discord.frat

import convergence.discord.defaultZoneOffset
import convergence.titleCase
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.net.URI


val illegalRosters = setOf("823")

data class BrotherInfo(
    val rosterNumber: String,
    val lastName: String,
    val firstName: String,
    val pledgeClass: String,
    val crossingDate: String,
    val bigBrother: String,
    val nickName: String,
    val major: String
)

fun getNewRoster(): List<BrotherInfo> {
    val workbook = WorkbookFactory.create(URI(fratConfig.rosterURL).toURL().openStream())
    val worksheet = workbook.getSheet("Rosters 350+")
    var i = 0
    var row = worksheet.getRow(i)
    formulaEvaluator = XSSFFormulaEvaluator(workbook as XSSFWorkbook)
    while (row.getCell(0).readString() != "351") {
        i += 1
        row = worksheet.getRow(i)
    }
    val brotherInfoList = mutableListOf<BrotherInfo>()
    while (i < 1000) {
        if (row.getCell(0).readString().isNotBlank())
            brotherInfoList.add(extractInfoFromRow(row))
        if (worksheet.getRow(i + 1).getCell(0).readString().isBlank() &&
            worksheet.getRow(i + 2).getCell(0).readString().isBlank()) {
            break
        }
        i += 1
        row = worksheet.getRow(i)
    }
    return brotherInfoList.filter { it.rosterNumber !in illegalRosters }
}

private fun getOrdinal(day: Int): String {
    if (day in 11..13 || day in -13..-11) {
        return "th"
    }
    return when(day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

private val dataFormatter = DataFormatter()
lateinit var formulaEvaluator: FormulaEvaluator
fun Cell.readString(): String {
    if (this.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(this)) {
        val date = this.dateCellValue.toInstant().atOffset(defaultZoneOffset)
        return "${date.month.name.titleCase()} ${date.dayOfMonth}${getOrdinal(date.dayOfMonth)}, ${date.year}"
    }
    return dataFormatter.formatCellValue(this, formulaEvaluator).trim()
}

fun extractInfoFromRow(row: Row): BrotherInfo {
    return BrotherInfo(
        row.getCell(0).readString(),
        row.getCell(1).readString(),
        row.getCell(2).readString(),
        row.getCell(3).readString(),
        row.getCell(4).readString(),
        row.getCell(5).readString(),
        row.getCell(6).readString(),
        row.getCell(7).readString(),
    )
}
