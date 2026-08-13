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

/**
 * Estado de la autenticación — patrón Sealed Class para representar
 * de forma exhaustiva todos los posibles estados del flujo auth.
 *
 * La UI observa [AuthViewModel.authState] y reacciona a cada estado:
 * - [Idle]: pantalla en reposo, sin acciones en curso.
 * - [Loading]: muestra un indicador de carga mientras espera respuesta del servidor.
 * - [Success]: navegación automática al dashboard (usuario o admin).
 * - [Error]: muestra un mensaje de error al usuario.
 */
sealed class AuthState {
    /** Estado inicial — no hay acción en curso. */
    object Idle : AuthState()

    /** Petición HTTP en progreso — mostrar loading indicator. */
    object Loading : AuthState()

    /**
     * Autenticación exitosa.
     * @property role Rol del usuario autenticado: "USER" o "ADMIN".
     *   Determina a qué pantalla navegar (dashboard normal o panel admin).
     */
    data class Success(val role: String) : AuthState()

    /**
     * Error durante la autenticación.
     * @property message Mensaje legible para mostrar al usuario.
     */
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel de autenticación para las pantallas [LoginScreen] y [RegisterScreen].
 *
 * Extiende [AndroidViewModel] para acceder al [Application] context,
 * necesario para instanciar [TokenManager] (que usa SharedPreferences).
 *
 * Responsabilidades:
 * - Verificar si hay una sesión activa al iniciar la app (auto-login).
 * - Realizar el login llamando a `POST /auth/login` y guardar el token.
 * - Realizar el registro llamando a `POST /auth/register`.
 * - Proporcionar el estado actual del flujo auth a través de un [StateFlow].
 * - Borrar la sesión al hacer logout.
 *
 * Patrón: MVVM — la UI nunca llama al API directamente, siempre a través del ViewModel.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    /** Gestiona el token JWT y datos de sesión en SharedPreferences. */
    private val tokenManager = TokenManager(application)

    /** Cliente HTTP Retrofit para llamadas al backend. */
    private val api = FestivalApiService.create()

    /** Estado mutable interno — solo el ViewModel puede modificarlo. */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)

    /**
     * Estado de autenticación observable por la UI.
     * Expuesto como [StateFlow] inmutable para cumplir el principio de encapsulamiento.
     */
    val authState: StateFlow<AuthState> = _authState

    /**
     * Verificación de auto-login al crear el ViewModel.
     *
     * Si hay un token y un rol guardados en SharedPreferences, emite [AuthState.Success]
     * inmediatamente, permitiendo a la UI navegar sin pasar por el login.
     *
     * Esto ocurre cuando el usuario ya inició sesión previamente y vuelve a abrir la app.
     */
    init {
        val token = tokenManager.getToken()
        val role = tokenManager.getUserRole()
        if (token != null && role != null) {
            _authState.value = AuthState.Success(role)
        }
    }

    /**
     * Inicia sesión con correo y contraseña.
     *
     * Validaciones locales:
     * - Los campos no pueden estar vacíos.
     *
     * Flujo asíncrono (en [viewModelScope]):
     * 1. Emite [AuthState.Loading].
     * 2. Llama a `POST /auth/login` con las credenciales.
     * 3. En éxito: guarda token, rol y userId en [TokenManager], emite [AuthState.Success].
     * 4. En error: emite [AuthState.Error] con mensaje genérico.
     *
     * @param correo Correo electrónico del usuario.
     * @param contrasena Contraseña del usuario en texto plano.
     */
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
                // Error genérico para no revelar si el correo existe o no (seguridad)
                _authState.value = AuthState.Error("Credenciales inválidas o error de red")
            }
        }
    }

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * Validaciones locales:
     * - Ningún campo puede estar vacío.
     * - La contraseña debe tener al menos 6 caracteres.
     *
     * Flujo asíncrono (en [viewModelScope]):
     * 1. Emite [AuthState.Loading].
     * 2. Llama a `POST /auth/register` con los datos del usuario.
     * 3. En éxito: guarda token y datos, emite [AuthState.Success].
     * 4. En error 409 (correo duplicado): muestra mensaje específico.
     * 5. En error de conexión: informa que el servidor no está disponible.
     *
     * @param nombre Nombre completo del nuevo usuario.
     * @param correo Correo electrónico (debe ser único en el sistema).
     * @param contrasena Contraseña (mínimo 6 caracteres).
     */
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
                val errorMsg = e.localizedMessage ?: "Error de conexión con el servidor"
                when {
                    errorMsg.contains("409") ->
                        _authState.value = AuthState.Error("El correo ya está registrado")
                    errorMsg.contains("Failed to connect") || errorMsg.contains("ConnectException") ->
                        _authState.value = AuthState.Error("No se pudo conectar al servidor en puerto 3001")
                    else ->
                        _authState.value = AuthState.Error("Error al registrarse: ${e.message}")
                }
            }
        }
    }

    /**
     * Cierra la sesión del usuario.
     * Borra el token JWT y todos los datos de sesión guardados en [TokenManager].
     * Emite [AuthState.Idle] para que la UI vuelva al estado inicial.
     */
    fun logout() {
        tokenManager.clear()
        _authState.value = AuthState.Idle
    }
    
    /**
     * Resetea el estado de error a [AuthState.Idle].
     * Útil para limpiar mensajes de error después de que el usuario los leyó,
     * por ejemplo al iniciar una nueva acción.
     */
    fun resetState() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
