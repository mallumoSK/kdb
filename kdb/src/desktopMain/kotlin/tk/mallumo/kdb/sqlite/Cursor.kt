@file:Suppress("MemberVisibilityCanBePrivate")

package tk.mallumo.kdb.sqlite

import tk.mallumo.kdb.*
import java.io.*
import java.sql.*
import java.util.*

@Suppress("unused")
actual open class Cursor(val query: ResultSet) : Closeable {

    private val columnsCount = query.metaData.columnCount

    private val itemsCount by lazy {
        val currentIndex = query.row
        query.last()
        query.row.let {
            query.absolute(currentIndex)
            it
        }
    }

    actual open val columns: Array<String> =
        if (columnsCount == 0) arrayOf()
        else (1..columnsCount).map {
            query.metaData
                .getColumnLabel(it)
                .lowercase(Locale.getDefault())
        }.toTypedArray()

    actual open val size: Int by lazy {
        itemsCount
    }

    actual open fun next(): Boolean = query.next()

    actual open fun moveTo(position: Int): Boolean = query.absolute(position + 1)

    actual open fun previous(): Boolean = query.previous()

    actual override fun close() {
        query.close()
    }

    actual open fun string(index: Int, callback: (String) -> Unit) {
        runCatching {
            query.getString(index + 1)?.also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    actual open fun int(index: Int, callback: (Int) -> Unit) {
        runCatching {
            query.getInt(index + 1).also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    actual open fun long(index: Int, callback: (Long) -> Unit) {
        runCatching {
            query.getLong(index + 1).also {
                callback.invoke(it)
            }
        }
    }

    actual open fun double(index: Int, callback: (Double) -> Unit) {
        runCatching {
            query.getDouble(index + 1).also {
                callback.invoke(it)
            }
        }.onFailure { it.printStackTrace() }
    }


}
