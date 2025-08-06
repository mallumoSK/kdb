package tk.mallumo.kdb

import tk.mallumo.kdb.sqlite.*
import kotlin.reflect.*

class ImplKdbConnection internal constructor(
    val db: DbEngine,
    private val debug: Boolean
) {

    suspend fun exec(sql: String) {
        if (sql.isEmpty()) return
        execIUD(sql)
    }

    suspend fun exec(commands: List<String>) {
        if (commands.isEmpty()) return
        if(commands.any { it.trim().isEmpty() }) error("empty command")
        db.exec(commands)
    }

    suspend fun call(sql: String, vararg args: KProperty0<*>) {
        if (sql.isEmpty()) return
        db.call(sql, *args)
    }

    private suspend  fun execIUD(command: String) {
        if (command.isEmpty()) return
        db.exec(command)
    }

    protected suspend  fun finalize() {
        db.close()
    }
}
