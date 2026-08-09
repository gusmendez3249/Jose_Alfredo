package mx.utng.festivaltrack.tv.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FestivalRepository
    private val _eventos = MutableStateFlow<List<EventoEntity>>(emptyList())
    val eventos: StateFlow<List<EventoEntity>> = _eventos.asStateFlow()

    init {
        val database = FestivalDatabase.getInstance(application)
        repository = FestivalRepository(database.eventoDao())

        viewModelScope.launch {
            repository.getEventosLocales().collectLatest { list ->
                _eventos.value = list
            }
        }

        // Sync with backend API
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            try {
                repository.syncEventos()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
