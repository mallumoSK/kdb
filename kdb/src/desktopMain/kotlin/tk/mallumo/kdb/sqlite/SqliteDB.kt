package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*
import java.sql.*

@Suppress("unused", "UNUSED_PARAMETER")
actual open class SqliteDB(
    val isDebug: Boolean,
    isSqLite: Boolean,
    private val connectionCallback: () -> Connection
) {

    actual open val path: String = ""

    var conn: Connection? = null

    actual open fun open() {
        conn = connectionCallback.invoke()
    }

    actual open fun close() {
        runCatching {
            conn?.close()
            conn = null
        }

    }

    actual open fun insert(command: String, body: (DbInsertStatement) -> Unit) {
        if (isDebug) logger(command)
        with(DbInsertStatement(this@SqliteDB, command)) {
            prepare()
            body(this)
        }
    }

    actual open fun exec(command: String) {
        if (isDebug) logger(command)
        conn?.execSQL(command)
    }

    actual open fun query(
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

    actual open fun queryUnclosed(query: String): ((Cursor) -> Unit) {
        if (isDebug) logger(query)
        return { Cursor(conn!!.rawQuery(query, null)) }
    }


    actual open fun call(sql: String) {
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
