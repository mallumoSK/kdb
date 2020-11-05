@file:Suppress("unused", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package tk.mallumo.kdb


import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tk.mallumo.kdb.sqlite.SqliteDB


class Kdb internal constructor(
    db: SqliteDB,
    dbDefArray: ArrayList<ImplKdbTableDef>,
    isDebug: Boolean
) {

    private lateinit var connection: ImplKdbConnection
    private var isInitComplete = false

    init {
        GlobalScope.launch(coroutineKdbDispatcher) {
            db.open()
            DbRecreatingFunctions.rebuildDatabase(db, dbDefArray, isDebug)
            connection = ImplKdbConnection(db, isDebug)
            isInitComplete = true
        }
    }

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


    companion object {
        fun newInstance(
            sqlite: SqliteDB,
            isDebug: Boolean,
            dbDefArray: ArrayList<ImplKdbTableDef>
        ): Kdb = Kdb(sqlite, dbDefArray, isDebug)

        internal val kdbLock = Mutex()
    }

    suspend fun <T : Any> connection(conn: suspend ImplKdbConnection.() -> T): T {

        return withContext(coroutineKdbDispatcher) {
            while (!isInitComplete) {
                delay(2)
            }
            val resp: T
            kdbLock.withLock {
                resp = conn.invoke(connection)
            }
            resp
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