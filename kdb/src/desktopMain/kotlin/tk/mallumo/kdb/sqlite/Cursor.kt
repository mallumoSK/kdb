@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*
import tk.mallumo.utils.tryPrint
import java.io.*
import java.sql.*
import java.util.*

@Suppress("unused")
actual class Cursor(val query: ResultSet) : Closeable {

    private val columnsCount = query.metaData.columnCount

    private val itemsCount by lazy {
        val currentIndex = query.row
        query.last()
        query.row.let {
            query.absolute(currentIndex)
            it
        }
    }

    actual val columns: Array<String> =
        if (columnsCount == 0) arrayOf()
        else (1..columnsCount).map {
            query.metaData
                .getColumnLabel(it)
                .lowercase(Locale.getDefault())
        }.toTypedArray()

    actual val size: Int by lazy {
        itemsCount
    }

    actual fun next(): Boolean = query.next()

    actual fun moveTo(position: Int): Boolean = query.absolute(position + 1)

    actual fun previous(): Boolean = query.previous()

    actual override fun close() {
        query.close()
    }

    actual fun string(index: Int, callback: (String) -> Unit) {
        tryPrint {
            query.getString(index + 1)?.also {
                callback.invoke(it)
            }
        }
    }

    actual fun int(index: Int, callback: (Int) -> Unit) {
        tryPrint {
            query.getInt(index + 1).also {
                callback.invoke(it)
            }
        }
    }

    actual fun long(index: Int, callback: (Long) -> Unit) {
        tryPrint {
            query.getLong(index + 1).also {
                callback.invoke(it)
            }
        }
    }

    actual fun double(index: Int, callback: (Double) -> Unit) {
        tryPrint {
            query.getDouble(index + 1).also {
                callback.invoke(it)
            }
        }
    }


}