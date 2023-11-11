package test


import kotlinx.coroutines.*
import tk.mallumo.kdb.*
import tk.mallumo.kdb.sqlite.*
import java.sql.*

@KdbTable
open class TEST_TABLE(
    @KdbColumnIndex @KdbColumnUnique var item_string: String = "",
    @KdbColumnIndex var item_double: Double = 11.0,
    @KdbColumnUnique var item_float: Float = 0F,
    var item_int: Int = 0,
    var item_long: Long = 0,
    var a: String = ""
)

@KdbQI
open class BindingTEST_TABLE(var x: Double = 1.3) : TEST_TABLE()

@KdbQI
open class BindingTEST(var xyz: String = "")

val kdb: Kdb by lazy {
    val sqlite = DbEngine.createSQLite(isDebug = true, "/tmp/test.sqlite")
    Kdb.get(sqlite,
        { println("-BEFORE INIT") },
        { println("-AFTER INIT") },
        { println("--BEFORE RECONFIGURE") },
        { println("--AFTER RECONFIGURE") })

}

fun main(args: Array<String>) {
    runBlocking {
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
