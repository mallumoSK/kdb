package tk.mallumo.test.android

import android.app.Application
import tk.mallumo.log.LOGGER_IS_ENABLED

class MainApplication : Application() {

    companion object {
        lateinit var instance: MainApplication
    }

    override fun onCreate() {
        super.onCreate()
        LOGGER_IS_ENABLED = true
        instance = this
    }
}