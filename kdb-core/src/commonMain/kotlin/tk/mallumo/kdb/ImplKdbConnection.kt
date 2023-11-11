package tk.mallumo.kdb

import tk.mallumo.kdb.sqlite.*

class ImplKdbConnection internal constructor(
    val db: DbEngine,
    private val debug: Boolean
) {

    fun exec(sql: String) {
        if (sql.isEmpty()) return
        if (debug) logger(sql)
        execIUD(sql)
    }

    fun call(sql: String) {
        if (sql.isEmpty()) return
        if (debug) logger(sql)
        db.call(sql)
    }

    private fun execIUD(command: String) {
        if (debug) logger(command)
        db.exec(command)
    }

    protected fun finalize() {
        db.close()
    }
}
