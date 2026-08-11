package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.BoletoDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val boletos: List<BoletoDto>, val userName: String) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    fun loadProfile() {
        val token = tokenManager.getToken()
        if (token == null) {
            _profileState.value = ProfileState.Error("No estás autenticado")
            return
        }

        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                // Here we fetch mis boletos
                val boletos = api.getMisBoletos("Bearer $token")
                
                // For a real app we'd fetch the user profile, but for now we'll just mock the name
                val userName = "Usuario" 
                
                _profileState.value = ProfileState.Success(boletos, userName)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error("Error al cargar tu perfil")
            }
        }
    }
}
