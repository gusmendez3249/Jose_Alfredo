package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.ChatMessageDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

class LiveViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    private val _messages = MutableStateFlow<List<ChatMessageDto>>(emptyList())
    val messages: StateFlow<List<ChatMessageDto>> = _messages

    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl

    private val _userName = MutableStateFlow("Usuario")

    private var isPolling = false

    fun startLiveStream(eventoId: String) {
        isPolling = true
        // Fetch stream url
        viewModelScope.launch {
            try {
                // To get the event we fetch all events and find ours, or just use a specific endpoint
                val eventos = api.getEventos()
                val evento = eventos.find { it.id == eventoId }
                val transmision = evento?.transmision
                if (transmision?.estado == "EN_VIVO") {
                    _streamUrl.value = transmision.streamUrl
                }
            } catch (e: Exception) {
                // Ignore error, streamUrl remains null
            }
        }

        // Poll chat messages
        viewModelScope.launch {
            while (isPolling) {
                try {
                    val msgs = api.getChatMessages(eventoId)
                    _messages.value = msgs
                } catch (e: Exception) {
                    // Ignore polling errors
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    fun stopLiveStream() {
        isPolling = false
    }

    fun sendMessage(eventoId: String, text: String) {
        if (text.isBlank()) return
        
        val nombre = "Usuario" // Ideally fetch from profile

        viewModelScope.launch {
            try {
                api.sendChatMessage(
                    ChatMessageDto(
                        eventoId = eventoId,
                        usuarioNombre = nombre,
                        mensaje = text
                    )
                )
                // Instantly add to local list to feel snappy
                val current = _messages.value.toMutableList()
                current.add(
                    ChatMessageDto(
                        eventoId = eventoId,
                        usuarioNombre = nombre,
                        mensaje = text
                    )
                )
                _messages.value = current
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        isPolling = false
    }
}
