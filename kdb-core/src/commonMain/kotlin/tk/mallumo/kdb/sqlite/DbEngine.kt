package tk.mallumo.kdb.sqlite

import java.sql.Connection
import kotlin.reflect.*

@Suppress("unused")
expect open class DbEngine {

    open val path: String
    open val isSqlite: Boolean
    open val maxConnections: Int

    open suspend fun close()
    open suspend fun insert(command: String, body: (DbInsertStatement) -> Unit)
    open suspend fun exec(command: String)
    open suspend fun exec(commands: List<String>)
    open suspend fun query(query: String, callback: (cursor: Cursor) -> Unit)
    open suspend fun call(cmd: String, vararg args: KProperty0<*>)
    suspend fun connection(body: suspend Connection.() -> Unit)
}
