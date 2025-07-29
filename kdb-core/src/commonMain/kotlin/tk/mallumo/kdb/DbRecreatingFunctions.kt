package tk.mallumo.kdb

import kotlinx.coroutines.*
import tk.mallumo.kdb.sqlite.*

internal object DbRecreatingFunctions {

    private const val dbDefColumns =
        "NAME_TABLE, NAME_COLUMN, TYPE, DEFAULT_VALUE, IS_UNIQUE, IS_INDEXED, SIZE"

    private const val dbDefColumnsCreator =
        "NAME_TABLE VARCHAR(128), NAME_COLUMN VARCHAR(128), TYPE VARCHAR(128), DEFAULT_VALUE VARCHAR(128), IS_UNIQUE INT, IS_INDEXED INT, SIZE INT DEFAULT '0'"

    private const val dbDefColumnsReplacement =
        """
            `NAME_TABLE`=VALUES(`NAME_TABLE`),
            `NAME_COLUMN`=VALUES(`NAME_COLUMN`),
            `TYPE`=VALUES(`TYPE`),
            `DEFAULT_VALUE`=VALUES(`DEFAULT_VALUE`),
            `IS_UNIQUE`=VALUES(`IS_UNIQUE`),
            `IS_INDEXED`=VALUES(`IS_INDEXED`),
            `SIZE`=VALUES(`SIZE`)
        """

    private const val dbDefTable = "__DB_DEF"

    suspend fun rebuildDatabase(
        kdb: Kdb,
        db: DbEngine,
        dbDefArray: MutableList<ImplKdbTableDef>,
        debug: Boolean
    ) {
        rebuildTables(kdb, db, readOldDbDef(db, dbDefArray, debug), dbDefArray, debug)

    }

    private suspend fun rebuildTables(
        kdb: Kdb,
        db: DbEngine,
        oldDefinitionArr: List<ImplKdbTableDef>,
        newDefinitionArr: MutableList<ImplKdbTableDef>,
        debug: Boolean
    ) {
        var writeChanges = false
        val indexes = readIndexes(db)

        newDefinitionArr.forEach { newDef ->
            val oldDef = oldDefinitionArr.firstOrNull { it.name == newDef.name }
            if (oldDef == null) {
                newDef.sqlCreator(redeclareType = false, isSqlite = db.isSqlite).forEach {
                    db.exec(it)
                }
                newDef.applyIndexes(db.isSqlite, indexes).forEach {
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
                        } else if (it.defaultValue != newC.defaultValue
                            || it.type != newC.type
                            || it.size != newC.size
                        ) {
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
                        if (debug) logger("ALTER TABLE ${newDef.name} ADD ${it.sqlCreator(db.isSqlite)}")
                        db.exec("ALTER TABLE ${newDef.name} ADD ${it.sqlCreator(db.isSqlite)}")
                    } catch (e: Exception) {
                        if (debug) logger(
                            e.message
                                ?: "ERR: ALTER TABLE ${newDef.name} ADD ${it.sqlCreator(db.isSqlite)}"
                        )
                    }
                }

                if (redeclareTable) {
                    writeChanges = true
                    newDef.sqlCreator(true, isSqlite = db.isSqlite).forEach {
                        db.exec(it)
                    }


                    db.exec(newDef.redeclare1_Refill())

                    db.exec(newDef.redeclare2_Drop())

                    db.exec(newDef.redeclare3_Rename())
                }

                val newIndexes = newDef.getIndexes()
                    .sortedBy { "${it.name},${it.type}" }
                    .joinToString { it.toString() }

                val oldIndexes = oldDef.getIndexes()
                    .sortedBy { "${it.name},${it.type}" }
                    .joinToString { it.toString() }

                if (newIndexes != oldIndexes) {
                    writeChanges = true
                    newDef.applyIndexes(db.isSqlite, indexes).forEach {
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


    private suspend fun writeNewDbDef(db: DbEngine, dbDefArray: MutableList<ImplKdbTableDef>) = coroutineScope {

        db.exec(
            """
            CREATE TABLE IF NOT EXISTS $dbDefTable (
                        ${if (db.isSqlite) dbDefColumns else dbDefColumnsCreator}, 
                UNIQUE (NAME_TABLE, NAME_COLUMN)
                        ${if (db.isSqlite) "ON CONFLICT REPLACE" else ""})
        """.trimIndent()
        )

        db.insert(
            """
            INSERT INTO $dbDefTable (${dbDefColumns})
            VALUES (?,?,?,?,?,?,?)
            ${if (db.isSqlite) "" else " ON DUPLICATE KEY UPDATE $dbDefColumnsReplacement"}
        """.trimIndent()
        ) {
            dbDefArray.forEach { def ->
                def.columns.forEach { column ->
                    it.string(0) { def.name }
                    it.string(1) { column.name }
                    it.string(2) { column.type.toString() }
                    it.string(3) { column.defaultValue }
                    it.int(4) { if (column.unique) 1 else 0 }
                    it.int(5) { if (column.index) 1 else 0 }
                    it.int(6) { column.size }
                    it.add()
                }
            }
            it.commit()
        }
        val podm =
            dbDefArray.map { table -> table.columns.map { column -> "'${table.name}-${column.name}'" } }
                .joinToString(",") { it.joinToString(",") }

        val concatWhere = if (db.isSqlite) " (NAME_TABLE || '-' || NAME_COLUMN) "
        else " CONCAT(NAME_TABLE, '-', NAME_COLUMN) "

        db.exec("DELETE FROM $dbDefTable WHERE $concatWhere NOT IN ($podm)")
    }

    private suspend fun readOldDefDirect(db: DbEngine): List<ImplKdbTableDef>? = coroutineScope {
        val map = HashMap<String, ImplKdbTableDef>()

       suspend  fun readTableData() {
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
                            item.type = ImplKdbTableDef.ColumnType[it]
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
                        cursor.int(6) {
                            item.size = it
                        }

                        val table = map[tablename] ?: ImplKdbTableDef(tablename)
                        table.columns.add(item)
                        map[tablename] = table
                    }
                }
            }
        }

        runCatching {
            readTableData()
        }.onFailure {
            runCatching {
                db.exec("ALTER TABLE $dbDefTable ADD COLUMN SIZE INT DEFAULT '0';")
                readTableData()
            }
        }
        map.values.toList().takeIf { it.isNotEmpty() }
    }

    private suspend fun readIndexes(db: DbEngine): MutableList<String> = coroutineScope {
        buildList {
            val cmd = if (db.isSqlite) {
                """
                SELECT name 
                FROM sqlite_master 
                WHERE type = 'index' AND name like 'INDEX_%'
                """
            } else {
                """
                select INDEX_NAME  as name
                from information_schema.statistics
                where TABLE_SCHEMA = database() AND INDEX_NAME like 'INDEX_%'
                """.trimIndent()
            }
            runCatching {
                db.query(cmd) { cursor ->
                    while (cursor.next()) {
                        cursor.string(0) { indexName ->
                            add(indexName)
                        }
                    }
                }

            }.onFailure { it.printStackTrace() }
        }.toMutableList()
    }


    private suspend fun readOldDbDef(
        db: DbEngine,
        newDbDefArray: MutableList<ImplKdbTableDef>,
        debug: Boolean
    ): List<ImplKdbTableDef> {
        return readOldDefDirect(db) ?: readOldDefFromTables(db, newDbDefArray, debug)
    }

    private suspend fun readOldDefFromTables(
        db: DbEngine,
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
