package tk.mallumo.kdb

internal expect fun log(data: String, offset: Int = 12)

internal inline fun tryPrint(function: () -> Unit) {
    try {
        function.invoke()
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

internal inline fun tryIgnore(function: () -> Unit) {
    try {
        function.invoke()
    } catch (e: Throwable) {
    }
}