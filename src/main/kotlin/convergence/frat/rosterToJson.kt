package convergence.frat

import convergence.objectMapper
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.net.URI


val illegalRosters = setOf("823")

fun rosterToJson(): String {
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
    while (true) {
        if (row.getCell(0).readString().isNotBlank())
            brotherInfoList.add(extractInfoFromRow(row))
        if (worksheet.getRow(i + 1).getCell(0).readString().isBlank() &&
            worksheet.getRow(i + 2).getCell(0).readString().isBlank()) {
            break
        }
        i += 1
        row = worksheet.getRow(i)
    }
    return objectMapper.writeValueAsString(brotherInfoList.filter { it.rosterNumber !in illegalRosters })
}

private fun getOrdinal(day: Int): String {
    if (day in 11..13) {
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
val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
lateinit var formulaEvaluator: FormulaEvaluator
fun Cell.readString(): String {
    if (this.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(this)) {
        val date = this.dateCellValue
        return "${months[date.month]} ${date.date}${getOrdinal(date.date)}, ${date.year + 1900}"
    }
    return dataFormatter.formatCellValue(this, formulaEvaluator)
}

fun extractInfoFromRow(row: Row): BrotherInfo {
    return BrotherInfo(
        row.getCell(0).readString().trim(),
        row.getCell(1).readString().trim(),
        row.getCell(2).readString().trim(),
        row.getCell(3).readString().trim(),
        row.getCell(4).readString().trim(),
        row.getCell(5).readString().trim(),
        row.getCell(6).readString().trim(),
        row.getCell(7).readString().trim(),
    )
}
