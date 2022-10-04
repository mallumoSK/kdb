package tk.mallumo.kdb.sqlite

import android.content.*
import android.database.sqlite.*
import tk.mallumo.kdb.*

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
            Cursor(rawQuery(query, null)).use {
                callback.invoke(it)
            }


        }

    }

    actual fun queryUnclosed(query: String): ((Cursor) -> Unit) {
        if (isDebug) log(query)
        return { Cursor(conn!!.rawQuery(query, null)) }
    }


    actual fun call(sql: String) {
        if (isDebug) log(sql)
        conn?.execSQL(sql)
    }


}

