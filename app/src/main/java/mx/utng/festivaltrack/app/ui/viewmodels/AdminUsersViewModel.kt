package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.RegisterDto
import mx.utng.festivaltrack.shared.data.remote.RoleUpdateDto
import mx.utng.festivaltrack.shared.data.remote.UsuarioDto

/**
 * Estado de la pantalla de gestión de usuarios.
 * 
 * - [Loading]: Muestra indicador de carga mientras se obtienen los usuarios.
 * - [Success]: Contiene la lista de usuarios cargada exitosamente.
 * - [Error]: Contiene un mensaje de error si falla la carga.
 */
sealed class AdminUsersState {
    /** Estado de carga inicial. */
    object Loading : AdminUsersState()
    
    /**
     * Estado exitoso con datos de usuarios.
     * @property users Lista de usuarios obtenidos desde el API.
     */
    data class Success(val users: List<UsuarioDto>) : AdminUsersState()
    
    /**
     * Estado de error.
     * @property message Mensaje de error a mostrar en la UI.
     */
    data class Error(val message: String) : AdminUsersState()
}

/**
 * ViewModel para gestionar usuarios desde el panel de administrador.
 * 
 * Responsabilidades:
 * - Cargar la lista de todos los usuarios registrados.
 * - Registrar nuevos administradores de forma directa.
 * - Cambiar el rol (USUARIO <-> ADMINISTRADOR) de los usuarios existentes.
 * 
 * @param application Instancia de la aplicación para acceder a [TokenManager].
 */
class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {
    private val api = FestivalApiService.create()
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow<AdminUsersState>(AdminUsersState.Loading)
    
    /** Flujo de estado observable para la interfaz de lista de usuarios. */
    val uiState: StateFlow<AdminUsersState> = _uiState

    private val _actionMessage = MutableStateFlow<String?>(null)
    
    /** Mensajes temporales de acciones (como éxito de registro o errores) para SnackBar. */
    val actionMessage: StateFlow<String?> = _actionMessage

    init {
        loadUsers()
    }

    /**
     * Carga todos los usuarios del sistema utilizando el token de administrador.
     */
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = AdminUsersState.Loading
            try {
                val token = "Bearer ${tokenManager.getToken()}"
                val users = api.getUsuarios(token)
                _uiState.value = AdminUsersState.Success(users)
            } catch (e: Exception) {
                _uiState.value = AdminUsersState.Error("Error al cargar usuarios: ${e.message}")
            }
        }
    }

    /**
     * Registra un nuevo administrador directamente en el sistema.
     * 
     * @param nombre Nombre del administrador.
     * @param correo Correo electrónico (único).
     * @param contrasena Contraseña para la cuenta.
     */
    fun registerAdmin(nombre: String, correo: String, contrasena: String) {
        if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank()) {
            _actionMessage.value = "Por favor, llena todos los campos"
            return
        }

        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenManager.getToken()}"
                api.registerAdmin(token, RegisterDto(nombre, correo, contrasena))
                _actionMessage.value = "Administrador registrado exitosamente"
                loadUsers() // Reload list
            } catch (e: Exception) {
                _actionMessage.value = "Error al registrar administrador: ${e.message}"
            }
        }
    }

    /**
     * Intercambia el rol del usuario especificado entre USUARIO y ADMINISTRADOR.
     * 
     * Actualiza la UI de manera optimista antes de confirmar el resultado completo, 
     * mejorando la respuesta percibida.
     * 
     * @param usuarioId ID del usuario a modificar.
     * @param currentRole El rol actual ("USUARIO" o "ADMINISTRADOR").
     */
    fun toggleRole(usuarioId: String, currentRole: String) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenManager.getToken()}"
                val newRole = if (currentRole == "ADMINISTRADOR") "USUARIO" else "ADMINISTRADOR"
                api.updateUserRole(token, usuarioId, RoleUpdateDto(newRole))
                
                // Update local state directly to be snappy
                val currentState = _uiState.value
                if (currentState is AdminUsersState.Success) {
                    val updatedUsers = currentState.users.map { 
                        if (it.id == usuarioId) it.copy(rol = newRole) else it 
                    }
                    _uiState.value = AdminUsersState.Success(updatedUsers)
                }
                _actionMessage.value = "Rol actualizado correctamente"
            } catch (e: Exception) {
                _actionMessage.value = "Error al cambiar rol: ${e.message}"
            }
        }
    }

    /**
     * Limpia el mensaje de acción actual (útil después de mostrarlo en un SnackBar).
     */
    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
