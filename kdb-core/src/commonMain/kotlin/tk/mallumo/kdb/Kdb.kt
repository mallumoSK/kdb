@file:Suppress("unused")

package tk.mallumo.kdb

import kotlinx.coroutines.sync.*
import tk.mallumo.kdb.sqlite.*


class Kdb internal constructor(
    private val db: DbEngine,
    private val dbDefArray: MutableList<ImplKdbTableDef>,
    private val isDebug: Boolean,
    private val reconfigureDatabaseOnStart:Boolean = true,
    internal val beforeInit: suspend DbEngine.() -> Unit = {},
    internal val afterInit: suspend DbEngine.() -> Unit = {},
    internal val beforeDatabaseChange: suspend DbEngine.() -> Unit = {},
    internal val afterDatabaseChange: suspend DbEngine.() -> Unit = {},
) {

    companion object {
        @Deprecated(
            message = "use Kdb.Companion.get(sqlite, beforeInit, reconfigureDatabaseOnStart, afterInit, beforeDatabaseChange, afterDatabaseChange)",
            replaceWith = ReplaceWith("get"),
            level = DeprecationLevel.WARNING
        )
        fun newInstance(
            sqlite: DbEngine,
            isDebug: Boolean,
            reconfigureDatabaseOnStart:Boolean = true,
            dbDefArray: MutableList<ImplKdbTableDef>,
            beforeInit: suspend DbEngine.() -> Unit = {},
            afterInit: suspend DbEngine.() -> Unit = {},
            beforeDatabaseChange: suspend DbEngine.() -> Unit = {},
            afterDatabaseChange: suspend DbEngine.() -> Unit = {}
        ): Kdb = Kdb(
            sqlite,
            dbDefArray,
            isDebug,
            reconfigureDatabaseOnStart,
            beforeInit,
            afterInit,
            beforeDatabaseChange,
            afterDatabaseChange,
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
        if(reconfigureDatabaseOnStart){
            DbRecreatingFunctions.rebuildDatabase(this, db, dbDefArray, isDebug)
        }
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
