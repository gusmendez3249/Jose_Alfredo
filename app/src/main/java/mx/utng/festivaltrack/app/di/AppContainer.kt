package mx.utng.festivaltrack.app.di

import android.content.Context
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

interface AppContainer {
    val festivalRepository: FestivalRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    
    private val database: FestivalDatabase by lazy {
        FestivalDatabase.getInstance(context)
    }

    private val apiService: FestivalApiService by lazy {
        FestivalApiService.create()
    }

    override val festivalRepository: FestivalRepository by lazy {
        FestivalRepository(database.eventoDao(), apiService)
    }
}
