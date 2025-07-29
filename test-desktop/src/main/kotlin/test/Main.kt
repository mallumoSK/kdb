package test


import kotlinx.coroutines.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import tk.mallumo.kdb.*
import tk.mallumo.kdb.sqlite.*
import java.sql.*

@KdbTable
open class TEST_TABLE(
    @KdbColumnIndex @KdbColumnUnique
    @KdbColumnSize(256)
    var item_string: String = "",
    @KdbColumnSize(10)
    @KdbColumnIndex var item_double: Double = 11.0,
    @KdbColumnSize(10)
    @KdbColumnUnique var item_float: Float = 0F,
    var item_int: Int = 0,
    var item_long: Long = 0,
    var a: String = "",

    var item_time: LocalTime = LocalTime(1,2,3, 4),
    var item_date: LocalDate = LocalDate(5,6,7),
    var item_datetime: LocalDateTime = LocalDateTime(item_date, item_time),
)

@KdbQI
open class BindingTEST_TABLE(var x: Double = 1.3) : TEST_TABLE()

@KdbQI
open class BindingTEST(var xyz: String = "")

val kdb: Kdb by lazy {
    val sqlite = DbEngine.createSQLite(
        isDebug = true,
        path = "/tmp/___/test.sqlite"
    )
    Kdb.get(
        engine = sqlite,
        reconfigureDatabaseOnStart = true,
        beforeInit = { println("-BEFORE INIT") },
        afterInit = { println("-AFTER INIT") },
        beforeDatabaseChange = { println("--BEFORE RECONFIGURE") },
        afterDatabaseChange = { println("--AFTER RECONFIGURE") })

}

fun main(args: Array<String>) {
    runBlocking {
        kdb.prepareDatabase()


        var ids: List<Long> = emptyList()
        ids = kdb.insert.test_table(TEST_TABLE(item_string = "a", item_float = 1F))
        println("tt 1 $ids")

        ids = kdb.insert.test_table(
            arrayOf(
                BindingTEST_TABLE().apply { item_string = "b"; item_float = 2F },
                TEST_TABLE(item_string = "c", item_float = 3F)
            )
        )
        println("tt 2+3 $ids")

        ids = kdb.insert.test_table(
            listOf(
                TEST_TABLE(item_string = "d", item_float = 4F),
                TEST_TABLE(item_string = "e", item_float = 5F)
            )
        )
        println("tt 4+5 $ids")

        //query
        val items = kdb.query.test_table("SELECT * FROM test_table")
        val items2 = kdb.query.binding_test_table("SELECT t.*, 123.0 as x  FROM test_table t ")
        val items3 = kdb.query.binding_test("SELECT date('now') as xyz ")

        //delete
        kdb.delete.test_table(where = "item_string = 'c'")

        //update
        kdb.update.test_table(
            where = "item_string = 'a'",
            mapOf(
                "a" to "'aa'",
                "item_float" to 558F,
                "item_int" to 16
            )
        )
    }
}
