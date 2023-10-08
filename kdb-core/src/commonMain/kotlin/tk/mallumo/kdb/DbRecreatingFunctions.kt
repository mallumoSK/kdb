package tk.mallumo.kdb

import kotlinx.coroutines.*
import tk.mallumo.kdb.sqlite.*

internal object DbRecreatingFunctions {

    private const val dbDefColumns =
        "NAME_TABLE, NAME_COLUMN, TYPE, DEFAULT_VALUE, IS_UNIQUE, IS_INDEXED"

    private const val dbDefColumnsCreator =
        "NAME_TABLE LONGVARCHAR, NAME_COLUMN LONGVARCHAR, TYPE LONGVARCHAR, DEFAULT_VALUE LONGVARCHAR, IS_UNIQUE INT, IS_INDEXED INT"

    private const val dbDefTable = "__DB_DEF"

    suspend fun rebuildDatabase(
        kdb: Kdb,
        db: SqliteDB,
        dbDefArray: MutableList<ImplKdbTableDef>,
        debug: Boolean
    ) {
        rebuildTables(kdb, db, readOldDbDef(db, dbDefArray, debug), dbDefArray, debug)

    }

    private suspend fun rebuildTables(
        kdb: Kdb,
        db: SqliteDB,
        oldDefinitionArr: List<ImplKdbTableDef>,
        newDefinitionArr: MutableList<ImplKdbTableDef>,
        debug: Boolean
    ) {
        var writeChanges = false
        newDefinitionArr.forEach { newDef ->
            val oldDef = oldDefinitionArr.firstOrNull { it.name == newDef.name }

            if (oldDef == null) {
                newDef.sqlCreator(redeclareType = false, isSqlite = true).forEach {
                    db.exec(it)
                }
                newDef.applyIndexes().forEach {
                    db.exec(it)
                }
                writeChanges = true
            } else {
                var redeclareTable = newDef.getUnique().toString() != oldDef.getUnique().toString()

                val alterColumn = mutableListOf<ImplKdbTableDef.Item>()
                newDef.columns.forEach { newC ->
                    oldDef.columns.firstOrNull { oldC -> oldC.name == newC.name }.also {
                        if (it == null) {
                            alterColumn.add(newC)
                            writeChanges = true
                        } else if (it.defaultValue != newC.defaultValue || it.type != newC.type) {
                            redeclareTable = true
                        }
                    }
                }

                if (!redeclareTable) {
                    val deletedColumns =
                        oldDef.columns.filter { od -> !newDef.columns.any { nd -> nd.name == od.name } }
                    if (deletedColumns.isNotEmpty()) {
                        redeclareTable = true
                    }
                }

                alterColumn.forEach {
                    try {
                        if (debug) logger("ALTER TABLE ${newDef.name} ADD ${it.sqlCreator(true)}")
                        db.exec("ALTER TABLE ${newDef.name} ADD ${it.sqlCreator(true)}")
                    } catch (e: Exception) {
                        if (debug) logger(
                            e.message
                                ?: "ERR: ALTER TABLE ${newDef.name} ADD ${it.sqlCreator(true)}"
                        )
                    }
                }

                if (redeclareTable) {
                    writeChanges = true
                    newDef.sqlCreator(true, isSqlite = true).forEach {
                        db.exec(it)
                    }


                    db.exec(newDef.redeclare1_Refill())

                    db.exec(newDef.redeclare2_Drop())

                    db.exec(newDef.redeclare3_Rename())
                }

                if (newDef.getIndexes().toString() != oldDef.getIndexes().toString()) {
                    writeChanges = true
                    newDef.applyIndexes(oldDef.getIndexes()).forEach {
                        db.exec(it)
                    }
                }
            }
        }
        if (writeChanges) {
            if (debug) logger("WRITING DATABASE CHANGES")
            kdb.beforeDatabaseChange(db)
            writeNewDbDef(db, newDefinitionArr)
            kdb.afterDatabaseChange(db)
        } else {
            if (debug) logger("NO DATABASE CHANGES")
        }
    }


