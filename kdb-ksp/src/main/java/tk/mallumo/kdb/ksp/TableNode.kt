package tk.mallumo.kdb.ksp


import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import java.util.*

class TableNode(
    declaration: KSClassDeclaration,
    val simpleNameOrigin: String = declaration.simpleName.asString(),
    val simpleName: String = simpleNameOrigin.toUpperCase(Locale.ENGLISH),
    val qualifiedName: String = declaration.qualifiedName!!.asString(),
    val functionName: String = qualifiedName.replace(".", "_"),
    val property: Sequence<PropertyTypeHolder> = extractProperty(declaration),
    val files: List<KSFile?> = declaration.getAllSuperTypes()
        .map { it.declaration.containingFile }
        .plus(declaration.containingFile)
        .toList()) {

    val niceClassName: String
        get() = simpleNameOrigin.split("_")
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

    companion object {

        private fun extractProperty(declaration: KSClassDeclaration): Sequence<PropertyTypeHolder> {
            return declaration.getAllSuperTypes() // find parents of annotated class
                .map { it.declaration }
                .filterIsInstance<KSClassDeclaration>()
                .plusElement(declaration)
                .map { property ->
                    property.getDeclaredProperties() // find all properties of class
                        .asSequence()
                        .filter { !it.isAbstract() }
                        .filter { it.getter != null }
                        .filter { it.setter != null }
                        .filter { it.extensionReceiver == null }
                        .map { PropertyTypeHolder.get(it) } // get only usable properties
                        .filterNotNull()
                }
                .flatten()
        }
    }

    override fun toString(): String {
        return "$qualifiedName-${property.joinToString { it.toString() }}"
    }
}