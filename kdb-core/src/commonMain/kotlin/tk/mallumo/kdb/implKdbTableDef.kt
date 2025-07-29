package tk.mallumo.kdb


class ImplKdbTableDef(
    var name: String = "",
    val columns: MutableList<Item> = mutableListOf()
) {

    data class IndexedField(val name: String, val type: ColumnType)

    fun getUnique(): List<String> = columns.filter { it.unique }.map { it.name }

    fun getIndexes(): List<IndexedField> = columns.filter { it.index }.map { IndexedField(it.name, it.type) }

    override fun toString(): String {
        return "$name;${columns.joinToString(";") { it.toString() }}"
    }

    data class Item(
        var name: String = "",
        var type: ColumnType = ColumnType.UNDEFINED,
        var defaultValue: String = "",
        var unique: Boolean = false,
        var index: Boolean = false,
        var size: Int = 0
    ) {


        override fun toString(): String {
            return "$name-$type-$defaultValue-$unique-$index"
        }
    }

    sealed class ColumnType(val name: String) {

        data object UNDEFINED : ColumnType("UNDEFINED")
        data object TEXT : ColumnType("TEXT")

        data object NUMERIC : ColumnType("NUMERIC")
        data object INTEGER : ColumnType("INTEGER")
        data object BIGINT : ColumnType("BIGINT")

        data object TIME : ColumnType("TIME")
        data object DATE : ColumnType("DATE")
        data object DATETIME : ColumnType("DATETIME")

        companion object{
            private val all by lazy {
                listOf(
                    UNDEFINED, TEXT,
                    NUMERIC, INTEGER, BIGINT,
                    TIME, DATE, DATETIME
                )
            }
            operator fun get(name: String): ColumnType {
                return all.first { it.name == name }
            }
        }
        override fun toString(): String = name
    }
}

fun ImplKdbTableDef.Item.sqlCreator(isSqlite: Boolean): String {
    return StringBuilder().apply {
        append("\n\t`${name}` ")
        when (type) {
            ImplKdbTableDef.ColumnType.TEXT -> {
                if (size > 0) append("VARCHAR(${size})")
                else {
                    if (isSqlite) append(type)
                    else append("TEXT")
                }
            }

            ImplKdbTableDef.ColumnType.NUMERIC -> {
                when {
                    isSqlite -> append(type)
                    size > 0 -> append("DECIMAL($size,10)")
                    else -> append("DECIMAL(20,10)")
                }
            }

            ImplKdbTableDef.ColumnType.INTEGER,
            ImplKdbTableDef.ColumnType.BIGINT -> {
                if (size > 0) append("$type(${size})")
                else append(type)
            }

            ImplKdbTableDef.ColumnType.DATE,
            ImplKdbTableDef.ColumnType.DATETIME,
            ImplKdbTableDef.ColumnType.TIME ->{
                if(isSqlite) append("TEXT")
                else append(type)
            }

            ImplKdbTableDef.ColumnType.UNDEFINED -> error("WTF column of ${this@sqlCreator}")
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

        getUnique().also { cols ->
            if (cols.isNotEmpty()) {
                if (isSqlite) {
                    append(",\nUNIQUE (${cols.joinToString(","){"`$it`"}})")
                    append(" ON CONFLICT REPLACE")
                } else {
                    append(",\nUNIQUE KEY `UNIQUE_ID` (${cols.joinToString(","){"`$it`"}})")
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
                    add("CREATE INDEX IF NOT EXISTS INDEX_${name}_${index.name} ON $name (`${index.name}`)")
                } else {
                    if (index.type == ImplKdbTableDef.ColumnType.TEXT) {
                        add("CREATE FULLTEXT INDEX INDEX_${name}_${index.name} ON $name (`${index.name}`)")
                    } else {
                        add("CREATE INDEX INDEX_${name}_${index.name} ON $name (`${index.name}`)")
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
