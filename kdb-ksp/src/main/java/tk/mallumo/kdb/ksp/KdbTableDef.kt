package tk.mallumo.kdb.ksp

import java.util.*

class KdbTableDef(
    var name: String = "",
    val columns: ArrayList<Item> = arrayListOf()
) {

    object ColumnType {
        val UNDEFINED = "UNDEFINED"
        val TEXT = "TEXT"
        val NUMERIC = "NUMERIC"
        val INTEGER = "INTEGER"
        val BIGINT = "BIGINT"
        val DOUBLE = "DOUBLE"
    }

    data class Item(
        var name: String = "",
        var type: String = ColumnType.UNDEFINED,
        var defaultValue: String = "",
        var unique: Boolean = false,
        var index: Boolean = false
    )
}
