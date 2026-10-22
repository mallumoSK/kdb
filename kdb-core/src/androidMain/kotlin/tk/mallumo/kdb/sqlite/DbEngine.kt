package tk.mallumo.kdb.sqlite

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tk.mallumo.kdb.*
import java.sql.*
import java.util.*
import kotlin.reflect.*

/**
 * if maxParallelConnections > 1, then requirements:
 *
 * ``SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;``
 */
@Suppress("unused", "UNUSED_PARAMETER")
actual open class DbEngine(
    val isDebug: Boolean,
    actual open val path: String = ""
) {

    companion object {
        fun createSQLite(isDebug: Boolean, path: String): DbEngine {
            return DbEngine(
                isDebug = isDebug,
                path = path
            )
        }
    }

    private val driver = BundledSQLiteDriver()

    private val connection = driver.open(path)

    actual open val maxConnections: Int = 1


    actual open val isSqlite: Boolean = true

    private val lock = Mutex()

    actual suspend fun connection(body: suspend DatabaseConnector.() -> Unit) {
        lock.withLock {
            body(connection as DatabaseConnector)
        }
    }


    actual open suspend fun close() {
        lock.withLock {
            connection.close()
        }
    }

    actual open suspend fun insert(command: String, body: (DbInsertStatement) -> Unit) {
        if (isDebug) logger(command)
        DbInsertStatement(this@DbEngine).run(command, body)
    }

    actual open suspend fun exec(command: String) {
        if (isDebug) logger(command)
        connection {
            execSQL(command)
        }
    }

    actual open suspend fun exec(commands: List<String>) {
        if (isDebug) {
            logger("commands: ${commands.size}x")
            commands.forEach(::println)
        }
        connection {
            execSQL(commands)
        }
    }

    actual open suspend fun query(
        query: String,
        callback: (cursor: Cursor) -> Unit
    ) {
        if (isDebug) logger(query)
        connection {
            Cursor(
                query = rawQuery(
                    query = query,
                    nothing = null
                ),
                isSqlite = isSqlite
            ).also {
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

    actual open suspend fun call(cmd: String, vararg args: KProperty0<*>) {
        if (isDebug) logger(cmd)
        val values = args.map { it.get() }
        connection {
            prepareCall(cmd)?.also { call ->
                if (args.isNotEmpty()) {
                    args.forEachIndexed { index, kProperty ->
                        call.setupArgument(index + 1, kProperty, values[index])
                    }
                }
                call.execute()
                if (args.isNotEmpty()) {
                    args.forEachIndexed { index, kProperty ->
                        call.readOutput(index + 1, kProperty, values[index])
                    }
                }
                call.close()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> CallableStatement.readOutput(index: Int, kProperty: KProperty0<T>, value: T) {
        if (kProperty !is KMutableProperty0) return
        when (value) {
            is Int -> kProperty.set(getInt(index) as T)
            is Long -> kProperty.set(getLong(index) as T)
            is String -> kProperty.set(getString(index) as T)
            is Boolean -> kProperty.set((getInt(index) == 1) as T)
            else -> error("undefined type")
        }
    }

    private fun <T> CallableStatement.setupArgument(
        index: Int,
        kProperty: KProperty0<T>,
        value: T
    ) {
        val isOutputParam = kProperty is KMutableProperty0
        when (value) {
            is Int -> {
                if (isOutputParam) registerOutParameter(index, Types.INTEGER)
                else this.setInt(index, value)
            }

            is Long -> {
                if (isOutputParam) registerOutParameter(index, Types.BIGINT)
                else this.setLong(index, value)
            }

            is String -> {
                if (isOutputParam) registerOutParameter(index, Types.VARCHAR)
                else this.setString(index, value)
            }

            is Boolean -> {
                if (isOutputParam) registerOutParameter(index, Types.INTEGER)
                else this.setInt(index, if (value) 1 else 0)
            }

            else -> error("undefined type")
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun DatabaseConnector.rawQuery(query: String, nothing: Nothing?): ResultSet {
    return createStatement().executeQuery(query)
}

private fun DatabaseConnector.transaction(body: DatabaseConnector.() -> Unit) {

    runCatching {
        this@transaction.execSQL("BEGIN IMMEDIATE TRANSACTION")
        body()
    }.onFailure {
        this@transaction.execSQL("ROLLBACK TRANSACTION")
        throw it
    }.onSuccess {
        this@transaction.execSQL("COMMIT")
    }
}

private fun DatabaseConnector.execSQL(command: String) {
    transaction {
        this@transaction.execSQL(command)
    }
}

private fun DatabaseConnector.execSQL(commands: List<String>) {
    if (commands.isNotEmpty()) {
        transaction {
            commands.forEach {
                this@transaction.execSQL(it)
            }
        }
    }
}