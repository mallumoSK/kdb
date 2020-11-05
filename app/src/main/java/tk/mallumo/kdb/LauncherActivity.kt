package tk.mallumo.kdb

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

@KdbTable
open class TEST_TABLE(
    @KdbColumnIndex @KdbColumnUnique var item_string: String = "",
    @KdbColumnIndex var item_double: Int = 11,
    @KdbColumnUnique var item_float: Float = 0F,
    var item_int: Int = 0,
    var item_long: Long = 0,
)

@KdbQI
open class BindingTEST_TABLE(var x: Double = 1.3) : TEST_TABLE()

val kdb by lazy { createKDB(MainApplication.instance) }

class LauncherActivity : AppCompatActivity() {


    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
//            kdb.insert.test_table(TEST_TABLE())
//            tk.mallumo.log.log(kdb.query.test_table("SELECT * FROM test_table"), true)
//            kdb.delete.test_table("1=1")
            tk.mallumo.log.log(kdb.query.test_table("SELECT * FROM test_table"), true)
        }
        //
    }
}