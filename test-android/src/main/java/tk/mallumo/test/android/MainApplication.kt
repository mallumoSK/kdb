package tk.mallumo.test.android

import android.app.*

class MainApplication : Application() {

    companion object {
        lateinit var instance: MainApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
