package tk.mallumo.kdb

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.zip.GZIPOutputStream

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun t2() {
        val pdf = File("/tmp/___/cc.pdf")
        val out = String(Base64.getEncoder().encode(pdf.readBytes()))
        val gOrigin = ByteArrayOutputStream()
        val gOriginL = GZIPOutputStream(gOrigin).use {
            it.write(pdf.readBytes())
            it.flush()
            gOrigin.toByteArray().size
        }

        val gOriginBase64 = ByteArrayOutputStream()
        val gOriginBase64L = GZIPOutputStream(gOriginBase64).use {
            it.write(out.toByteArray())
            it.flush()
            gOriginBase64.toByteArray().size
        }
        fun Number.nicePrint():String{
            return (this.toLong() /1000).toString()
        }
        println("""
            ORIGIN = ${pdf.readBytes().size.nicePrint()}
            ORIGIN GZIP = ${gOriginL.nicePrint()}
            BASE64 = ${out.toByteArray().size.nicePrint()}
            BASE64 GZIP = ${gOriginBase64L.nicePrint()}
        """.trimIndent())
    }
}