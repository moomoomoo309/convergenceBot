package convergence

import org.jetbrains.exposed.sql.Table

object SerializedCommand: Table("SerializedCommand") {
    val command = blob("command")
}

object SerializedAlias: Table("SerializedAlias") {
    val name = varchar("name", 80).primaryKey()
    val alias = blob("alias")
}

object SerializedScheduledCommand: Table("SerializedScheduledCommand") {
    val id = integer("id").primaryKey()
    val scheduledCommand = blob("scheduledCommand")
}

