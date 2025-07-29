package tk.mallumo.kdb.ksp


import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.util.*

/**
 * This class holding info about property for speedup processing
 *
 * @param propertyName simple name of property
 * @param qualifiedName qualifiedName of property type or supertype which bundle supports
 */
data class PropertyTypeHolder(
    val propertyName: String,
    val qualifiedName: String,
    val cursorTypeName: String,
    val sqlColumnTypeName: String,
    val defaultValue: String,
    val isUnique: Boolean,
    val isIndex: Boolean,
    val columnSize: Int
) {


    companion object {
        /**
         * object types which Bundle supports directly
         */
         val typeToSqlTypeMap = mapOf(
            "kotlin.Double" to "NUMERIC",
            "kotlin.Float" to "NUMERIC",
            "kotlin.Int" to "INTEGER",
            "kotlin.Long" to "BIGINT",
            "kotlin.String" to "TEXT",

            "kotlinx.datetime.LocalTime" to "TIME",
            "kotlin.datetime.LocalTime" to "TIME",

            "kotlinx.datetime.LocalDate" to "DATE",
            "kotlin.datetime.LocalDate" to "DATE",

            "kotlinx.datetime.LocalDateTime" to "DATETIME",
            "kotlinx.datetime.LocalDateTime" to "DATETIME"
        )

        /**
         * PropertyTypeHolder instance creator
         * @return if is property supported, then returns instance, otherwise returns null
         */
        fun get(prop: KSPropertyDeclaration): PropertyTypeHolder? {

            val name = prop.simpleName.asString()
            val declaration = prop.type.resolve().declaration
            val typeName = declaration.qualifiedName?.asString() ?: return null

            val sqlColumnTypeName = typeToSqlTypeMap[typeName] ?: "UNDEFINED"

            val simpleTypeName = declaration.simpleName.asString()

            val cursorTypeName = when (simpleTypeName) {
                "Float" -> "double" // Cursors use 'double' for floats.
                "LocalTime" -> "time"
                "LocalDate" -> "date"
                "LocalDateTime" -> "dateTime"
                else -> simpleTypeName.lowercase()
            }

            val defaultValue = when (sqlColumnTypeName) {
                "TEXT",
                "TIME",
                "DATE",
                "DATETIME" ->"\"'${'$'}{instance.$name}'\""
                else -> "instance.${name}.toString()"
            }

            val isUnique = prop.annotations.any { it.shortName.asString() == "KdbColumnUnique" }
            val isIndex = prop.annotations.any { it.shortName.asString() == "KdbColumnIndex" }
            val columnSize = prop.annotations
                .firstOrNull { it.shortName.asString() == "KdbColumnSize" }
                ?.arguments
                ?.first { it.name?.asString() == "size" }
                ?.value
                ?.toString()
                ?.toIntOrNull()
                ?: 0

            return when (typeName) {
                in typeToSqlTypeMap.keys -> {
                    PropertyTypeHolder(
                        name,
                        typeName,
                        cursorTypeName,
                        sqlColumnTypeName,
                        defaultValue,
                        isUnique,
                        isIndex,
                        columnSize
                    )
                }

                else -> null
            }
        }
    }
}
