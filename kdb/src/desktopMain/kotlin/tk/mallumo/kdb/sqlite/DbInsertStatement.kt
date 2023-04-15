@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*

@Suppress("unused")
actual open class DbInsertStatement actual constructor(val db: SqliteDB, command: String) {

    private val statement = db.conn!!.prepareStatement(command)

    private var rowAdded = false


    actual open fun prepare() {
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
        } catch (e: Exception) {
            runCatching { statement.close() }
            throw e
        }
        rowAdded = true
    }

    actual open fun commit() {
        if (!rowAdded) add()

        statement.executeBatch()
        db.conn!!.commit()
        statement.clearBatch()
        close()
    }

    actual open fun close() {
        rowAdded = true
        runCatching { statement.close() }
    }
}
