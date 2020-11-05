package tk.mallumo.kdb

import android.util.Log

internal actual fun log(data: String, offset: Int) {
    Log.w("KDB", data)
}