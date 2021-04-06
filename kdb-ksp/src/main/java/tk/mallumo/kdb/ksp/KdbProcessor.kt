package tk.mallumo.kdb.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


class KdbProcessor : SymbolProcessor {

    private lateinit var codeGenerator: CodeGenerator

    private lateinit var options: Map<String, String>

    lateinit var mode: String

    companion object {
        const val packageOut = "tk.mallumo.kdb"
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
        this.codeGenerator = codeGenerator
    }

    private fun getDclarations(resolver: Resolver, annotationClass: String) =
        resolver.getSymbolsWithAnnotation(annotationClass) // symbols with annotation

    val tableNodes = hashMapOf<String, TableNode>()
    val queryNodes = hashMapOf<String, TableNode>()
    val allNodes = hashMapOf<String, TableNode>()

    val cache = File("/tmp/___/cache-x").apply {
        if(!exists()) createNewFile()
    }
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val tableDeclarations = getDclarations(resolver, "tk.mallumo.kdb.KdbTable")
        val queryDeclarations = getDclarations(resolver, "tk.mallumo.kdb.KdbQI")

        cache.appendText("\n NEW->\n${resolver.getAllFiles().joinToString("\n") { it.filePath }}")
        //tableValid
        tableDeclarations.filterIsInstance<KSClassDeclaration>()
            .map { it.qualifiedName!!.asString() to TableNode(it) }
            .also {
                tableNodes.putAll(it)
                allNodes.putAll(it)
            }
        //queryValid
        queryDeclarations.filterIsInstance<KSClassDeclaration>()
            .map { it.qualifiedName!!.asString() to TableNode(it) }
            .also {
                queryNodes.putAll(it)
                allNodes.putAll(it)
            }
        return tableDeclarations.filterNot { it is KSClassDeclaration }
            .plus(queryDeclarations.filterNot { it is KSClassDeclaration })
    }


    override fun finish() {
        if (allNodes.isNotEmpty()) {

            val all = allNodes.values.toList()
            val tables = tableNodes.values.toList()

            val filesAll = all.map { it.files }
                .flatten()
                .filterNotNull()
                .distinctBy { it.filePath }
                .toTypedArray()

            val filesTables = tables.map { it.files }
                .flatten()
                .filterNotNull()
                .distinctBy { it.filePath }
                .toTypedArray()

            val dependenciesAll = Dependencies(true, *filesAll)
            val dependenciesTables = Dependencies(true, *filesTables)

            output(
                name = "KdbGenerated",
                dependencies = dependenciesAll,
                imports = ""
            ) {
                generateCreator(mode)
            }

            output(
                name = "KdbGeneratedDefStructure",
                dependencies = dependenciesTables,
                imports = ""
            ) {
                generateDefStructure(tables)
            }

//
            output(
                name = "KdbGeneratedIndex",
                dependencies = dependenciesAll,
                imports = """
import tk.mallumo.kdb.sqlite.Cursor
import java.util.Locale"""
            ) {
                generateIndexFunctions(all)
            }

            output(
                name = "KdbGeneratedFill",
                dependencies = dependenciesAll,
                imports = ""
            ) {
                generateFillFunctions(all)
            }
            output(
                name = "KdbGeneratedQuery",
                dependencies = dependenciesAll,
                imports = ""
            ) {
                generateCursorFunctions(all)
            }
            output(
                name = "KdbGeneratedInsert",
                dependencies = dependenciesTables,
                imports = """
import tk.mallumo.kdb.sqlite.SqliteDB"""
            ) {
                generateInsertFunctions(tables)
            }

            output(
                name = "KdbExtGeneratedQuery",
                dependencies = dependenciesAll,
                imports = ""
            ) {
                generateExtCursorFunctions(all)
            }

            output(
                name = "KdbExtGeneratedInsert",
                dependencies = dependenciesTables,
                imports = ""
            ) {
                generateExtInsertFunctions(tables)
            }
            output(
                name = "KdbExtGeneratedUpdate",
                dependencies = dependenciesTables,
                imports = ""
            ) {
                generateExtUpdateFunctions(tables)
            }
            output(
                name = "KdbExtGeneratedDelete",
                dependencies = dependenciesTables,
                imports = ""
            ) {
                generateExtDeleteFunctions(tables)
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun output(
        name: String,
        dependencies: Dependencies,
        imports: String = "",
        content: () -> String
    ) {
        contract {
            callsInPlace(content, InvocationKind.EXACTLY_ONCE)
        }
        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = packageOut,
            fileName = name,
            extensionName = "kt"
        ).bufferedWriter().use {
            it.write(
                """
@file:Suppress("unused")
package $packageOut

$imports

${content()}"""
            )
            it.flush()
        }
    }
}

