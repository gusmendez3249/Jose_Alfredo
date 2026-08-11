package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.LoginDto
import mx.utng.festivaltrack.shared.data.remote.RegisterDto

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Auto-login check on startup
    init {
        val token = tokenManager.getToken()
        val role = tokenManager.getUserRole()
        if (token != null && role != null) {
            _authState.value = AuthState.Success(role)
        }
    }

    fun login(correo: String, contrasena: String) {
        if (correo.isBlank() || contrasena.isBlank()) {
            _authState.value = AuthState.Error("Por favor llena todos los campos")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.login(LoginDto(correo, contrasena))
                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUserRole(response.usuario.rol)
                tokenManager.saveUserId(response.usuario.id)
                _authState.value = AuthState.Success(response.usuario.rol)
            } catch (e: Exception) {
                // If the error message is unhelpful, fallback to generic
                val msg = e.message ?: "Error al iniciar sesión"
                _authState.value = AuthState.Error("Credenciales inválidas o error de red")
            }
        }
    }

    fun register(nombre: String, correo: String, contrasena: String) {
        if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank()) {
            _authState.value = AuthState.Error("Todos los campos son obligatorios")
            return
        }
        
        if (contrasena.length < 6) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.register(RegisterDto(nombre, correo, contrasena))
                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUserRole(response.usuario.rol)
                tokenManager.saveUserId(response.usuario.id)
                _authState.value = AuthState.Success(response.usuario.rol)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error al registrarse. Intenta con otro correo.")
            }
        }
    }

    fun logout() {
        tokenManager.clear()
        _authState.value = AuthState.Idle
    }
    
    fun resetState() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
