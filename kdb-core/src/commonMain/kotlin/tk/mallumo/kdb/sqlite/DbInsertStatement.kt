package tk.mallumo.kdb.sqlite

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime


@Suppress("unused")
expect open class DbInsertStatement(db: DbEngine) {
    protected val ids: MutableList<Long>

    open suspend fun run(command: String, body: DbInsertStatement.() -> Unit)
    open fun string(index: Int, callback: () -> String)
    open fun int(index: Int, callback: () -> Int)
    open fun long(index: Int, callback: () -> Long)
    open fun double(index: Int, callback: () -> Double)

    open fun time(index: Int, callback: () -> LocalTime)
    open fun date(index: Int, callback: () -> LocalDate)
    open fun dateTime(index: Int, callback: () -> LocalDateTime)

    open fun add()
    open fun commit(): List<Long>
    open fun close()
}
