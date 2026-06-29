package mx.utng.festivaltrack.wear.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.wear.data.local.FestivalDatabase
import mx.utng.festivaltrack.wear.data.local.entity.EventoEntity
import mx.utng.festivaltrack.wear.data.repository.FestivalRepository
import java.time.Instant

class ProximosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FestivalRepository

    private val _eventos = MutableStateFlow<List<EventoEntity>>(emptyList())
    val eventos: StateFlow<List<EventoEntity>> = _eventos.asStateFlow()

    init {
        val database = FestivalDatabase.getInstance(application)
        repository = FestivalRepository(database.eventoDao())
        
        // 1. Observar la base de datos Room (flujo en tiempo real)
        viewModelScope.launch {
            val ahora = Instant.now().toString()
            repository.getProximosEventosLocales(ahora).collectLatest { lista ->
                _eventos.value = lista
            }
        }

        // 2. Intentar sincronizar con el backend
        syncData()
    }

    private fun syncData() {
        viewModelScope.launch {
            repository.syncEventos()
        }
    }
}
