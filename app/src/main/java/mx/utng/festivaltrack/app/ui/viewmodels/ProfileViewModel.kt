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
import mx.utng.festivaltrack.shared.data.remote.VincularTvRequest

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val boletos: List<BoletoDto>, val userName: String) : ProfileState()
    /** [requiereLogin] = no hay sesión o ya venció: la pantalla ofrece iniciar sesión en lugar de reintentar. */
    data class Error(val message: String, val requiereLogin: Boolean = false) : ProfileState()
}

/** Cómo va la vinculación de una TV desde el perfil. */
sealed class VincularTvState {
    object Inactivo : VincularTvState()
    object Enviando : VincularTvState()
    object Listo : VincularTvState()
    data class Error(val mensaje: String) : VincularTvState()
}

/**
 * Deja el código como lo muestra la TV mientras se escribe: mayúsculas, solo letras y números,
 * máximo 8 y con guion a la mitad ("kx7m3q" → "KX7M-3Q").
 */
fun limpiarCodigoTv(texto: String): String {
    val limpio = texto.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(8)
    return if (limpio.length > 4) "${limpio.take(4)}-${limpio.drop(4)}" else limpio
}

/** Mensaje para cada respuesta de error de `POST auth/tv-sync`. */
fun mensajeErrorVincularTv(codigoHttp: Int): String = when (codigoHttp) {
    404 -> "Ese código no existe o ya venció. Revisa el que muestra la TV."
    400 -> "Escribe el código de 8 letras y números que muestra la TV."
    401 -> "Tu sesión expiró. Inicia sesión de nuevo."
    429 -> "Demasiados intentos. Espera un minuto e intenta de nuevo."
    else -> "No se pudo vincular la TV. Intenta de nuevo."
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _vincularTv = MutableStateFlow<VincularTvState>(VincularTvState.Inactivo)
    /** Estado del diálogo "Vincular una TV". */
    val vincularTv: StateFlow<VincularTvState> = _vincularTv

    /**
     * Aprueba el código que muestra la TV: la TV entra con esta cuenta en unos segundos
     * (ella pregunta al servidor cada 3 s).
     */
    fun vincularTv(codigo: String) {
        val token = tokenManager.getToken()
        if (token == null) {
            _vincularTv.value = VincularTvState.Error(mensajeErrorVincularTv(401))
            return
        }
        if (codigo.count { it.isLetterOrDigit() } != 8) {
            _vincularTv.value = VincularTvState.Error(mensajeErrorVincularTv(400))
            return
        }
        viewModelScope.launch {
            _vincularTv.value = VincularTvState.Enviando
            _vincularTv.value = try {
                api.vincularTv("Bearer $token", VincularTvRequest(codigo))
                VincularTvState.Listo
            } catch (e: retrofit2.HttpException) {
                VincularTvState.Error(mensajeErrorVincularTv(e.code()))
            } catch (e: Exception) {
                VincularTvState.Error("Sin conexión con el servidor. Revisa tu internet.")
            }
        }
    }

    /** Cierra el diálogo y lo deja listo para otra TV. */
    fun reiniciarVincularTv() {
        _vincularTv.value = VincularTvState.Inactivo
    }

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
