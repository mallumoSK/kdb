package tk.mallumo.kdb.sqlite

import android.database.sqlite.SQLiteDatabase
import tk.mallumo.kdb.log
import tk.mallumo.kdb.tryIgnore

@Suppress("unused", "UNUSED_PARAMETER")
actual class SqliteDB(private val isDebug: Boolean, dbPath: String) {

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

