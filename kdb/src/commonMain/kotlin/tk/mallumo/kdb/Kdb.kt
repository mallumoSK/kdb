@file:Suppress("unused", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package tk.mallumo.kdb

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tk.mallumo.kdb.sqlite.SqliteDB


class Kdb internal constructor(
    private val db: SqliteDB,
    private val dbDefArray: ArrayList<ImplKdbTableDef>,
    private val isDebug: Boolean
) {

    companion object {
        fun newInstance(
            sqlite: SqliteDB,
            isDebug: Boolean,
            dbDefArray: ArrayList<ImplKdbTableDef>
        ): Kdb = Kdb(sqlite, dbDefArray, isDebug)

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
        withContext(coroutineKdbDispatcher) {
            kdbLock.withLock {
                initDatabase()
            }
        }
        return this
    }

    private suspend fun initDatabase() {
        if (isInitComplete) return

        db.open()
        DbRecreatingFunctions.rebuildDatabase(db, dbDefArray, isDebug)
        connection = ImplKdbConnection(db, isDebug)
        isInitComplete = true
    }

    suspend fun <T : Any> connection(conn: suspend ImplKdbConnection.() -> T): T {
        return withContext(coroutineKdbDispatcher) {
            kdbLock.withLock {
                if (!isInitComplete) {
                    initDatabase()
                }
                conn.invoke(connection)
            }
        }
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