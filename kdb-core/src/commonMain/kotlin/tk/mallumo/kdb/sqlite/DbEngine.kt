package tk.mallumo.kdb.sqlite

@Suppress("unused")
expect open class DbEngine {

    open val path: String
    open val isSqlite: Boolean

    open fun open()
    open fun close()
    open fun insert(command: String, body: (DbInsertStatement) -> Unit)
    open fun exec(command: String)
    open fun query(query: String, callback: (cursor: Cursor) -> Unit)
    open fun queryUnclosed(query: String): ((Cursor) -> Unit)
    open fun call(sql: String)
}
