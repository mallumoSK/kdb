@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import android.database.sqlite.*

@Suppress("unused")
actual class DbInsertStatement actual constructor(
    @Suppress("MemberVisibilityCanBePrivate") val db: SqliteDB,
    command: String
) {

    private val statement: SQLiteStatement

    var executed = false
        private set

    init {
        db.conn!!.beginTransaction()
        statement = db.conn!!.compileStatement(command)
    }

    actual fun string(index: Int, callback: () -> String) {
        statement.bindString(index + 1, callback.invoke())
        executed = false
    }

    actual fun int(index: Int, callback: () -> Int) {
        statement.bindLong(index + 1, callback.invoke().toLong())
        executed = false
    }

    actual fun long(index: Int, callback: () -> Long) {
        statement.bindLong(index + 1, callback.invoke())
        executed = false
    }

    actual fun double(index: Int, callback: () -> Double) {
        statement.bindDouble(index + 1, callback.invoke())
        executed = false
    }

    actual fun add() {
        try {
            statement.executeInsert()
        } catch (e: Exception) {
            db.conn!!.endTransaction()
            throw e
        }
        executed = true
    }

    actual fun commit() {
        if (!executed) {
            add()
        }
        db.conn!!.setTransactionSuccessful()
        close()
    }

    actual fun close() {
        executed = true
        db.conn!!.endTransaction()
        statement.close()
    }

}
