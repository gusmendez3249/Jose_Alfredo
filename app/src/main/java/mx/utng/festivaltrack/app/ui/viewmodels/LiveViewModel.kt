package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.ChatMessageDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto

/** Qué ve el usuario en la zona del video. */
sealed class EstadoTransmision {
    /** Primera consulta al servidor. */
    object Consultando : EstadoTransmision()

    /** Este concierto no está transmitiendo; se reintenta solo. */
    data class SinTransmision(val fechaHora: String?, val nombreEvento: String?) : EstadoTransmision()

    /** Se transmite otro concierto en este momento. */
    data class OtroConcierto(val eventoId: String, val nombre: String) : EstadoTransmision()

    /** En vivo: [urls] en orden de preferencia (IP del celular del admin, luego la del emulador). */
    data class EnVivo(val urls: List<String>) : EstadoTransmision()

    /** Sin conexión con el servidor. */
    object SinConexion : EstadoTransmision()
}

/**
 * Transmisión en vivo de un concierto: consulta cada pocos segundos el estado que publica el
 * panel del administrador (`GET /stream/status`) y el chat del concierto.
 * Cuando el admin inicia, el video aparece solo; cuando detiene, vuelve el aviso.
 */
class LiveViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    private val _messages = MutableStateFlow<List<ChatMessageDto>>(emptyList())
    val messages: StateFlow<List<ChatMessageDto>> = _messages

    private val _estado = MutableStateFlow<EstadoTransmision>(EstadoTransmision.Consultando)
    val estado: StateFlow<EstadoTransmision> = _estado

    private val _errorChat = MutableStateFlow<String?>(null)
    val errorChat: StateFlow<String?> = _errorChat

    private var sondeo: Job? = null
    private var fechaEvento: String? = null
    private var nombreEvento: String? = null

    fun startLiveStream(eventoId: String) {
        if (sondeo?.isActive == true) return
        sondeo = viewModelScope.launch {
            runCatching { api.getEventos().find { it.id == eventoId } }.getOrNull()?.let {
                fechaEvento = it.fechaHora
                nombreEvento = it.nombre
            }
            while (isActive) {
                actualizarEstado(eventoId)
                runCatching { api.getChatMessages(eventoId) }.onSuccess { _messages.value = it }
                delay(SONDEO_MS)
            }
        }
    }

    private suspend fun actualizarEstado(eventoId: String) {
        val status = runCatching { api.getStreamStatus() }.getOrElse {
            _estado.value = EstadoTransmision.SinConexion
            return
        }
        val nuevo = estadoPara(status, eventoId, fechaEvento, nombreEvento)
        // No reinicia el reproductor si sigue la misma transmisión.
        if (nuevo != _estado.value) _estado.value = nuevo
    }

    fun stopLiveStream() {
        sondeo?.cancel()
        sondeo = null
    }

    fun sendMessage(eventoId: String, text: String) {
        if (text.isBlank()) return
        val nombre = tokenManager.getUserName() ?: "Usuario"
        // El chat exige sesión (#50): el backend toma el nombre y el rol del token.
        val token = tokenManager.getToken() ?: run {
            _errorChat.value = "Inicia sesión para escribir en el chat"
            return
        }
        viewModelScope.launch {
            try {
                api.sendChatMessage("Bearer $token", ChatMessageDto(eventoId = eventoId, usuarioNombre = nombre, mensaje = text))
                _messages.value = _messages.value + ChatMessageDto(eventoId = eventoId, usuarioNombre = nombre, mensaje = text)
                _errorChat.value = null
            } catch (e: Exception) {
                _errorChat.value = "No se pudo enviar el mensaje. Intenta de nuevo."
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveStream()
    }

    companion object {
        const val SONDEO_MS = 4000L

        /** Decide qué mostrar para el concierto [eventoId] según el estado publicado por el admin. */
        fun estadoPara(
            status: StreamStatusDto,
            eventoId: String,
            fechaEvento: String? = null,
            nombreEvento: String? = null
        ): EstadoTransmision {
            val urls = listOf(status.streamUrl, status.emulatorUrl).filter { it.isNotBlank() }.distinct()
            return when {
                !status.isLive || urls.isEmpty() -> EstadoTransmision.SinTransmision(fechaEvento, nombreEvento)
                status.eventoId != null && status.eventoId != eventoId ->
                    EstadoTransmision.OtroConcierto(status.eventoId!!, status.eventoNombre ?: "otro concierto")
                else -> EstadoTransmision.EnVivo(urls)
            }
        }
    }
}
