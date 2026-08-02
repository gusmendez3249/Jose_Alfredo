package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

class EventosViewModel(private val repository: FestivalRepository) : ViewModel() {

    // Flujo reactivo con todos los eventos para la vista normal
    val eventosLocales: StateFlow<List<EventoEntity>> = repository.getEventosLocales()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Al iniciar, intentamos sincronizar con la API para traer datos frescos
        viewModelScope.launch {
            repository.syncEventos()
        }
    }

    companion object {
        fun provideFactory(repository: FestivalRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EventosViewModel::class.java)) {
                    return EventosViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
