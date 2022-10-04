@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*

@Suppress("unused")
actual class DbInsertStatement actual constructor(val db: SqliteDB, command: String) {

    private val statement = db.conn!!.prepareStatement(command)

    var executed = false
        private set

    actual fun string(index: Int, callback: () -> String) {
        statement.setString(index + 1, callback.invoke())
        executed = false
    }

    actual fun int(index: Int, callback: () -> Int) {
        statement.setLong(index + 1, callback.invoke().toLong())
        executed = false
    }

    actual fun long(index: Int, callback: () -> Long) {
        statement.setLong(index + 1, callback.invoke())
        executed = false
    }

    actual fun double(index: Int, callback: () -> Double) {
        statement.setDouble(index + 1, callback.invoke())
        executed = false
    }

    actual fun add() {
        try {
            statement.addBatch()
        } catch (e: Exception) {
            tryIgnore { statement.close() }
            throw e
        }
        executed = true
    }

    actual fun commit() {
        if (!executed) {
            add()
        }
        statement.executeBatch()
        db.conn!!.commit()
        statement.clearBatch()
        close()
    }

    actual fun close() {
        executed = true
        tryIgnore { statement.close() }
    }

}
