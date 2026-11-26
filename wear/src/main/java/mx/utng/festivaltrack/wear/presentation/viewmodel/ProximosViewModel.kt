package mx.utng.festivaltrack.wear.presentation.viewmodel

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
import mx.utng.festivaltrack.wear.domain.usecase.ScheduleAlertasUseCase
import mx.utng.festivaltrack.wear.presentation.util.AgendaWear

/**
 * Agenda del reloj con estrategia Offline-First: muestra lo que hay en Room al instante y
 * sincroniza con el backend en segundo plano.
 *
 * @property agenda Conciertos vigentes agrupados por día ("Hoy", "Mañana", "Vie 27 nov").
 * @property sincronizando true mientras se descarga la agenda por primera vez.
 */
class ProximosViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FestivalDatabase.getInstance(application)
    private val repository = FestivalRepository(database.eventoDao())

    private val _agenda = MutableStateFlow<List<Pair<String, List<EventoEntity>>>>(emptyList())
    val agenda: StateFlow<List<Pair<String, List<EventoEntity>>>> = _agenda.asStateFlow()

    private val _sincronizando = MutableStateFlow(true)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getEventosLocales().collectLatest { lista -> _agenda.value = AgendaWear.porDia(lista) }
        }
        sincronizar()
    }

    fun sincronizar() {
        viewModelScope.launch {
            _sincronizando.value = true
            runCatching { repository.syncEventos() }
            _sincronizando.value = false
            // Con la agenda al día se programan los avisos de 15 minutos antes.
            ScheduleAlertasUseCase(getApplication(), database).execute()
        }
    }
}
