package tk.mallumo.kdb

import tk.mallumo.kdb.sqlite.*
import kotlin.reflect.*

class ImplKdbConnection internal constructor(
    val db: DbEngine,
    private val debug: Boolean
) {

    fun exec(sql: String) {
        if (sql.isEmpty()) return
        execIUD(sql)
    }

    fun call(sql: String, vararg args: KProperty0<*>) {
        if (sql.isEmpty()) return
        db.call(sql, *args)
    }

    private fun execIUD(command: String) {
        db.exec(command)
    }

    protected fun finalize() {
        db.close()
    }
}
