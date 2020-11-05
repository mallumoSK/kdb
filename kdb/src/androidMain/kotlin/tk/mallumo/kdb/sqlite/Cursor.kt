package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.tryPrint
import java.io.Closeable
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
actual class Cursor(val query: android.database.Cursor) : Closeable {

    actual val columns: Array<String> = query.columnNames.map {
        it.toLowerCase(Locale.ROOT)
    }.toTypedArray()

    actual val size: Int = query.count

    actual fun next(): Boolean = query.moveToNext()

    actual fun moveTo(position: Int): Boolean = query.moveToPosition(position)

    actual fun previous(): Boolean = query.moveToPrevious()

    actual override fun close() {
        query.close()
    }

    actual fun string(index: Int, callback: (String) -> Unit) {
        tryPrint {
            query.getString(index)?.also {
                callback.invoke(it)
            }
        }
    }

    actual fun int(index: Int, callback: (Int) -> Unit) {
        tryPrint {
            query.getInt(index).also {
                callback.invoke(it)
            }
        }
    }

    actual fun long(index: Int, callback: (Long) -> Unit) {
        tryPrint {
            query.getLong(index).also {
                callback.invoke(it)
            }
        }
    }

    actual fun double(index: Int, callback: (Double) -> Unit) {
        tryPrint {
            query.getDouble(index).also {
                callback.invoke(it)
            }
        }
    }


}