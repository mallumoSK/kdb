package tk.mallumo.kdb

class ImplKdbTableDef(
    var name: String = "",
    val columns: ArrayList<Item> = arrayListOf()
) {

    fun getUnique(): List<String> = columns.filter { it.unique }.map { it.name }
    fun getIndexes(): List<String> = columns.filter { it.index }.map { it.name }

    override fun toString(): String {
        return "$name;${columns.joinToString(";") { it.toString() }}"
    }

    class Item(
        var name: String = "",
        var type: String = ColumnType.UNDEFINED,
        var defaultValue: String = "",
        var unique: Boolean = false,
        var index: Boolean = false
    ) {


        override fun toString(): String {
            return "$name-$type-$defaultValue-$unique-$index"
        }
    }

    object ColumnType {
        const val UNDEFINED = "UNDEFINED"
        const val TEXT = "TEXT"
        const val NUMERIC = "NUMERIC"
        const val INTEGER = "INTEGER"
        const val BIGINT = "BIGINT"
//        const val DOUBLE = "DOUBLE"
    }
}

fun ImplKdbTableDef.Item.sqlCreator(isSqlite: Boolean): String {
    return StringBuilder().apply {
        append("\n${name} ")
        if (type == ImplKdbTableDef.ColumnType.TEXT && !isSqlite) {
            append("LONGVARCHAR")
        } else {
            append(type)
        }
        if (defaultValue.isNotEmpty()) {
            append(" NOT NULL DEFAULT (${defaultValue})")

        }
    }.toString()
}

fun ImplKdbTableDef.sqlCreator(redeclareType: Boolean = false, isSqlite: Boolean)
        : List<String> {

    val result = arrayListOf<String>()
    if (redeclareType) {
        result.add("DROP TABLE IF EXISTS KDB_REDECLARE_TABLE_$name")
    }
    StringBuilder().apply {
        append("CREATE TABLE IF NOT EXISTS ")
        if (redeclareType) {
            append("KDB_REDECLARE_TABLE_")
        }
        append("$name (")
        columns.forEachIndexed { index, item ->
            if (index != 0) append(",")
            append(item.sqlCreator(isSqlite))
        }
        if (!redeclareType) {
            getUnique().also {
                if (it.isNotEmpty()) {
                    append(",\nUNIQUE (${it.joinToString(",")})")
                    if (isSqlite) {
                        append(" ON CONFLICT REPLACE")
                    }
                }
            }
        }

        append(")")
    }.also {
        result.add(it.toString())
    }
    return result
}

fun ImplKdbTableDef.redeclare1_Refill(): String {
    return StringBuilder().apply {
        append("INSERT INTO KDB_REDECLARE_TABLE_$name SELECT ")
        columns.forEachIndexed { index, item ->
            if (index != 0) append(",")
            append(item.name)
        }
        append(" FROM $name")
    }.toString()
}

fun ImplKdbTableDef.redeclare2_Drop(): String {
    return "DROP TABLE IF EXISTS $name"
}

fun ImplKdbTableDef.redeclare3_Rename(): String {
    return "ALTER TABLE KDB_REDECLARE_TABLE_$name RENAME TO  $name"
}

fun ImplKdbTableDef.applyIndexes(oldIndexes: List<String> = listOf()): ArrayList<String> {
    val newIndexes = getIndexes()
        .filter { index -> columns.any { it.name == index } }

    return arrayListOf<String>().apply {
        newIndexes.forEach {
            add("CREATE INDEX IF NOT EXISTS INDEX_${name}_${it} ON $name (${it})")
        }
        oldIndexes.filterNot { index -> newIndexes.any { it == index } }
            .forEach {
                add("DROP INDEX IF EXISTS INDEX_${name}_${it}")
            }
    }
}