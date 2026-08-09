package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.EventoCreateDto
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

class AdminManageViewModel(private val repository: FestivalRepository) : ViewModel() {

    val eventos: StateFlow<List<EventoEntity>> = repository.getEventosLocales()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.syncEventos()
        }
    }

    fun saveEvent(id: String?, title: String, date: String, location: String, price: String) {
        viewModelScope.launch {
            val dto = EventoCreateDto(
                nombre = title,
                fechaHora = date, // Note: real app would format this correctly
                ubicacion = location,
                escenario = "Principal", // Placeholder
                estado = "ACTIVO"
            )
            
            try {
                if (id.isNullOrEmpty()) {
                    repository.addEvento(dto)
                } else {
                    repository.updateEvento(id, dto)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteEvento(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun provideFactory(repository: FestivalRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AdminManageViewModel::class.java)) {
                    return AdminManageViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
