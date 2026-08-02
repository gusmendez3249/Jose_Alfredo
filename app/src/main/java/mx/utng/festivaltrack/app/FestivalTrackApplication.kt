package mx.utng.festivaltrack.app

import android.app.Application
import mx.utng.festivaltrack.app.di.AppContainer
import mx.utng.festivaltrack.app.di.DefaultAppContainer

class FestivalTrackApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
