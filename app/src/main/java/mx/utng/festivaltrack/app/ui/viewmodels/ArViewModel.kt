package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.ar.ArFacade
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import mx.utng.festivaltrack.shared.data.remote.VisitaSitioDto

/** Pasos del flujo de la pantalla 1: apuntar → capturar foto → reconocimiento IA → resultado AR. */
sealed class ArState {
    object Apuntando : ArState()
    object Capturando : ArState()
    object Reconociendo : ArState()
    data class Reconocido(val sitio: SitioArDto, val confianza: Double) : ArState()
    data class NoReconocido(val mensaje: String) : ArState()
    data class Error(val mensaje: String) : ArState()
}

sealed class HistorialArState {
    object Loading : HistorialArState()
    data class Success(val visitas: List<VisitaSitioDto>) : HistorialArState()
    data class Error(val mensaje: String) : HistorialArState()
}

class ArViewModel(application: Application) : AndroidViewModel(application) {
    private val facade = ArFacade(application)

    private val _state = MutableStateFlow<ArState>(ArState.Apuntando)
    val state: StateFlow<ArState> = _state

    private val _historial = MutableStateFlow<HistorialArState>(HistorialArState.Loading)
    val historial: StateFlow<HistorialArState> = _historial

    fun reconocer(imageCapture: ImageCapture) {
        if (_state.value is ArState.Capturando || _state.value is ArState.Reconociendo) return
        viewModelScope.launch {
            _state.value = ArState.Capturando
            try {
                val r = facade.reconocer(imageCapture) { _state.value = ArState.Reconociendo }
                _state.value = if (r.reconocido && r.sitio != null) {
                    ArState.Reconocido(r.sitio!!, r.confianza ?: 0.0)
                } else {
                    ArState.NoReconocido(r.mensaje ?: "No reconocimos ningún sitio")
                }
            } catch (e: IllegalStateException) {
                _state.value = ArState.Error(e.message ?: "Inicia sesión para continuar")
            } catch (e: Exception) {
                _state.value = ArState.Error(mensajeDeError(e))
            }
        }
    }

    /** Traduce la excepción a un mensaje útil para el usuario (y para depurar). */
    private fun mensajeDeError(e: Exception): String = when (e) {
        is retrofit2.HttpException -> when (e.code()) {
            401 -> "Tu sesión expiró. Cierra sesión y vuelve a entrar."
            503 -> "El servicio de reconocimiento no está disponible (error del servidor de IA)."
            else -> "El servidor respondió con error ${e.code()}."
        }
        is java.net.SocketTimeoutException -> "El reconocimiento tardó demasiado. Intenta de nuevo."
        is java.io.IOException -> "No se pudo conectar con el servidor. Revisa tu Wi-Fi."
        else -> "No se pudo reconocer el sitio (${e.javaClass.simpleName})."
    }

    /** Vuelve a la vista de cámara para escanear otro sitio. */
    fun reiniciar() {
        _state.value = ArState.Apuntando
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            _historial.value = HistorialArState.Loading
            _historial.value = try {
                HistorialArState.Success(facade.historial())
            } catch (e: IllegalStateException) {
                HistorialArState.Error(e.message ?: "Inicia sesión para ver tu historial")
            } catch (e: Exception) {
                HistorialArState.Error("Error al cargar el historial")
            }
        }
    }
}
