package tk.mallumo.kdb.sqlite

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
