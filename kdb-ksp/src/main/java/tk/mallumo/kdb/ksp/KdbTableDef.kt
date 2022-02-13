@file:Suppress("unused")

package tk.mallumo.kdb.ksp

import java.util.*

class KdbTableDef(
    var name: String = "",
    val columns: ArrayList<Item> = arrayListOf()
) {

    object ColumnType {
        const val UNDEFINED = "UNDEFINED"
        const val TEXT = "TEXT"
        const val NUMERIC = "NUMERIC"
        const val INTEGER = "INTEGER"
        const val BIGINT = "BIGINT"
        const val DOUBLE = "DOUBLE"
    }

    data class Item(
        var name: String = "",
        var type: String = ColumnType.UNDEFINED,
        var defaultValue: String = "",
        var unique: Boolean = false,
        var index: Boolean = false
    )
}
