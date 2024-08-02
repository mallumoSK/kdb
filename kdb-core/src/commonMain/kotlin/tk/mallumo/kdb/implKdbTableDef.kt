package tk.mallumo.kdb


class ImplKdbTableDef(
    var name: String = "",
    val columns: MutableList<Item> = mutableListOf()
) {

    @JvmInline
    value class IndexedField(private val data: Pair<String, String>) {
        val name get() = data.first
        val type get() = data.second
        override fun toString(): String {
            return "$name :: $type"
        }
    }

    fun getUnique(): List<String> = columns.filter { it.unique }.map { it.name }

    fun getIndexes(): List<IndexedField> = columns.filter { it.index }.map { IndexedField(it.name to it.type) }

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
    }
}

fun ImplKdbTableDef.Item.sqlCreator(isSqlite: Boolean): String {
    return StringBuilder().apply {
        append("\n${name} ")
        when (type) {
            ImplKdbTableDef.ColumnType.TEXT -> {
                if (isSqlite) append(type)
                else append("TEXT")
            }

            ImplKdbTableDef.ColumnType.NUMERIC -> {
                if (isSqlite) append(type)
                else append("DECIMAL(10,10)")
            }

            else -> append(type)
        }

        if (defaultValue.isNotEmpty()) {
            append(" NOT NULL DEFAULT (${defaultValue})")

        }
    }.toString()
}

fun ImplKdbTableDef.sqlCreator(redeclareType: Boolean = false, isSqlite: Boolean)
    : List<String> {

    val result = mutableListOf<String>()
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

        getUnique().also {
            if (it.isNotEmpty()) {
                if (isSqlite) {
                    append(",\nUNIQUE (${it.joinToString(",")})")
                    append(" ON CONFLICT REPLACE")
                } else {
                    append(",\nUNIQUE KEY `UNIQUE_ID` (${it.joinToString(",")})")
                }
            }
        }

        append(")")
    }.also {
        result.add(it.toString())
    }
    return result
}

@Suppress("FunctionName")
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

@Suppress("FunctionName")
fun ImplKdbTableDef.redeclare2_Drop(): String {
    return "DROP TABLE IF EXISTS $name"
}

@Suppress("FunctionName")
fun ImplKdbTableDef.redeclare3_Rename(): String {
    return "ALTER TABLE KDB_REDECLARE_TABLE_$name RENAME TO  $name"
}

/*
select INDEX_NAME
from information_schema.statistics
where TABLE_SCHEMA = database();

--CREATE FULLTEXT INDEX I_FLOOR_ROUTE_JSON ON FLOOR_ROUTE(JSON); //TEXT
--CREATE INDEX I_FLOOR_ROUTE_ID ON FLOOR_ROUTE(ID); //TEXTINT

 */
fun ImplKdbTableDef.applyIndexes(isSqlite: Boolean, currentIndexes: MutableList<String>): MutableList<String> {
    val newIndexes = getIndexes()
        .filter { indexedField -> columns.any { tableField -> tableField.name == indexedField.name } }

    return mutableListOf<String>().apply {
        newIndexes.forEach { index ->
            val indexName = "INDEX_${name}_${index.name}"
            if (!currentIndexes.contains(indexName)) {
                currentIndexes += indexName
                if (isSqlite) {
                    add("CREATE INDEX IF NOT EXISTS INDEX_${name}_${index.name} ON $name (${index.name})")
                } else {
                    if (index.type == ImplKdbTableDef.ColumnType.TEXT) {
                        add("CREATE FULLTEXT INDEX INDEX_${name}_${index.name} ON $name (${index.name})")
                    } else {
                        add("CREATE INDEX INDEX_${name}_${index.name} ON $name (${index.name})")
                    }
                }
            }
        }
        currentIndexes.filterNot { index -> newIndexes.any { it.name == index } }
            .forEach {
                val indexName = "INDEX_${name}_${it}"
                if (currentIndexes.contains(indexName)) {
                    currentIndexes -= indexName
                    if (isSqlite) {
                        add("DROP INDEX IF EXISTS $indexName")
                    } else {
                        add("DROP INDEX $indexName")
                    }
                }

            }
    }
}
