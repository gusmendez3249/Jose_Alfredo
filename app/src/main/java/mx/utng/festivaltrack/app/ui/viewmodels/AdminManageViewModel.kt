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

    fun saveEvent(token: String? = null, id: String?, title: String, date: String, location: String, price: String) {
        viewModelScope.launch {
            // Attempt to create an ISO string if the user typed something simple
            var isoDate = date
            try {
                if (!date.contains("T")) {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    val parsed = sdf.parse(date)
                    if (parsed != null) {
                        val sdfOut = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                        isoDate = sdfOut.format(parsed)
                    }
                }
            } catch (e: Exception) {
                // Fallback to current ISO timestamp
                isoDate = "2026-11-23T20:00:00Z"
            }

            val finalId = id.takeIf { !it.isNullOrEmpty() } ?: java.util.UUID.randomUUID().toString()

            val dto = EventoCreateDto(
                nombre = title,
                fechaHora = isoDate, 
                ubicacion = location,
                escenario = "Principal",
                capacidad = 1000,
                estado = "PUBLICADO"
            )
            
            try {
                if (id.isNullOrEmpty()) {
                    repository.addEvento(token, finalId, dto)
                } else {
                    repository.updateEvento(token, id, dto)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteEvent(token: String? = null, id: String) {
        viewModelScope.launch {
            try {
                repository.deleteEvento(token, id)
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
