package tk.mallumo.kdb.sqlite


@Suppress("unused")
expect open class DbInsertStatement(db: DbEngine) {
    protected val ids: MutableList<Long>

    open suspend fun run(command: String, body: DbInsertStatement.() -> Unit)
    open fun string(index: Int, callback: () -> String)
    open fun int(index: Int, callback: () -> Int)
    open fun long(index: Int, callback: () -> Long)
    open fun double(index: Int, callback: () -> Double)

    open fun add()
    open fun commit(): List<Long>
    open fun close()
}
