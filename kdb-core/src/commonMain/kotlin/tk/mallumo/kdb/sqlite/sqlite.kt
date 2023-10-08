package tk.mallumo.kdb.sqlite


@Suppress("unused")
expect open class SqliteDB {

    open val path: String

    open fun open()
    open fun close()
    open fun insert(command: String, body: (DbInsertStatement) -> Unit)
    open fun exec(command: String)
    open fun query(query: String, callback: (cursor: Cursor) -> Unit)
    open fun queryUnclosed(query: String): ((Cursor) -> Unit)
    open fun call(sql: String)
}

@Suppress("unused")
expect open class Cursor {

    open val columns: Array<String>
    open val size: Int

    open fun next(): Boolean
    open fun moveTo(position: Int): Boolean
    open fun previous(): Boolean
    open fun close()

    open fun string(index: Int, callback: (String) -> Unit)
    open fun int(index: Int, callback: (Int) -> Unit)
    open fun long(index: Int, callback: (Long) -> Unit)
    open fun double(index: Int, callback: (Double) -> Unit)
}

@Suppress("unused")
expect open class DbInsertStatement(db: SqliteDB, command: String) {
    protected val ids: MutableList<Long>

    open fun prepare()

    open fun string(index: Int, callback: () -> String)
    open fun int(index: Int, callback: () -> Int)
    open fun long(index: Int, callback: () -> Long)
    open fun double(index: Int, callback: () -> Double)

    open fun add()
    open fun commit(): List<Long>
    open fun close()
}

