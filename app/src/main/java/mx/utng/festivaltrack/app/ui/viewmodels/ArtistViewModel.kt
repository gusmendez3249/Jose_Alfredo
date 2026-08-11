package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.BiografiaDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

class ArtistViewModel : ViewModel() {
    private val api = FestivalApiService.create()

    private val _biografia = MutableStateFlow<BiografiaDto?>(null)
    val biografia: StateFlow<BiografiaDto?> = _biografia

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchBiografia()
    }

    private fun fetchBiografia() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val list = api.getBiografias()
                if (list.isNotEmpty()) {
                    _biografia.value = list.first()
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar la biografía: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