    private suspend fun writeNewDbDef(db: SqliteDB, dbDefArray: MutableList<ImplKdbTableDef>) = coroutineScope {
        val isMySqlMode = false
        db.exec(
            """
            CREATE TABLE IF NOT EXISTS $dbDefTable (
                        ${if (!isMySqlMode) dbDefColumns else dbDefColumnsCreator}, 
                UNIQUE (NAME_TABLE, NAME_COLUMN)
                        ${if (!isMySqlMode) "ON CONFLICT REPLACE" else ""})
        """.trimIndent()
        )

        db.insert(
            """
            ${if (!isMySqlMode) "INSERT" else "MERGE"}
            INTO $dbDefTable (${dbDefColumns})
            ${if (!isMySqlMode) "" else "KEY (NAME_TABLE, NAME_COLUMN)"}
            VALUES (?,?,?,?,?,?)
        """.trimIndent()
        ) {
            dbDefArray.forEach { def ->
                def.columns.forEach { column ->
                    it.string(0) { def.name }
                    it.string(1) { column.name }
                    it.string(2) { column.type }
                    it.string(3) { column.defaultValue }
                    it.int(4) { if (column.unique) 1 else 0 }
                    it.int(5) { if (column.index) 1 else 0 }
                    it.add()
                }
            }
            it.commit()
        }
        val podm =
            dbDefArray.map { table -> table.columns.map { column -> "'${table.name}-${column.name}'" } }
                .joinToString(",") { it.joinToString(",") }
        db.exec("DELETE FROM $dbDefTable WHERE (NAME_TABLE || '-' || NAME_COLUMN) NOT IN ($podm)")
    }

    private suspend fun readOldDefDirect(db: SqliteDB): List<ImplKdbTableDef>? = coroutineScope {
        val map = HashMap<String, ImplKdbTableDef>()
        try {
            db.query(
                """
           SELECT $dbDefColumns
           FROM $dbDefTable
       """.trimIndent()
            ) { cursor ->
                while (cursor.next()) {
                    cursor.string(0) { tablename ->

                        val item = ImplKdbTableDef.Item()
                        cursor.string(1) {
                            item.name = it
                        }
                        cursor.string(2) {
                            item.type = it
                        }
                        cursor.string(3) {
                            item.defaultValue = it
                        }
                        cursor.int(4) {
                            item.unique = it == 1
                        }
                        cursor.int(5) {
                            item.index = it == 1
                        }

                        val table = map[tablename] ?: ImplKdbTableDef(tablename)
                        table.columns.add(item)
                        map[tablename] = table
                    }
                }
            }
            map.values.toList()
        } catch (e: Exception) {
            null
        }
    }


    private suspend fun readOldDbDef(
        db: SqliteDB,
        newDbDefArray: MutableList<ImplKdbTableDef>,
        debug: Boolean
    ): List<ImplKdbTableDef> {
        return readOldDefDirect(db) ?: readOldDefFromTables(db, newDbDefArray, debug)
    }

    private suspend fun readOldDefFromTables(
        db: SqliteDB,
        newDbDefArray: MutableList<ImplKdbTableDef>,
        debug: Boolean
    ): List<ImplKdbTableDef> = coroutineScope {
        val oldDef = mutableListOf<ImplKdbTableDef>()
        newDbDefArray.forEach { def ->
            val table = ImplKdbTableDef(def.name)
            try {
                db.query("SELECT * FROM ${def.name} LIMIT 1") { cursor ->
                    cursor.columns.forEach {
                        table.columns.add(ImplKdbTableDef.Item(it.uppercase()))
                    }
                }
                if (debug) logger("RECREATING TABLE ${def.name} FOUND")
                oldDef.add(table)
            } catch (e: Exception) {
                if (debug) logger("RECREATING TABLE ${def.name} NOT FOUND")
            }

        }
        oldDef
    }
}
