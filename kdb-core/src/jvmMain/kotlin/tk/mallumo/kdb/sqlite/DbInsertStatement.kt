@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import java.math.*
import java.sql.*


@Suppress("unused")
actual open class DbInsertStatement actual constructor(val db: DbEngine) {

    private lateinit var statement: PreparedStatement
    private var rows = 0

    private var rowAdded = false

    protected actual val ids: MutableList<Long> = mutableListOf()

    actual open suspend fun run(command: String, body: DbInsertStatement.() -> Unit) {
        db.connection {
            ids.clear()
            statement = prepareStatement(command, Statement.RETURN_GENERATED_KEYS)
            body()
        }
    }

    actual open fun string(index: Int, callback: () -> String) {
        statement.setString(index + 1, callback.invoke())
        rowAdded = false
    }

    actual open fun int(index: Int, callback: () -> Int) {
        statement.setLong(index + 1, callback.invoke().toLong())
        rowAdded = false
    }

    actual open fun long(index: Int, callback: () -> Long) {
        statement.setLong(index + 1, callback.invoke())
        rowAdded = false
    }

    actual open fun double(index: Int, callback: () -> Double) {
        statement.setDouble(index + 1, callback.invoke().truncateDecimal(10))
        rowAdded = false
    }

    actual open fun time(index: Int, callback: () -> LocalTime) {
        if (db.isSqlite) {
            statement.setString(index + 1, callback.invoke().format(LocalTime.Formats.ISO))
        } else {
            statement.setObject(index + 1, callback.invoke().toJavaLocalTime())
        }
        rowAdded = false
    }

    actual open fun date(index: Int, callback: () -> LocalDate) {
        if (db.isSqlite) {
            statement.setString(index + 1, callback.invoke().format(LocalDate.Formats.ISO))
        } else {
            statement.setObject(index + 1, callback.invoke().toJavaLocalDate())
        }
        rowAdded = false
    }

    actual open fun dateTime(index: Int, callback: () -> LocalDateTime) {
        if (db.isSqlite) {
            statement.setString(index + 1, callback.invoke().format(LocalDateTime.Formats.ISO))
        } else {
            statement.setObject(index + 1, callback.invoke().toJavaLocalDateTime())
        }
        rowAdded = false
    }

    private fun Double.truncateDecimal(numberOfDecimals: Int): Double {
        return if (this > 0) {
            BigDecimal(toString())
                .setScale(numberOfDecimals, RoundingMode.FLOOR)
                .toDouble()
        } else {
            BigDecimal(toString())
                .setScale(numberOfDecimals, RoundingMode.CEILING)
                .toDouble()
        }
    }

    actual open fun add() {
        try {
            statement.addBatch()
            rows += 1
        } catch (e: Exception) {
            runCatching { statement.close() }
            throw e
        }
        rowAdded = true
    }

    actual open fun commit(): List<Long> {
        if (!rowAdded) add()

        statement.executeBatch()
        statement.connection.commit()

        val newIds = mutableListOf<Long>()
        statement.generatedKeys.also { rs ->
            while (rs.next()) {
                newIds += rs.getLong(1)
            }
        }
        if (rows != 0) {
            if (newIds.size == 1) {
                ids += (0 until rows)
                    .reversed()
                    .map { newIds[0] - it }
            } else {
                ids += newIds
            }
        }

        statement.clearBatch()
        close()
        return ids
    }

    actual open fun close() {
        rowAdded = true
        runCatching { statement.close() }
    }
}
