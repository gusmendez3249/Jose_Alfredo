package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.utils.LugarFestival
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.EventoCreateDto
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

/** Estado del guardado del formulario de conciertos. */
sealed class GuardadoEvento {
    object Inactivo : GuardadoEvento()
    object Guardando : GuardadoEvento()
    object Listo : GuardadoEvento()
    data class Error(val mensaje: String) : GuardadoEvento()
}

/**
 * ViewModel encargado de la gestión de eventos por parte del administrador.
 * 
 * Permite listar, crear, actualizar y eliminar eventos. Interactúa con el 
 * [FestivalRepository] para mantener sincronizada la fuente de datos local 
 * (Room) con la remota (API).
 * 
 * @property repository Repositorio que maneja las operaciones de datos de eventos.
 */
class AdminManageViewModel(private val repository: FestivalRepository) : ViewModel() {

    /**
     * Flujo de estado que expone la lista de eventos locales.
     * Se actualiza automáticamente cuando hay cambios en la base de datos Room.
     */
    val eventos: StateFlow<List<EventoEntity>> = repository.getEventosLocales()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.syncEventos()
        }
    }

    private val _guardado = MutableStateFlow<GuardadoEvento>(GuardadoEvento.Inactivo)

    /** Resultado del último guardado del formulario de conciertos. */
    val guardado: StateFlow<GuardadoEvento> = _guardado

    /**
     * Crea o actualiza un concierto. Al editar, los campos null no se envían y el servidor
     * conserva su valor (capacidad, descripción, estado).
     *
     * @param id ID del concierto si es edición, o null si es alta.
     * @param fechaIso Inicio en UTC (ver [mx.utng.festivaltrack.app.ui.utils.FormularioEvento.fechaIso]).
     */
    fun guardarEvento(
        token: String?,
        id: String?,
        nombre: String,
        fechaIso: String,
        lugar: LugarFestival,
        capacidad: Int?,
        descripcion: String?
    ) {
        _guardado.value = GuardadoEvento.Guardando
        viewModelScope.launch {
            val dto = EventoCreateDto(
                nombre = nombre.trim(),
                fechaHora = fechaIso,
                ubicacion = lugar.ubicacion,
                escenario = lugar.escenario,
                capacidad = capacidad,
                estado = if (id.isNullOrEmpty()) "PUBLICADO" else null,
                descripcion = descripcion?.trim()?.takeIf { it.isNotEmpty() },
                latitud = lugar.latitud,
                longitud = lugar.longitud
            )
            _guardado.value = try {
                if (id.isNullOrEmpty()) repository.addEvento(token, java.util.UUID.randomUUID().toString(), dto)
                else repository.updateEvento(token, id, dto)
                GuardadoEvento.Listo
            } catch (e: retrofit2.HttpException) {
                GuardadoEvento.Error(
                    when (e.code()) {
                        401, 403 -> "Tu sesión de administrador terminó. Vuelve a iniciar sesión."
                        400 -> "El servidor rechazó los datos. Revisa el formulario."
                        else -> "No se pudo guardar el concierto. Intenta de nuevo."
                    }
                )
            } catch (e: Exception) {
                GuardadoEvento.Error("Sin conexión: el concierto quedó guardado solo en este teléfono.")
            }
        }
    }

    fun reiniciarGuardado() {
        _guardado.value = GuardadoEvento.Inactivo
    }

    /**
     * Elimina un evento dado su ID.
     * 
     * @param token Token JWT del administrador.
     * @param id Identificador único del evento a eliminar.
     */
    fun deleteEvent(token: String? = null, id: String) {
        viewModelScope.launch {
            try {
                repository.deleteEvento(token, id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Companion object para proveer el Factory necesario para instanciar 
     * el ViewModel con dependencias.
     */
    companion object {
        /**
         * Crea un [ViewModelProvider.Factory] para inyectar [FestivalRepository].
         * 
         * @param repository Instancia del repositorio de eventos.
         * @return Factory para construir [AdminManageViewModel].
         */
        fun provideFactory(repository: FestivalRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AdminManageViewModel::class.java)) {
                    return AdminManageViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
