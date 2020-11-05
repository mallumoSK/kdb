package tk.mallumo.kdb.sqlite


@Suppress("SpellCheckingInspection", "unused", "UNUSED_PARAMETER")
expect class SqliteDB {

    val path: String

    fun open()
    fun close()
    fun insert(command: String, body: (DbInsertStatement) -> Unit)
    fun exec(command: String)
    fun query(query: String, callback: (cursor: Cursor) -> Unit)
    fun queryUnclosed(query: String): ((Cursor) -> Unit)
    fun call(sql: String)
}

@Suppress("unused", "UNUSED_PARAMETER")
expect class Cursor {

    val columns: Array<String>
    val size: Int

    fun next(): Boolean
    fun moveTo(position: Int): Boolean
    fun previous(): Boolean
    fun close()

    fun string(index: Int, callback: (String) -> Unit)
    fun int(index: Int, callback: (Int) -> Unit)
    fun long(index: Int, callback: (Long) -> Unit)
    fun double(index: Int, callback: (Double) -> Unit)
}

@Suppress("unused", "UNUSED_PARAMETER")
expect class DbInsertStatement(db: SqliteDB, command: String) {

    fun string(index: Int, callback: () -> String)
    fun int(index: Int, callback: () -> Int)
    fun long(index: Int, callback: () -> Long)
    fun double(index: Int, callback: () -> Double)

    fun add()
    fun commit()
    fun close()

}

