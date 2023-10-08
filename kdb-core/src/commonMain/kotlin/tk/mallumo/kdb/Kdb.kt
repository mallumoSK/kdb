@file:Suppress("unused")

package tk.mallumo.kdb

import kotlinx.coroutines.sync.*
import tk.mallumo.kdb.sqlite.*


class Kdb internal constructor(
    private val db: SqliteDB,
    private val dbDefArray: MutableList<ImplKdbTableDef>,
    private val isDebug: Boolean,
    internal val beforeInit: suspend SqliteDB.() -> Unit = {},
    internal val afterInit: suspend SqliteDB.() -> Unit = {},
    internal val beforeDatabaseChange: suspend SqliteDB.() -> Unit = {},
    internal val afterDatabaseChange: suspend SqliteDB.() -> Unit = {},
) {

    companion object {
        @Deprecated(
            message = "use Kdb.Companion.get(sqlite, beforeInit, afterInit, beforeDatabaseChange, afterDatabaseChange)",
            replaceWith = ReplaceWith("get"),
            level = DeprecationLevel.WARNING
        )
        fun newInstance(
            sqlite: SqliteDB,
            isDebug: Boolean,
            dbDefArray: MutableList<ImplKdbTableDef>,
            beforeInit: suspend SqliteDB.() -> Unit = {},
            afterInit: suspend SqliteDB.() -> Unit = {},
            beforeDatabaseChange: suspend SqliteDB.() -> Unit = {},
            afterDatabaseChange: suspend SqliteDB.() -> Unit = {},
        ): Kdb = Kdb(
            sqlite,
            dbDefArray,
            isDebug,
            beforeInit,
            afterInit,
            beforeDatabaseChange,
            afterDatabaseChange
        )

        internal val kdbLock = Mutex()
    }

    private lateinit var connection: ImplKdbConnection
    private var isInitComplete = false

    val insert by lazy {
        ImplKdbCommand.Insert(this)
    }

    val delete by lazy {
        ImplKdbCommand.Delete(this)
    }

    val update by lazy {
        ImplKdbCommand.Update(this)
    }

    val query by lazy {
        ImplKdbCommand.Query(this)
    }

    suspend fun prepareDatabase(): Kdb {
        kdbLock.withLock { initDatabase() }
        return this
    }

    private suspend fun initDatabase() {
        if (isInitComplete) return

        db.open()
        db.beforeInit()
        DbRecreatingFunctions.rebuildDatabase(this, db, dbDefArray, isDebug)
        connection = ImplKdbConnection(db, isDebug)
        isInitComplete = true
        db.afterInit()
    }

    suspend fun <T : Any> connection(conn: suspend ImplKdbConnection.() -> T): T = kdbLock.withLock {
        if (!isInitComplete) {
            initDatabase()
        }
        conn.invoke(connection)
    }

    suspend fun exec(sql: String) {
        connection {
            exec(sql)
        }
    }

    suspend fun call(sql: String) {
        connection {
            call(sql)
        }
    }
}