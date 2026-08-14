package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

class AudioViewModel : ViewModel() {
    private val api = FestivalApiService.create()

    private val _canciones = MutableStateFlow<List<CancionDto>>(emptyList())
    val canciones: StateFlow<List<CancionDto>> = _canciones

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _canciones.value = api.getCanciones()
            } catch (e: Exception) {
                _error.value = "Error al cargar las canciones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
