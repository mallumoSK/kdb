package tk.mallumo.kdb.ksp

import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import java.util.*

private val KSClassDeclaration.functionName: String
    get() = qualifiedName!!.asString().replace(".", "_")

val KSClassDeclaration.niceClassName: String
    get() = simpleName.asString().split("_")
        .map { part ->
            var joined = ""
            part.forEachIndexed { index, c ->
                when {
                    index == 0 -> {
                        joined += c.toLowerCase()
                    }
                    part.lastIndex == index -> {
                        joined += c.toLowerCase()
                    }
                    c.isLowerCase() && part[index + 1].isUpperCase() -> {
                        joined += "${c}_"
                    }
                    else -> joined += c.toLowerCase()
                }
            }
            joined
        }.joinToString("_")

object KdbMode {
    const val ANDROID = "ANDROID"
    const val JVM_DESKTOP = "JVM-DESKTOP"
}

fun generateCreator(codeWriter: CodeWriter, mode: String) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbGenerated.kt",
    ) {
        append("private val databases = hashMapOf<String,Kdb>()\n")
        when (mode) {
            KdbMode.ANDROID -> {
                append(
                    """
fun createKDB(context:android.content.Context, name:String="default-kdb.sqlite",isDebug: Boolean = true):Kdb{
        return databases.getOrPut(name){
           Kdb.newInstance(tk.mallumo.kdb.sqlite.SqliteDB(isDebug, context.getDatabasePath(name).absolutePath), isDebug, KdbGeneratedDefStructure.getTablesDef())
        }
}   
"""
                )
            }
            KdbMode.JVM_DESKTOP -> {
                append(
                    """
fun createKDB(name:String, isDebug: Boolean = true, connectionCallback:() -> java.sql.Connection):Kdb{
        return databases.getOrPut(name){
            Kdb.newInstance(tk.mallumo.kdb.sqlite.SqliteDB(isDebug, true, connectionCallback), isDebug, KdbGeneratedDefStructure.getTablesDef())
        }
}    
"""
                )
            }
        }
    }

}

fun generateDefStructure(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbGeneratedDefStructure.kt",
        suppress = listOf("RemoveRedundantQualifierName", "SpellCheckingInspection")
    ) {
        append("object KdbGeneratedDefStructure{\n")

        val tables =
            map.keys.joinToString(separator = ",\n", prefix = "arrayListOf(", postfix = ")") {
                "getTableDefItem(${it.qualifiedName!!.asString()}())"
            }
        append("\n\tfun getTablesDef(): ArrayList<ImplKdbTableDef> = $tables\n")

        map.entries.forEach { entry ->
            val columns = entry.value.map {
                "\t\t\t\tImplKdbTableDef.Item(name = \"${it.propertyName.toUpperCase(Locale.ROOT)}\", type = ImplKdbTableDef.ColumnType.${it.sqlColumnTypeName}, defaultValue = ${it.defaultValue}, unique = ${it.isUnique}, index = ${it.isIndex})"
            }.joinToString(",\n", prefix = "arrayListOf(\n", postfix = ")")

            append(
                """
    private fun getTableDefItem(instance: ${entry.key.qualifiedName!!.asString()}): ImplKdbTableDef = ImplKdbTableDef(
            "${entry.key.simpleName.asString().toUpperCase(Locale.ROOT)}",
            $columns
    )
"""
            )
        }
        append("}")
    }
}

fun generateIndexFunctions(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbGeneratedIndex.kt",
        imports = listOf(
            "tk.mallumo.kdb.sqlite.Cursor",
            "java.util.Locale"
        ),
        suppress = listOf("FunctionName", "SpellCheckingInspection")
    ) {
        append("object KdbGeneratedIndex{\n")

        map.entries.forEach { entry ->

            val queryColumns = entry.value
                .map { "\t\t\tcolumns.indexOf(\"${it.propertyName.toUpperCase(java.util.Locale.getDefault())}\")" }
                .joinToString(",\n", prefix = "intArrayOf(\n", postfix = ")")
            append(
                """
    fun index_${entry.key.functionName}(cursor: Cursor): IntArray {
        val columns = cursor.columns.map { it.toUpperCase(Locale.ROOT) }
        return $queryColumns
    }
"""
            )
        }

        append("}")
    }

}

