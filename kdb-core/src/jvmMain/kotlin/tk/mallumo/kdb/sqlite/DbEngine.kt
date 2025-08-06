package tk.mallumo.kdb.sqlite

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
    maxParallelConnections: Int,
    sqlite: Boolean,
    private val connectionCallback: () -> Connection
) {

    companion object {
        fun createSQLite(isDebug: Boolean, path: String) = DbEngine(
            isDebug = isDebug,
            maxParallelConnections = 1,
            sqlite = true
        ) {
            DriverManager.getConnection("jdbc:sqlite:${path}").apply {
                autoCommit = false
            }
        }

        /**
         * if maxParallelConnections > 1, then requirements:
         *
         * ``SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;``
         */
        fun createMySql(maxParallelConnections: Int = 1, isDebug: Boolean, name: String, pass: String, database: String, host: String, port: Int) = DbEngine(
            isDebug = isDebug,
            maxParallelConnections = maxParallelConnections,
            sqlite = false
        ) {
            DriverManager.getConnection("jdbc:mysql://${host}:$port/${database}", name, pass).apply {
                autoCommit = false
            }
        }

        /**
         * if maxParallelConnections > 1, then requirements:
         *
         * ``SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;``
         */
        fun createMadiaDb(maxParallelConnections: Int = 1, isDebug: Boolean, name: String, pass: String, database: String, host: String, port: Int) = DbEngine(
            isDebug = isDebug,
            maxParallelConnections = maxParallelConnections,
            sqlite = false
        ) {
            DriverManager.getConnection("jdbc:mariadb://${host}:$port/${database}", name, pass).apply {
                autoCommit = false
            }
        }

        fun createProperties(name: String, pass: String): Properties = Properties().apply {
            setProperty("user", name)
            setProperty("password", pass)
        }

        /**
         * if maxParallelConnections > 1, then requirements:
         *
         * ``SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;``
         */
        fun createMySql(maxParallelConnections: Int = 1, isDebug: Boolean, database: String, host: String, port: Int, properties: Properties) = DbEngine(
            isDebug = isDebug,
            maxParallelConnections = maxParallelConnections,
            sqlite = false
        ) {
            DriverManager.getConnection("jdbc:mysql://${host}:$port/${database}", properties).apply {
                autoCommit = false
            }
        }

        /**
         * if maxParallelConnections > 1, then requirements:
         *
         * ``SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;``
         */
        fun createMadiaDb(maxParallelConnections: Int = 1, isDebug: Boolean, database: String, host: String, port: Int, properties: Properties) = DbEngine(
            isDebug = isDebug,
            maxParallelConnections = maxParallelConnections,
            sqlite = false
        ) {
            DriverManager.getConnection("jdbc:mariadb://${host}:$port/${database}", properties).apply {
                autoCommit = false
            }
        }

        /**
         * if maxParallelConnections > 1, then requirements:
         *
         * ``SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;``
         */
        fun createFromUrl(maxParallelConnections: Int = 1, isDebug: Boolean, url: String) = DbEngine(
            isDebug = isDebug,
            maxParallelConnections = maxParallelConnections,
            sqlite = false
        ) {
            DriverManager.getConnection(url).apply {
                autoCommit = false
            }
        }
    }

    actual open val maxConnections: Int = maxParallelConnections

    actual open val path: String = ""

    actual open val isSqlite: Boolean = sqlite

    private val connStackFree = mutableListOf<Connection>()

    private var connStackSize = 0

    private val lock = Mutex()

    actual suspend fun connection(body: suspend Connection.() -> Unit) {
        val connection = findConnection()

        runCatching {
            body(connection)
        }.onFailure {
            connStackSize -= 1
            connection.runCatching {
                close()
            }
            throw it
        }.onSuccess {
            lock.withLock {
                connStackFree += connection
            }
        }
    }

    private suspend fun findConnection(): Connection {
        val result = lock.withLock {
            val newConnectionRequired = connStackSize == 0
                    || (connStackSize < maxConnections && connStackFree.isEmpty())

            if (newConnectionRequired) {
                connectionCallback.invoke().also {
                    connStackSize += 1
                }
            } else if (connStackFree.isNotEmpty()) {
                connStackFree.removeFirst().let {
                    if (it.isValid(1000)) it
                    else null
                }
            } else null
        }


        return if (result == null) {
            delay(50)
            findConnection()
        } else result
    }


    actual open suspend fun close() {
        lock.withLock {
            while (connStackFree.isNotEmpty()) {
                connStackFree.removeFirst().runCatching {
                    close()
                }
                connStackSize -= 1
            }
            connStackSize = 0
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

    private fun <T> CallableStatement.setupArgument(index: Int, kProperty: KProperty0<T>, value: T) {
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
private fun Connection.rawQuery(query: String, nothing: Nothing?): ResultSet {
    return createStatement().executeQuery(query)
}

private fun Connection.execSQL(command: String) {
    createStatement().use {
        it.execute(command)
    }
    commit()
}

private fun Connection.execSQL(commands: List<String>) {
    if(commands.isNotEmpty()){
        createStatement().use { statement ->
            commands.forEach { cmd ->
                statement.addBatch(cmd.trim())
            }
            statement.executeBatch()
        }
        commit()
    }
}