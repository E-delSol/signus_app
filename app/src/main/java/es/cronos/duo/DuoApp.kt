package es.cronos.duo

import android.app.Application
import es.cronos.duo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class DuoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@DuoApp)
            modules(appModule)
        }
    }
}