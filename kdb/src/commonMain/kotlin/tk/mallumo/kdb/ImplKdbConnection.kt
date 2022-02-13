package tk.mallumo.kdb

import tk.mallumo.kdb.sqlite.SqliteDB

class ImplKdbConnection internal constructor(
    val db: SqliteDB,
    private val debug: Boolean
) {

    fun exec(sql: String) {
        if (sql.isEmpty()) return
        if (debug) log(sql)
        execIUD(sql)
    }

    fun call(sql: String) {
        if (sql.isEmpty()) return
        if (debug) log(sql)
        db.call(sql)
    }

    private fun execIUD(command: String) {
        if (debug) log(command)
        db.exec(command)
    }

    protected fun finalize() {
        db.close()
    }
}