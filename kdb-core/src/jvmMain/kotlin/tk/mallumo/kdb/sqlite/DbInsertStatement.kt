@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import java.sql.Statement

@Suppress("unused")
actual open class DbInsertStatement actual constructor(val db: DbEngine, command: String) {

    private val statement = db.getConnection().prepareStatement(command,  Statement.RETURN_GENERATED_KEYS)
    private var rows = 0

    private var rowAdded = false
    protected actual val ids: MutableList<Long> = mutableListOf()


    actual open fun prepare() {
        ids.clear()
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
        statement.setDouble(index + 1, callback.invoke())
        rowAdded = false
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
        db.getConnection().commit()

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
