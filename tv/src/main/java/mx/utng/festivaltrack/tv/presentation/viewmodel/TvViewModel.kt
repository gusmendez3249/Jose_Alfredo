package mx.utng.festivaltrack.tv.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.GaleriaDto
import mx.utng.festivaltrack.shared.data.remote.LoginDto
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository
import mx.utng.festivaltrack.shared.util.HorarioFestival
import java.time.Instant

/** Cuenta con la que se inició sesión en la TV. Solo vive en memoria: al cerrar la app se pide de nuevo. */
data class SesionTv(val nombre: String, val correo: String, val token: String)

/** Estado de la galería traída del servidor. */
sealed class EstadoGaleria {
    object Cargando : EstadoGaleria()
    data class Lista(val galerias: List<GaleriaDto>) : EstadoGaleria()
    object SinConexion : EstadoGaleria()
}

/**
 * ViewModel central de la app de Android TV.
 *
 * - **Cartelera**: eventos de Room sincronizados con el backend cada 5 s; [proximos] aplica las
 *   mismas reglas que el móvil (sin cancelados ni pasados, ordenados, hora de México).
 * - **Transmisión**: consulta `stream/status` cada 3 s; [stream] es null si el servidor no responde.
 * - **Sesión**: login real contra el backend; [sesion] es null para ver como invitado.
 * - **Reloj**: [ahora] avanza cada 30 s para que "Hoy", "En curso" y la hora se actualicen solos.
 */
class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FestivalRepository
    private val api = FestivalApiService.create()

    private val _eventos = MutableStateFlow<List<EventoEntity>>(emptyList())
    val eventos: StateFlow<List<EventoEntity>> = _eventos.asStateFlow()

    private val _ahora = MutableStateFlow(Instant.now())
    val ahora: StateFlow<Instant> = _ahora.asStateFlow()

    /** Conciertos que todavía no terminan, del más próximo al más lejano. */
    val proximos: StateFlow<List<EventoEntity>> = combine(_eventos, _ahora) { lista, ahora ->
        HorarioFestival.proximos(lista, ahora)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _stream = MutableStateFlow<StreamStatusDto?>(null)
    /** Estado de la transmisión; null mientras no hay respuesta del servidor. */
    val stream: StateFlow<StreamStatusDto?> = _stream.asStateFlow()

    private val _servidorConectado = MutableStateFlow<Boolean?>(null)
    /** null = todavía no se sabe; true/false según la última consulta al backend. */
    val servidorConectado: StateFlow<Boolean?> = _servidorConectado.asStateFlow()

    private val _galeria = MutableStateFlow<EstadoGaleria>(EstadoGaleria.Cargando)
    val galeria: StateFlow<EstadoGaleria> = _galeria.asStateFlow()

    private val _sesion = MutableStateFlow<SesionTv?>(null)
    val sesion: StateFlow<SesionTv?> = _sesion.asStateFlow()

    init {
        val database = FestivalDatabase.getInstance(application)
        repository = FestivalRepository(database.eventoDao())

        viewModelScope.launch {
            repository.getEventosLocales().collectLatest { list -> _eventos.value = list }
        }

        // Cartelera: sincroniza con el backend al abrir y cada 5 s.
        viewModelScope.launch {
            while (true) {
                runCatching { repository.syncEventos() }
                delay(5_000)
            }
        }

        // Transmisión en vivo y estado del servidor: cada 3 s.
        viewModelScope.launch {
            while (true) {
                val estado = runCatching { api.getStreamStatus() }.getOrNull()
                _stream.value = estado
                _servidorConectado.value = estado != null
                delay(3_000)
            }
        }

        viewModelScope.launch {
            while (true) {
                _ahora.value = Instant.now()
                delay(30_000)
            }
        }

        cargarGaleria()
    }

    /** Vuelve a pedir las galerías al servidor. */
    fun cargarGaleria() {
        _galeria.value = EstadoGaleria.Cargando
        viewModelScope.launch {
            _galeria.value = runCatching { api.getGalerias() }
                .map { EstadoGaleria.Lista(it) }
                .getOrDefault(EstadoGaleria.SinConexion)
        }
    }

    /**
     * Inicia sesión contra el backend. Regresa null si entró o el mensaje de error para mostrar
     * (los mismos textos que el móvil).
     */
    suspend fun iniciarSesion(correo: String, contrasena: String): String? {
        if (correo.isBlank() || contrasena.isBlank()) return "Escribe tu correo y tu contraseña"
        return try {
            val respuesta = api.login(LoginDto(correo.trim(), contrasena))
            _sesion.value = SesionTv(respuesta.usuario.nombre, respuesta.usuario.correo, respuesta.accessToken)
            null
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                400, 401 -> "Correo o contraseña incorrectos"
                429 -> "Demasiados intentos. Espera un minuto e intenta de nuevo."
                else -> "El servidor no respondió. Intenta de nuevo."
            }
        } catch (e: Exception) {
            "Sin conexión con el servidor. Revisa la red de la TV."
        }
    }

    fun cerrarSesion() {
        _sesion.value = null
    }
}
