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
import mx.utng.festivaltrack.shared.data.remote.BiografiaDto
import mx.utng.festivaltrack.shared.data.remote.BoletoDto
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.PublicacionDto
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import mx.utng.festivaltrack.shared.data.remote.GaleriaDto
import mx.utng.festivaltrack.shared.data.remote.LoginDto
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.data.DatosSesion
import mx.utng.festivaltrack.tv.data.SesionGuardadaTv
import retrofit2.HttpException
import java.time.Instant

/**
 * Cuenta con la que se inició sesión en la TV. Se guarda cifrada ([SesionGuardadaTv]) y se
 * restaura al abrir la app mientras el token siga vigente.
 */
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
 * - **Sesión**: login real contra el backend; [sesion] es null para ver como invitado. Se guarda
 *   en la TV y, si el servidor rechaza el token (401), se cierra sola.
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

    private val sesionGuardada = SesionGuardadaTv(application)

    private val _sesion = MutableStateFlow(sesionGuardada.leer()?.let { SesionTv(it.nombre, it.correo, it.token) })
    /** Sesión actual; ya viene restaurada al crear el ViewModel si había una guardada y vigente. */
    val sesion: StateFlow<SesionTv?> = _sesion.asStateFlow()

    /** Música: demos de 30 s, como en el móvil. */
    val reproductor = ReproductorTv(application, viewModelScope)
    val musica: StateFlow<EstadoMusica> = reproductor.estado

    private val _biografia = MutableStateFlow<Cargable<BiografiaDto?>>(Cargable.Cargando)
    val biografia: StateFlow<Cargable<BiografiaDto?>> = _biografia.asStateFlow()

    private val _canciones = MutableStateFlow<Cargable<List<CancionDto>>>(Cargable.Cargando)
    /** Catálogo de canciones del artista (`GET /canciones`). */
    val canciones: StateFlow<Cargable<List<CancionDto>>> = _canciones.asStateFlow()

    private val _sitios = MutableStateFlow<List<SitioArDto>>(emptyList())
    val sitios: StateFlow<List<SitioArDto>> = _sitios.asStateFlow()

    private val _comunidad = MutableStateFlow<Cargable<List<PublicacionDto>>>(Cargable.Cargando)
    val comunidad: StateFlow<Cargable<List<PublicacionDto>>> = _comunidad.asStateFlow()

    private val _boletos = MutableStateFlow<Cargable<List<BoletoDto>>>(Cargable.Cargando)
    /** Boletos de la cuenta con la que se inició sesión. */
    val boletos: StateFlow<Cargable<List<BoletoDto>>> = _boletos.asStateFlow()

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

        // Sesión restaurada: se confirma con el servidor (si ya no acepta el token, se cierra).
        _sesion.value?.let { restaurada ->
            viewModelScope.launch {
                runCatching { api.getMisBoletos("Bearer ${restaurada.token}") }
                    .onFailure { if (esTokenRechazado(it)) cerrarSesionSiSigue(restaurada.token) }
            }
        }

        cargarGaleria()
        cargarBiografiaYMusica()
        cargarComunidad()
        viewModelScope.launch { runCatching { api.getSitiosAr() }.onSuccess { _sitios.value = it } }
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
            sesionGuardada.guardar(DatosSesion(respuesta.usuario.nombre, respuesta.usuario.correo, respuesta.accessToken))
            null
        } catch (e: HttpException) {
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
        sesionGuardada.borrar()
        _sesion.value = null
        _boletos.value = Cargable.Cargando
    }

    /** El servidor ya no acepta el token (venció, se cambió la llave o se borró la cuenta). */
    private fun esTokenRechazado(error: Throwable) = (error as? HttpException)?.code() == 401

    /** Cierra la sesión por un token rechazado, salvo que mientras tanto se haya entrado con otra. */
    private fun cerrarSesionSiSigue(token: String) {
        if (_sesion.value?.token == token) cerrarSesion()
    }

    /** Biografía del artista y catálogo de canciones. */
    fun cargarBiografiaYMusica() {
        _biografia.value = Cargable.Cargando
        _canciones.value = Cargable.Cargando
        viewModelScope.launch {
            _biografia.value = runCatching { api.getBiografias().firstOrNull() }
                .map { Cargable.Listo(it) }.getOrDefault(Cargable.SinConexion)
        }
        viewModelScope.launch {
            _canciones.value = runCatching { api.getCanciones() }
                .map { Cargable.Listo(it) }.getOrDefault(Cargable.SinConexion)
        }
    }

    /** Publicaciones de la comunidad (las mismas que se ven en la app del celular). */
    fun cargarComunidad() {
        _comunidad.value = Cargable.Cargando
        viewModelScope.launch {
            val actual = _sesion.value?.token
            val token = actual?.let { "Bearer $it" }
            var resultado = runCatching { api.getForo(token = token, limit = 30) }
            // Con un token que el servidor ya no acepta, se cierra la sesión y se lee como invitado.
            if (actual != null && resultado.exceptionOrNull()?.let(::esTokenRechazado) == true) {
                cerrarSesionSiSigue(actual)
                resultado = runCatching { api.getForo(token = null, limit = 30) }
            }
            _comunidad.value = resultado.map { Cargable.Listo(it) }.getOrDefault(Cargable.SinConexion)
        }
    }

    /** Boletos de la sesión actual; sin sesión se queda en [Cargable.Cargando] (la pantalla pide entrar). */
    fun cargarBoletos() {
        val sesion = _sesion.value ?: return
        _boletos.value = Cargable.Cargando
        viewModelScope.launch {
            val resultado = runCatching { api.getMisBoletos("Bearer ${sesion.token}") }
            if (resultado.exceptionOrNull()?.let(::esTokenRechazado) == true) {
                cerrarSesionSiSigue(sesion.token) // la pantalla vuelve a pedir que se inicie sesión
                return@launch
            }
            _boletos.value = resultado.map { Cargable.Listo(it) }.getOrDefault(Cargable.SinConexion)
        }
    }

    /** Repertorio de un concierto para escuchar sus demos. */
    suspend fun repertorio(eventoId: String): List<CancionDto>? =
        runCatching { api.getCancionesEvento(eventoId) }.getOrNull()

    override fun onCleared() {
        reproductor.liberar()
        super.onCleared()
    }
}

/** Datos que se piden al servidor: cargando, listos o sin conexión. */
sealed class Cargable<out T> {
    object Cargando : Cargable<Nothing>()
    data class Listo<T>(val datos: T) : Cargable<T>()
    object SinConexion : Cargable<Nothing>()
}
