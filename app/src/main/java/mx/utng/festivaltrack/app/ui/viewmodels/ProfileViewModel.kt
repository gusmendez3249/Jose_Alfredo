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
    /** [requiereLogin] = no hay sesión o ya venció: la pantalla ofrece iniciar sesión en lugar de reintentar. */
    data class Error(val message: String, val requiereLogin: Boolean = false) : ProfileState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    fun loadProfile() {
        val token = tokenManager.getToken()
        if (token == null) {
            _profileState.value = ProfileState.Error("Inicia sesión para ver tus boletos", requiereLogin = true)
            return
        }

        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                // Here we fetch mis boletos
                val boletos = api.getMisBoletos("Bearer $token")
                
                // Fetch the real name from TokenManager
                val userName = tokenManager.getUserName() ?: "Usuario" 
                
                _profileState.value = ProfileState.Success(boletos, userName)
            } catch (e: retrofit2.HttpException) {
                // 401: el token guardado ya venció o es de otra sesión; se borra para pedir login de nuevo
                if (e.code() == 401) {
                    tokenManager.clear()
                    _profileState.value = ProfileState.Error("Tu sesión expiró. Inicia sesión de nuevo", requiereLogin = true)
                } else {
                    _profileState.value = ProfileState.Error("No se pudieron cargar tus boletos")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error("No se pudieron cargar tus boletos. Revisa tu conexión")
            }
        }
    }
}
