package tk.mallumo.kdb.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.*
import kotlin.contracts.*

class KdbProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return KdbProcessor(environment)
    }
}

class KdbProcessor(
    private var environment: SymbolProcessorEnvironment,
    private var invoked: Boolean = false
) : SymbolProcessor {

    companion object {
        const val basePackage = "tk.mallumo.kdb"
    }

    private val commonSourcesOnly get() = environment.options["commonSourcesOnly"] == "true"

    private fun getDclarations(resolver: Resolver, annotationClass: String) =
        resolver.getSymbolsWithAnnotation(annotationClass) // symbols with annotation

    private val tableNodes = hashMapOf<String, TableNode>()
    private val queryNodes = hashMapOf<String, TableNode>()
    private val allNodes = hashMapOf<String, TableNode>()


    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()

        val tableDeclarations = getDclarations(resolver, "tk.mallumo.kdb.KdbTable")
        val queryDeclarations = getDclarations(resolver, "tk.mallumo.kdb.KdbQI")

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
        invoked = true
        return tableDeclarations.filterNot { it is KSClassDeclaration }
            .plus(queryDeclarations.filterNot { it is KSClassDeclaration })
            .toList()
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
                generateCreator()
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

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @OptIn(ExperimentalContracts::class)
    private fun output(
        name: String,
        dependencies: Dependencies,
        imports: String = "",
        ext: String = "kt",
        content: () -> String
    ) {
        contract {
            callsInPlace(content, InvocationKind.EXACTLY_ONCE)
        }
        @Suppress("SameParameterValue") val data = """
@file:Suppress("unused", "FunctionName")
package $basePackage

$imports

${content()}"""
        environment.codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = basePackage,
            fileName = name,
            extensionName = ext
        ).bufferedWriter().use {
            it.write(data.commented(commonSourcesOnly))
            it.flush()
        }
        if (commonSourcesOnly) {
            generateCommonFile(basePackage, name, ext)
                ?.writeText(data)
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun generateCommonFile(
        @Suppress("SameParameterValue") pckg: String,
        name: String,
        ext: String
    ): File? {
        val key = "${pckg.replace('.', '/')}/$name.$ext"
        return environment.codeGenerator.generatedFile
            .firstOrNull { it.absolutePath.endsWith(key) }
            ?.let { file ->
                val rootDirPrefix = "/build/generated/ksp/"
                val sourceDirPrefix = "/kotlin/"
                file.absolutePath.indexOf(rootDirPrefix)
                    .takeIf { it > 0 }
                    ?.let { startRootIndex ->
                        val rootDir = File(file.absolutePath.substring(0, startRootIndex), rootDirPrefix)
                        val suffix = file.absolutePath.substring(rootDir.absolutePath.lastIndex)
                        suffix.indexOf(sourceDirPrefix)
                            .takeIf { it > 0 }
                            ?.let { sourceDirPrefixIndex ->
                                File(rootDir, "common/commonMain${suffix.substring(sourceDirPrefixIndex)}").apply {
                                    if (!parentFile.exists()) parentFile.mkdirs()
                                }

                            }
                    }
            }
    }
}

private fun String.commented(allLines: Boolean): String =
    if (!allLines) this
    else lines().joinToString(prefix = "//", separator = "\n//")

