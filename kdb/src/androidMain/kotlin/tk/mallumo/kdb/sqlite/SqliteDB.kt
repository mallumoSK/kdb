package tk.mallumo.kdb.sqlite

import android.content.*
import android.database.sqlite.*
import tk.mallumo.kdb.*
import tk.mallumo.log.logINFO
import tk.mallumo.utils.tryIgnore

fun Context.defaultSqLitePath(name: String = "default-kdb.sqlite"): String = getDatabasePath(name).absolutePath

@Suppress("unused")
actual class SqliteDB(val isDebug: Boolean, dbPath: String) {

    actual val path: String = dbPath

    var conn: SQLiteDatabase? = null

    actual fun open() {
        conn = SQLiteDatabase.openOrCreateDatabase(path, null)
    }

    actual fun close() {

        tryIgnore {
            conn?.close()
            conn = null
        }
    }

    actual fun insert(command: String, body: (DbInsertStatement) -> Unit) {
        if (isDebug) logINFO(command)
        body(DbInsertStatement(this@SqliteDB, command))
    }

    actual fun exec(command: String) {
        if (isDebug) logINFO(command)

        conn?.execSQL(command)
    }

    actual fun query(
        query: String,
        callback: (cursor: Cursor) -> Unit
    ) {
        if (isDebug) logINFO(query)


        conn?.apply {
            Cursor(rawQuery(query, null)).use {
                callback.invoke(it)
            }
        }

    }

    actual fun queryUnclosed(query: String): ((Cursor) -> Unit) {
        if (isDebug) logINFO(query)
        return { Cursor(conn!!.rawQuery(query, null)) }
    }


    actual fun call(sql: String) {
        if (isDebug) logINFO(sql)
        conn?.execSQL(sql)
    }
}

