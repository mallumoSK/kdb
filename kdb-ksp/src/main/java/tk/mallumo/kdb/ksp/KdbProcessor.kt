package tk.mallumo.kdb.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import tk.mallumo.kdb.ksp.HashUtils.sha1
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
        val all = arrayListOf<TableNode>().apply {
            addAll(tablesArr)
            addAll(queryInsertArr)
        }

        if (all.isEmpty()) return

        val hash = all.joinToString("\n") { it.toString().sha1() }
        if (hash != codeWriter.readTmpFile("hash.tmp")) {
            generateCreator(codeWriter, mode)
            generateDefStructure(codeWriter, tablesArr)
//
            generateIndexFunctions(codeWriter, all)
            generateFillFunctions(codeWriter, all)
            generateCursorFunctions(codeWriter, all)
            generateInsertFunctions(codeWriter, tablesArr)
//
            generateExtCursorFunctions(codeWriter, all)
            generateExtInsertFunctions(codeWriter, tablesArr)
            generateExtUpdateFunctions(codeWriter, tablesArr)
            generateExtDeleteFunctions(codeWriter, tablesArr)

            write(hash)
        }

    }

    private fun buildDeclarationMap(resolver: Resolver, annotationClass: String) =
        resolver.getSymbolsWithAnnotation(annotationClass) // symbols with annotation
            .filterIsInstance<KSClassDeclaration>() // only usable classes
            .map { TableNode(it) }

    private fun write(hash: String) {
        codeWriter.write(deleteOld = true)
        codeWriter.writeTmpFile("hash.tmp", hash)
    }


    override fun finish() {
    }
}

