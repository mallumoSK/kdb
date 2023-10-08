@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import android.database.sqlite.*

@Suppress("unused")
actual open class DbInsertStatement actual constructor(
    @Suppress("MemberVisibilityCanBePrivate") val db: SqliteDB,
    private val command: String
) {

    protected actual val ids: MutableList<Long> = mutableListOf()

    private lateinit var statement: SQLiteStatement

    private var executed = false

    actual open fun prepare() {
        ids.clear()
        db.conn!!.beginTransaction()
        statement = db.conn!!.compileStatement(command)
    }

    actual open fun string(index: Int, callback: () -> String) {
        statement.bindString(index + 1, callback.invoke())
        executed = false
    }

    actual open fun int(index: Int, callback: () -> Int) {
        statement.bindLong(index + 1, callback.invoke().toLong())
        executed = false
    }

    actual open fun long(index: Int, callback: () -> Long) {
        statement.bindLong(index + 1, callback.invoke())
        executed = false
    }

    actual open fun double(index: Int, callback: () -> Double) {
        statement.bindDouble(index + 1, callback.invoke())
        executed = false
    }

    actual open fun add() {
        try {
            ids += statement.executeInsert()
        } catch (e: Exception) {
            db.conn!!.endTransaction()
            throw e
        }
        executed = true
    }

    actual open fun commit(): List<Long> {
        if (!executed) {
            add()
        }
        db.conn!!.setTransactionSuccessful()
        close()
        return ids
    }

    actual open fun close() {
        executed = true
        db.conn!!.endTransaction()
        statement.close()
    }

}
