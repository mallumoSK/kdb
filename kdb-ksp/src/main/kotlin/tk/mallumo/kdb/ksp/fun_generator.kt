package tk.mallumo.kdb.ksp

fun generateCreator() = """
private val databases = hashMapOf<String,Kdb>()
 
fun Kdb.Companion.get(
    sqlite:tk.mallumo.kdb.sqlite.DbEngine,
    reconfigureDatabaseOnStart:Boolean = true,
    beforeInit: suspend tk.mallumo.kdb.sqlite.DbEngine.() -> Unit = {},
    afterInit: suspend tk.mallumo.kdb.sqlite.DbEngine.() -> Unit = {},
    beforeDatabaseChange: suspend tk.mallumo.kdb.sqlite.DbEngine.() -> Unit = {},
    afterDatabaseChange: suspend tk.mallumo.kdb.sqlite.DbEngine.() -> Unit = {})
    :Kdb{
        return databases.getOrPut(sqlite.path){
            @Suppress("DEPRECATION")
            Kdb.Companion.newInstance(
                sqlite,
                sqlite.isDebug,
                reconfigureDatabaseOnStart,
                KdbGeneratedDefStructure.getTablesDef(),
                beforeInit,
                afterInit,
                beforeDatabaseChange,
                afterDatabaseChange
            )
        }
}   
"""

fun generateDefStructure(
    map: List<TableNode>
) = buildString {
    append("object KdbGeneratedDefStructure{\n")

    val tables =
        map.joinToString(separator = ",\n", prefix = "mutableListOf(", postfix = ")") {
            "getTableDefItem(${it.qualifiedName}())"
        }
    append("\n\tfun getTablesDef(): MutableList<ImplKdbTableDef> = $tables\n")

    map.forEach { entry ->
        val columns = entry.property.map {
            "\t\t\t\tImplKdbTableDef.Item(name = \"${it.propertyName.uppercase()}\", type = ImplKdbTableDef.ColumnType.${it.sqlColumnTypeName}, defaultValue = ${it.defaultValue}, unique = ${it.isUnique}, index = ${it.isIndex}, size = ${it.columnSize})"
        }.joinToString(",\n", prefix = "mutableListOf(\n", postfix = ")")

        append(
            """
    private fun getTableDefItem(instance: ${entry.qualifiedName}): ImplKdbTableDef = ImplKdbTableDef(
            "${entry.simpleName}",
            $columns
    )
"""
        )
    }
    append("}")
}

fun generateIndexFunctions(
    map: List<TableNode>
) = buildString {
    append("object KdbGeneratedIndex{\n")

    map.forEach { entry ->

        val queryColumns = entry.property
            .joinToString(",\n", prefix = "intArrayOf(\n", postfix = ")") {
                "\t\t\tcolumns.indexOf(\"${it.propertyName.uppercase()}\")"
            }
        append(
            """
    fun index_${entry.functionName}(cursor: Cursor): IntArray {
        val columns = cursor.columns.map { it.uppercase() }
        return $queryColumns
    }
"""
        )
    }
    append("}")
}

fun generateFillFunctions(
    map: List<TableNode>
) = buildString {
    append("object KdbGeneratedFill{\n")

    map.forEach { entry ->
        val mapping = entry.property.mapIndexed { index, property ->
            when (property.qualifiedName) {
                "kotlin.String" -> "\t\t\tif (indexArray[$index] != -1) cursor.string(indexArray[$index]) { ${property.propertyName} = it }"
                "kotlin.Int" -> "\t\t\tif (indexArray[$index] != -1) cursor.int(indexArray[$index]) { ${property.propertyName} = it }"
                "kotlin.Long" -> "\t\t\tif (indexArray[$index] != -1) cursor.long(indexArray[$index]) { ${property.propertyName} = it }"
                "kotlin.Double" -> "\t\t\tif (indexArray[$index] != -1) cursor.double(indexArray[$index]) { ${property.propertyName} = it }"
                "kotlin.Float" -> "\t\t\tif (indexArray[$index] != -1) cursor.double(indexArray[$index]) { ${property.propertyName} = it.toFloat() }"
                else -> ""
            }
        }.joinToString("\n")

        append(
            """
    fun fill_${entry.functionName}(item:${entry.qualifiedName}, cursor: tk.mallumo.kdb.sqlite.Cursor, indexArray: IntArray) {
        item.apply {
$mapping
        }
    }
"""
        )
    }

    append("}")
}


fun generateCursorFunctions(
    map: List<TableNode>
) = buildString {
    append("object KdbGeneratedQuery{\n")
    map.forEach { entry ->
        append(
            """
    fun query_${entry.functionName}(cursor: tk.mallumo.kdb.sqlite.Cursor): MutableList<${entry.qualifiedName}> {
        val out = mutableListOf<${entry.qualifiedName}>()
        if (cursor.columns.isNotEmpty()) {
            val indexArray = KdbGeneratedIndex.index_${entry.functionName}(cursor)
            while (cursor.next()) {
                out.add(${entry.qualifiedName}().apply {
                   KdbGeneratedFill.fill_${entry.functionName}(this, cursor, indexArray)
                })
            }
        }
        return out
    }
"""
        )
    }

    append("}")
}

