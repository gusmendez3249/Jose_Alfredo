package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.factory.BiographyEntryType
import mx.utng.festivaltrack.app.data.factory.BiographyFactoryProducer
import mx.utng.festivaltrack.app.data.factory.InteractiveBiographyItem
import mx.utng.festivaltrack.app.data.firebase.BiographyFirestoreService
import mx.utng.festivaltrack.shared.data.remote.BiografiaDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

class ArtistViewModel(application: Application) : AndroidViewModel(application) {
    private val api = FestivalApiService.create()
    private val firestoreService = BiographyFirestoreService.getInstance(application)

    private val _biografia = MutableStateFlow<BiografiaDto?>(null)
    val biografia: StateFlow<BiografiaDto?> = _biografia

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Lista reactiva de elementos de la Galería Interactiva (Firestore / Storage)
    val interactiveItems: StateFlow<List<InteractiveBiographyItem>> = firestoreService.itemsFlow

    // Filtro seleccionado en la UI
    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init {
        fetchBiografia()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleLike(itemId: String) {
        firestoreService.toggleLike(itemId)
    }

    fun refresh() {
        fetchBiografia()
    }

    /**
     * Utiliza el FACTORY PATTERN para dar de alta una nueva biografía / hito / foto
     * tanto en el servicio de Firestore/Storage como en el backend.
     */
    fun registerNewBiographyWithFactory(
        type: BiographyEntryType,
        titulo: String,
        anoOEpoca: String,
        descripcion: String,
        imagenUrl: String? = null,
        citaCelebre: String? = null,
        ubicacion: String? = null
    ) {
        viewModelScope.launch {
            // 1. Invocar la Factory
            val newItem = BiographyFactoryProducer.createAdminEntry(
                entryType = type,
                titulo = titulo,
                anoOEpoca = anoOEpoca,
                descripcion = descripcion,
                imagenUrl = imagenUrl,
                citaCelebre = citaCelebre,
                ubicacion = ubicacion
            )

            // 2. Persistir en Firestore / Storage local
            firestoreService.registerBiographyItem(newItem)

            // 3. Sincronizar con API REST si está disponible
            try {
                api.altaBiografia(
                    request = mx.utng.festivaltrack.shared.data.remote.BiografiaAltaDto(
                        titulo = newItem.titulo,
                        descripcion = newItem.descripcion,
                        citaCelebre = newItem.citaCelebre,
                        anoOEpoca = newItem.anoOEepoca,
                        imagenUrl = newItem.storageImageUrl,
                        tipo = newItem.entryType.name
                    )
                )
            } catch (e: Exception) {
                // Modo offline / tolerancia a fallas de red
            }
        }
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
