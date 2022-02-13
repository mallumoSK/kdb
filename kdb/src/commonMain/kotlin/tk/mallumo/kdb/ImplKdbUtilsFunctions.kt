package tk.mallumo.kdb

@Suppress("unused")
object ImplKdbUtilsFunctions {
    fun mapUpdateParams(params: Map<String, Any?>): String {
        var values = ""
        params.entries.forEachIndexed { index, it ->
            if (index != 0) {
                values += ",\n\t"
            }
            values += "  " + it.key + " = "
            values += when (it.value) {
                null -> "NULL"
                else -> it.value
            }
        }
        return values.ifEmpty { "1=1" }
    }

    fun mapQueryParams(origin: String, params: Map<String, Any?>): String {
        var query = origin
        params.entries.forEach {
            if (it.key.isNotEmpty()) {
                query = when (it.value) {
                    null -> query.replace(("@" + it.key), "NULL")
                    is String -> query.replace(
                        ("@" + it.key),
                        ("'" + it.value + "'")
                    )
                    else -> query.replace(("@" + it.key), ("'" + it.value + "'"))
                }
            }
        }
        return query
    }
}