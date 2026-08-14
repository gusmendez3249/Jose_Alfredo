package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.GaleriaDto

class GalleryViewModel : ViewModel() {
    private val api = FestivalApiService.create()

    private val _galerias = MutableStateFlow<List<GaleriaDto>>(emptyList())
    val galerias: StateFlow<List<GaleriaDto>> = _galerias

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
                _galerias.value = api.getGalerias()
            } catch (e: Exception) {
                _error.value = "Error al cargar galería: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
