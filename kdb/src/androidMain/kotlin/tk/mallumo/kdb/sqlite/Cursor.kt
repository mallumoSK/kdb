@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*
import java.io.*
import java.util.*

@Suppress("unused")
actual class Cursor(val query: android.database.Cursor) : Closeable {

    actual val columns: Array<String> = query.columnNames.map {
        it.lowercase(Locale.ROOT)
    }.toTypedArray()

    actual val size: Int = query.count

    actual fun next(): Boolean = query.moveToNext()

    actual fun moveTo(position: Int): Boolean = query.moveToPosition(position)

    actual fun previous(): Boolean = query.moveToPrevious()

    actual override fun close() {
        query.close()
    }

    actual fun string(index: Int, callback: (String) -> Unit) {
        runCatching {
            query.getString(index)?.also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    actual fun int(index: Int, callback: (Int) -> Unit) {
        runCatching {
            query.getInt(index).also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    actual fun long(index: Int, callback: (Long) -> Unit) {
        runCatching {
            query.getLong(index).also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    actual fun double(index: Int, callback: (Double) -> Unit) {
        runCatching {
            query.getDouble(index).also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

}
