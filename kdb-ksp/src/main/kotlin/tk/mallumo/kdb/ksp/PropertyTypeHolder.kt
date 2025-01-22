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
    val columnSize:Int
) {


    companion object {
        /**
         * object types which Bundle supports directly
         */
        val directTypes = arrayOf(
            "kotlin.Double",
            "kotlin.Float",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.String"
        )

        /**
         * PropertyTypeHolder instance creator
         * @return if is property supported, then returns instance, otherwise returns null
         */
        fun get(prop: KSPropertyDeclaration): PropertyTypeHolder? {

            val name = prop.simpleName.asString()
            val declaration = prop.type.resolve().declaration
            val typeName = declaration.qualifiedName?.asString() ?: return null
            val cursorTypeName = typeName
                .split(".")[1]
                .lowercase(Locale.ENGLISH)
                .let {
                    if (it != "float") it
                    else "double"
                }
            val sqlColumnTypeName = when (typeName) {
                "kotlin.Double" -> "NUMERIC"
                "kotlin.Float" -> "NUMERIC"
                "kotlin.Int" -> "INTEGER"
                "kotlin.Long" -> "BIGINT"
                "kotlin.String" -> "TEXT"
                else -> "UNDEFINED"
            }
            val defaultValue =
                if (sqlColumnTypeName == "TEXT") "\"'${'$'}{instance.$name}'\""
                else "instance.${name}.toString()"

            val isUnique = prop.annotations.any { it.shortName.asString() == "KdbColumnUnique" }
            val isIndex = prop.annotations.any { it.shortName.asString() == "KdbColumnIndex" }
            val columnSize = prop.annotations
                .firstOrNull { it.shortName.asString() == "KdbColumnSize" }
                ?.arguments
                ?.first { it.name?.asString() == "size" }
                ?.value
                ?.toString()
                ?.toIntOrNull()
                ?:0
            return when (typeName) {
                in directTypes -> {
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
