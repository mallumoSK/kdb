package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*
import java.sql.*
import java.util.*


@Suppress("unused", "UNUSED_PARAMETER")
actual open class DbEngine(
    val isDebug: Boolean,
    sqlite: Boolean,
    private val connectionCallback: () -> Connection
) {

    companion object {
        fun createSQLite(isDebug: Boolean, path: String) = DbEngine(isDebug = isDebug, sqlite = true) {
            DriverManager.getConnection("jdbc:sqlite:${path}").apply {
                autoCommit = false
            }
        }

        fun createMySql(isDebug: Boolean, name: String, pass: String, database: String, host: String, port: Int) = DbEngine(isDebug = isDebug, sqlite = false) {
            DriverManager.getConnection("jdbc:mysql://${host}:$port/${database}", name, pass).apply {
                autoCommit = false
            }
        }

        fun createProperties(name: String, pass: String): Properties = Properties().apply {
            setProperty("user", name)
            setProperty("password", pass)
        }

        fun createMySql(isDebug: Boolean, database: String, host: String, port: Int, properties: Properties) = DbEngine(isDebug = isDebug, sqlite = false) {
            DriverManager.getConnection("jdbc:mysql://${host}:$port/${database}", properties).apply {
                autoCommit = false
            }
        }

        fun createFromUrl(isDebug: Boolean, url:String) = DbEngine(isDebug = isDebug, sqlite = false) {
            DriverManager.getConnection(url).apply {
                autoCommit = false
            }
        }
    }

    actual open val path: String = ""

    actual open val isSqlite: Boolean = sqlite

    private var conn: Connection? = null

   fun getConnection():Connection{
       while (conn?.isValid(5_000) != true){
           open()
       }
       return conn!!
    }

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
        with(DbInsertStatement(this@DbEngine, command)) {
            prepare()
            body(this)
        }
    }

    actual open fun exec(command: String) {
        if (isDebug) logger(command)
        getConnection().execSQL(command)
    }

    actual open fun query(
        query: String,
        callback: (cursor: Cursor) -> Unit
    ) {
        if (isDebug) logger(query)
        getConnection().apply {
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
        return { Cursor(getConnection().rawQuery(query, null)) }
    }


    actual open fun call(sql: String) {
        if (isDebug) logger(sql)
        getConnection().prepareCall(sql)?.also {
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