fun generateInsertFunctions(
    map: List<TableNode>
) = buildString {
    append("object KdbGeneratedInsert{\n")

    map.forEach { entry ->

        val columnNamesAreUnique = entry.property
            .filter { it.isUnique }
            .map { "`${it.propertyName.uppercase()}`" }

        val columnNamesNotUnique = entry.property
            .filter { !it.isUnique }
            .map { "`${it.propertyName.uppercase()}`" }

        val comumns = entry.property.map { "`${it.propertyName.uppercase()}`" }
            .joinToString(",", prefix = "(", postfix = ")")

        val values = entry.property.map { "?" }
            .joinToString(",", prefix = "(", postfix = ")")

        val insertVal = entry.property.mapIndexed { index, prop ->
            when {
                prop.qualifiedName == "kotlin.Float" -> {
                    "\t\t\t\t\tit.${prop.cursorTypeName}($index) { item.${prop.propertyName}.toDouble() }"
                }

                PropertyTypeHolder.directTypes.none { it == prop.qualifiedName } -> {
                    "\t\t\t\t\tit.${prop.cursorTypeName}($index) { item.${prop.propertyName}.toString() }"
                }

                else -> {
                    "\t\t\t\t\tit.${prop.cursorTypeName}($index) { item.${prop.propertyName} }"
                }
            }
        }.joinToString("\n", prefix = "\n", postfix = "\n")

        val suffixStatement = buildString {
            if(columnNamesAreUnique.count() == 0){
                append("val sufix =\"\"")
            }else{
                append("val sufix = if(db.isSqlite) \"\"")
                appendLine()
                append("\t\telse \" ON DUPLICATE KEY UPDATE ")
                if(columnNamesNotUnique.count()==0){
                    append(columnNamesAreUnique.joinToString(",") { "${it}=VALUES($it)" })
                }else{
                    append(columnNamesNotUnique.joinToString(",") { "${it}=VALUES($it)" })
                }
                append("\"")
            }
        }

        val insert = """"INSERT INTO ${
            entry.simpleName
        } $comumns VALUES $values ${"\$"}sufix" """


        append(
            """
    fun insert_${entry.functionName}(items: Array<${entry.qualifiedName}>, db: DbEngine):List<Long>{
        var rowIds:List<Long> = emptyList()
        if (items.isEmpty()) return rowIds
    
        $suffixStatement
        
        db.insert(${insert}) {
            items.forEach { item ->
$insertVal
                it.add()
            }
            rowIds = it.commit()
        }
        return rowIds
    }
"""
        )
    }

    append("}")
}

fun generateExtCursorFunctions(
    map: List<TableNode>
) = buildString {
    map.forEach { entry ->
        append(
            """
suspend fun ImplKdbCommand.Query.${entry.niceClassName}(query: String, params: Map<String, Any?> = mapOf()): MutableList<${entry.qualifiedName}> {
    var resp = mutableListOf<${entry.qualifiedName}>()
    kdb.connection {
        db.query(ImplKdbUtilsFunctions.mapQueryParams(query, params)) { cursor ->
            resp = KdbGeneratedQuery.query_${entry.functionName}(cursor)
        }
    }
    return resp
}
"""
        )
    }
}

fun generateExtDeleteFunctions(
    map: List<TableNode>
) = buildString {
    map.forEach { entry ->
        append(
            """
suspend fun ImplKdbCommand.Delete.${entry.niceClassName}(where: String = "1=1") {
    val command = "DELETE FROM ${
                entry.simpleName
            } WHERE ${'$'}where"
    kdb.connection {        
        db.exec(command)
    }
}
"""
        )

    }
}

fun generateExtUpdateFunctions(
    map: List<TableNode>
) = buildString {
    map.forEach { entry ->
        val command = "UPDATE ${
            entry.simpleName
        } SET ${'$'}{ImplKdbUtilsFunctions.mapUpdateParams(params)}  WHERE ${'$'}where"
        append(
            """
suspend fun ImplKdbCommand.Update.${entry.niceClassName}(where: String = "1=1", params: Map<String, Any?> = mapOf()) {
    kdb.connection {
        db.exec("$command")
    }
}
"""
        )

    }
}

fun generateExtInsertFunctions(

    map: List<TableNode>
) = buildString {
    map.forEach { entry ->
        append(
            """
suspend fun ImplKdbCommand.Insert.${entry.niceClassName}(item: ${entry.qualifiedName}): List<Long> = 
    ${entry.niceClassName}(arrayOf(item))

suspend fun ImplKdbCommand.Insert.${entry.niceClassName}(items: List<${entry.qualifiedName}>): List<Long> = 
    ${entry.niceClassName}(items.toTypedArray())

suspend fun ImplKdbCommand.Insert.${entry.niceClassName}(items: Array<${entry.qualifiedName}>): List<Long> = 
    kdb.connection { KdbGeneratedInsert.insert_${entry.functionName}(items, db) }
"""
        )
    }
}

