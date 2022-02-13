package tk.mallumo.kdb

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("tk.mallumo.kdb", appContext.packageName)
    }


    @Test
    fun t2() {

    }

    @Test
    fun t1() {
        val out = jsonNormalize(
            """
            {
            "item":0,
            "item1":null,
            "items":[null, 1, "123456"]
            }
        """.trimIndent()
        )
        out
        out
    }

    fun jsonNormalize(json: String): String = JSONObject(json).let { obj ->
        makeJObject(obj).toString()
    }

    private fun makeJObject(obj: JSONObject): JSONObject = JSONObject().apply {
        obj.keys().forEach { name ->
            if (!obj.isNull(name)) {
                obj[name].also { item ->
                    when (item) {
                        is JSONObject -> put(name, makeJObject(item))
                        is JSONArray -> put(name, makeJArray(item))
                        else -> putOpt(name, item)
                    }
                }
            }
        }
    }

    private fun makeJArray(obj: JSONArray): JSONArray = JSONArray().apply {
        if (obj.length() > 0) {
            repeat(obj.length()) { index ->
                if (!obj.isNull(index)) {
                    obj[index].also { item ->
                        when (item) {
                            is JSONObject -> put(makeJObject(item))
                            is JSONArray -> put(makeJArray(item))
                            else -> put(item)
                        }
                    }
                }
            }
        }
    }
}