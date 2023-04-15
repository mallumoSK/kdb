@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*
import java.io.*
import java.util.*

@Suppress("unused")
actual open class Cursor(val query: android.database.Cursor) : Closeable {

    actual open val columns: Array<String> = query.columnNames.map {
        it.lowercase(Locale.ROOT)
    }.toTypedArray()

    actual open val size: Int = query.count

    actual open fun next(): Boolean = query.moveToNext()

    actual open fun moveTo(position: Int): Boolean = query.moveToPosition(position)

    actual open fun previous(): Boolean = query.moveToPrevious()

    actual override fun close() {
        query.close()
    }

    actual open fun string(index: Int, callback: (String) -> Unit) {
        runCatching {
            query.getString(index)?.also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    actual open fun int(index: Int, callback: (Int) -> Unit) {
        runCatching {
            query.getInt(index).also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    actual open fun long(index: Int, callback: (Long) -> Unit) {
        runCatching {
            query.getLong(index).also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    actual open fun double(index: Int, callback: (Double) -> Unit) {
        runCatching {
            query.getDouble(index).also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

}
