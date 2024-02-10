package tk.mallumo.kdb.sqlite

import android.content.*
import android.database.sqlite.*
import tk.mallumo.kdb.*
import java.sql.*
import kotlin.reflect.*

@Suppress("unused")
fun Context.defaultSqLitePath(name: String = "default-kdb.sqlite"): String = getDatabasePath(name).absolutePath

@Suppress("unused")
actual open class DbEngine(@Suppress("MemberVisibilityCanBePrivate") val isDebug: Boolean, dbPath: String) {

    actual open val path: String = dbPath

    var conn: SQLiteDatabase? = null

    actual open val isSqlite: Boolean = true

   companion object{
       fun createSQLite(isDebug:Boolean, path:String)  =DbEngine(isDebug = isDebug,  path)
   }

    actual open fun open() {
        conn = SQLiteDatabase.openOrCreateDatabase(path, null)
    }

    actual open fun close() {
        runCatching {
            conn?.close()
            conn = null
        }
    }

    actual open fun insert(command: String, body: (DbInsertStatement) -> Unit) {
        @Suppress("MemberVisibilityCanBePrivate")
        if (isDebug) logger(command)
        with(DbInsertStatement(this@DbEngine, command)) {
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
            Cursor(rawQuery(query, null)).use {
                callback.invoke(it)
            }
        }

    }

    actual open fun queryUnclosed(query: String): ((Cursor) -> Unit) {
        if (isDebug) logger(query)
        return { Cursor(conn!!.rawQuery(query, null)) }
    }


    actual open fun call(cmd: String, vararg args: KProperty0<*>) {
        if (isDebug) logger(cmd)
        error("undefined function")
    }
}

