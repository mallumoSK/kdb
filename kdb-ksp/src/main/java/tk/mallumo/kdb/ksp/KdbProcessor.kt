package tk.mallumo.kdb.ksp

import org.jetbrains.kotlin.ksp.getAllSuperTypes
import org.jetbrains.kotlin.ksp.getDeclaredProperties
import org.jetbrains.kotlin.ksp.isAbstract
import org.jetbrains.kotlin.ksp.processing.CodeGenerator
import org.jetbrains.kotlin.ksp.processing.KSPLogger
import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.processing.SymbolProcessor
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import java.io.File


class KdbProcessor : SymbolProcessor {

    private lateinit var codeWriter: CodeWriter

    private lateinit var options: Map<String, String>

    lateinit var mode: String

    companion object {
        const val packageOut = "tk.mallumo.kdb"
        private const val errProjectOutDir =
            "Inside yours gradle.build must be defined constant (output): 'ksp.arg(\"KdbSrcOut\", \"\${projectDir.absolutePath}/src/main/ksp\")'"
        private const val errProjectMode =
            "Inside yours gradle.build must be defined mode (ANDROID, JVM-DESKTOP): 'ksp.arg(\"KdbMode\", \"ANDROID\")'"
    }

    override fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        this.options = options
        this.mode = options["KdbMode"] ?: throw RuntimeException(errProjectMode)
        this.codeWriter = CodeWriter(
            directory = File(
                options["KdbSrcOut"] ?: throw RuntimeException(
                    errProjectOutDir
                )
            ),
            rootPackage = packageOut
        )
    }

    override fun process(resolver: Resolver) {
        val tablesArr = buildDeclarationMap(resolver, "tk.mallumo.kdb.KdbTable")
        val queryInsertArr = buildDeclarationMap(resolver, "tk.mallumo.kdb.KdbQI")
        val all = hashMapOf<KSClassDeclaration, Sequence<PropertyTypeHolder>>().apply {
            putAll(tablesArr)
            putAll(queryInsertArr)
        }

        if (all.isEmpty()) return

        generateCreator(codeWriter, mode)
        generateDefStructure(codeWriter, tablesArr)

        generateIndexFunctions(codeWriter, all)
        generateFillFunctions(codeWriter, all)
        generateCursorFunctions(codeWriter, all)
        generateInsertFunctions(codeWriter, tablesArr)

        generateExtCursorFunctions(codeWriter, all)
        generateExtInsertFunctions(codeWriter, tablesArr)
        generateExtUpdateFunctions(codeWriter, tablesArr)
        generateExtDeleteFunctions(codeWriter, tablesArr)
    }


    private fun buildDeclarationMap(resolver: Resolver, annotationClass: String) =
        resolver.getSymbolsWithAnnotation(annotationClass) // symbols with annotation
            .filterIsInstance<KSClassDeclaration>() // only usable classes
            .associateBy({ it }, { current -> // map all properties to annotated class
                current.getAllSuperTypes() // find parents of annotated class
                    .map { it.declaration }
                    .filterIsInstance<KSClassDeclaration>()
                    .plusElement(current)
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
            })


    override fun finish() {
        codeWriter.write(false)
    }
}