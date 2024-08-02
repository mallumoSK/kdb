@Suppress("SpellCheckingInspection", "ClassName")
object Deps {
    const val group = "tk.mallumo"


    object version {
        const val kotlin = "1.9.23"
        const val agp = "8.0.2"
        const val ksp = "1.9.23-1.0.19"
    }

    object core {
        const val version = "${Deps.version.ksp}-1.3.4"
        const val artifact = "kdb-core"
    }

    object ksp {
        const val version = core.version
        const val artifact = "kdb-ksp"
    }

    object lib {
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0"
        const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0"
        const val sqliteJdbc = "org.xerial:sqlite-jdbc:3.39.3.0"
        const val ksp = "com.google.devtools.ksp:symbol-processing-api:${version.ksp}"
    }
}
