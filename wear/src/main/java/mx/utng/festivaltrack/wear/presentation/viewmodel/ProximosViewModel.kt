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
        
        // 1. Observar la base de datos Room y filtrar inteligentemente
        viewModelScope.launch {
            // Pasamos un string vacío para que traiga todos y nosotros los filtramos con exactitud de zona horaria
            repository.getProximosEventosLocales("").collectLatest { lista ->
                val currentInstant = Instant.now()
                val filtrados = lista.filter { evento ->
                    val eventoInstant = try {
                        Instant.parse(evento.fechaHora)
                    } catch (e: Exception) {
                        try {
                            java.time.LocalDateTime.parse(evento.fechaHora.replace("Z", "")).atZone(java.time.ZoneId.systemDefault()).toInstant()
                        } catch (e2: Exception) {
                            currentInstant
                        }
                    }
                    // Mostrar si es en el futuro o si empezó hace menos de 1 hora
                    java.time.Duration.between(eventoInstant, currentInstant).toHours() < 1
                }.sortedBy { it.fechaHora }
                
                _eventos.value = filtrados.take(15)
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