fun generateFillFunctions(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbGeneratedFill.kt",
        suppress = listOf("FunctionName", "RemoveRedundantQualifierName", "SpellCheckingInspection")
    ) {
        append("object KdbGeneratedFill{\n")


        map.entries.forEach { entry ->
            val mapping = entry.value.mapIndexed { index, property ->
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
    fun fill_${entry.key.functionName}(item:${entry.key.qualifiedName!!.asString()}, cursor: tk.mallumo.kdb.sqlite.Cursor, indexArray: IntArray) {
        item.apply {
$mapping
        }
    }
"""
            )
        }

        append("}")
    }

}


fun generateCursorFunctions(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbGeneratedQuery.kt",
        suppress = listOf("FunctionName", "RemoveRedundantQualifierName", "SpellCheckingInspection")
    ) {
        append("object KdbGeneratedQuery{\n")

        map.entries.forEach { entry ->

            append(
                """
    fun query_${entry.key.functionName}(cursor: tk.mallumo.kdb.sqlite.Cursor): ArrayList<${entry.key.qualifiedName!!.asString()}> {
        val out = arrayListOf<${entry.key.qualifiedName!!.asString()}>()
        if (cursor.columns.isNotEmpty()) {
            val indexArray = KdbGeneratedIndex.index_${entry.key.functionName}(cursor)
            while (cursor.next()) {
                out.add(${entry.key.qualifiedName!!.asString()}().apply {
                   KdbGeneratedFill.fill_${entry.key.functionName}(this, cursor, indexArray)
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
}

fun generateInsertFunctions(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbGeneratedInsert.kt",
        imports = listOf(
            "tk.mallumo.kdb.sqlite.SqliteDB"
        ),
        suppress = listOf("FunctionName", "RemoveRedundantQualifierName", "SpellCheckingInspection")
    ) {
        append("object KdbGeneratedInsert{\n")

        map.entries.forEach { entry ->

            val comumns = entry.value.map { "`${it.propertyName.toUpperCase(Locale.ROOT)}`" }
                .joinToString(",", prefix = "(", postfix = ")")

            val values = entry.value.map { "?" }
                .joinToString(",", prefix = "(", postfix = ")")

            val insertVal = entry.value.mapIndexed { index, prop ->
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

            val insert = """"INSERT INTO ${
                entry.key.simpleName.asString().toUpperCase(Locale.ROOT)
            } $comumns VALUES $values""""
            append(
                """
    fun insert_${entry.key.functionName}(items: Array<${entry.key.qualifiedName!!.asString()}>, db: SqliteDB){
        if (items.isEmpty()) return
    
        db.insert(${insert}) {
            items.forEach { item ->
$insertVal
                it.add()
            }
            it.commit()
        }
    }
"""
            )
        }

        append("}")
    }
}

fun generateExtCursorFunctions(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbExtGeneratedQuery.kt",
        suppress = listOf("RemoveRedundantQualifierName", "FunctionName")
    ) {
        map.entries.forEach { entry ->

            append(
                """
suspend fun ImplKdbCommand.Query.${entry.key.niceClassName}(query: String, params: Map<String, Any?> = mapOf()): ArrayList<${entry.key.qualifiedName!!.asString()}> {
    var resp = arrayListOf<${entry.key.qualifiedName!!.asString()}>()
    kdb.connection {
        db.query(ImplKdbUtilsFunctions.mapQueryParams(query, params)) { cursor ->
            resp = KdbGeneratedQuery.query_${entry.key.functionName}(cursor)
        }
    }
    return resp
}
"""
            )
        }
    }
}

fun generateExtDeleteFunctions(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbExtGeneratedDelete.kt",
        suppress = listOf("FunctionName")
    ) {
        map.entries.forEach { entry ->
            append(
                """
suspend fun ImplKdbCommand.Delete.${entry.key.niceClassName}(where: String = "1=1") {
    val command = "DELETE FROM ${
                    entry.key.simpleName.asString().toUpperCase(Locale.ROOT)
                } WHERE ${'$'}where"
    kdb.connection {        
        db.exec(command)
    }
}
"""
            )
        }
    }
}

fun generateExtUpdateFunctions(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbExtGeneratedUpdate.kt",
        suppress = listOf("FunctionName")
    ) {
        map.entries.forEach { entry ->
            val command = "UPDATE ${
                entry.key.simpleName.asString().toUpperCase(Locale.ROOT)
            } SET ${'$'}{ImplKdbUtilsFunctions.mapUpdateParams(params)}  WHERE ${'$'}where"
            append(
                """
suspend fun ImplKdbCommand.Update.${entry.key.niceClassName}(where: String = "1=1", params: Map<String, Any?> = mapOf()) {
    kdb.connection {
        db.exec("$command")
    }
}
"""
            )
        }
    }
}

fun generateExtInsertFunctions(
    codeWriter: CodeWriter,
    map: Map<KSClassDeclaration, Sequence<PropertyTypeHolder>>
) {
    codeWriter.add(
        KdbProcessor.packageOut,
        "KdbExtGeneratedInsert.kt",
        suppress = listOf("RemoveRedundantQualifierName", "FunctionName")
    ) {
        map.entries.forEach { entry ->
            append(
                """
suspend fun ImplKdbCommand.Insert.${entry.key.niceClassName}(item: ${entry.key.qualifiedName!!.asString()}) = ${entry.key.niceClassName}(arrayOf(item))

suspend fun ImplKdbCommand.Insert.${entry.key.niceClassName}(items: List<${entry.key.qualifiedName!!.asString()}>) = ${entry.key.niceClassName}(items.toTypedArray())

suspend fun ImplKdbCommand.Insert.${entry.key.niceClassName}(items: Array<${entry.key.qualifiedName!!.asString()}>) = kdb.connection { KdbGeneratedInsert.insert_${entry.key.functionName}(items, db) }
"""
            )
        }
    }
}

