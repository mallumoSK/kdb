package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*
import java.sql.*

@Suppress("unused", "UNUSED_PARAMETER")
actual class SqliteDB(
    val isDebug: Boolean,
    isSqLite: Boolean,
    private val connectionCallback: () -> Connection
) {

    actual val path: String = ""

    var conn: Connection? = null

    actual fun open() {
        conn = connectionCallback.invoke()
    }

    actual fun close() {
        runCatching {
            conn?.close()
            conn = null
        }

    }

    actual fun insert(command: String, body: (DbInsertStatement) -> Unit) {
        if (isDebug) logger(command)
        body(DbInsertStatement(this@SqliteDB, command))

    }

    actual fun exec(command: String) {
        if (isDebug) logger(command)
        conn?.execSQL(command)
    }

    actual fun query(
        query: String,
        callback: (cursor: Cursor) -> Unit
    ) {
        if (isDebug) logger(query)
        conn?.apply {
            Cursor(rawQuery(query, null)).also {
                try {
                    callback.invoke(it)
                } catch (e: Exception) {
                    throw e
                } finally {
                    runCatching {
                        it.close()
                    }
                }
            }

        }
    }

    actual fun queryUnclosed(query: String): ((Cursor) -> Unit) {
        if (isDebug) logger(query)
        return { Cursor(conn!!.rawQuery(query, null)) }
    }


    actual fun call(sql: String) {
        if (isDebug) logger(sql)
        conn?.prepareCall(sql)?.also {
            it.execute()
            it.close()
        }

    }

}

@Suppress("UNUSED_PARAMETER")
private fun Connection.rawQuery(query: String, nothing: Nothing?): ResultSet {
    return createStatement().executeQuery(query)
}

private fun Connection.execSQL(command: String) {
    createStatement().also {
        it.execute(command)
        it.close()
    }
    commit()
}
