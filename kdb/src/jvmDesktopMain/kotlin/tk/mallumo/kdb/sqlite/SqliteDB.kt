package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.log
import tk.mallumo.kdb.tryIgnore
import java.sql.Connection
import java.sql.ResultSet

@Suppress("unused", "UNUSED_PARAMETER")
actual class SqliteDB(
    private val isDebug: Boolean,
    isSqLite: Boolean,
    private val connectionCallback: () -> Connection
) {

    actual val path: String = ""

    var conn: Connection? = null

    actual fun open() {
        conn = connectionCallback.invoke()
    }

    actual fun close() {
        tryIgnore {
            conn?.close()
            conn = null
        }

    }

    actual fun insert(command: String, body: (DbInsertStatement) -> Unit) {
        if (isDebug) log(command)
        body(DbInsertStatement(this@SqliteDB, command))

    }

    actual fun exec(command: String) {
        if (isDebug) log(command)
        conn?.execSQL(command)
    }

    actual fun query(
        query: String,
        callback: (cursor: Cursor) -> Unit
    ) {
        if (isDebug) log(query)
        conn?.apply {
            Cursor(rawQuery(query, null)).also {
                try {
                    callback.invoke(it)
                } catch (e: Exception) {
                    throw e
                } finally {
                    tryIgnore {
                        it.close()
                    }
                }
            }

        }
    }

    actual fun queryUnclosed(query: String): ((Cursor) -> Unit) {
        if (isDebug) log(query)
        return { Cursor(conn!!.rawQuery(query, null)) }
    }


    actual fun call(sql: String) {
        if (isDebug) log(sql)
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