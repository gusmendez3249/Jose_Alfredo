package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.ui.utils.FormatoPago
import mx.utng.festivaltrack.shared.data.remote.BoletoDto
import mx.utng.festivaltrack.shared.data.remote.CompraBoletoDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/**
 * Estado de pago — patrón Sealed Class para representar el flujo de checkout.
 *
 * - [Idle]: pantalla en reposo, esperando que el usuario inicie el pago.
 * - [Processing]: pago en proceso, muestra un indicador de carga.
 * - [Success]: pago completado; trae los boletos reales que generó el servidor (con su QR).
 * - [Error]: mensaje para el usuario si la validación o el pago fallan.
 */
sealed class CheckoutState {
    object Idle : CheckoutState()
    object Processing : CheckoutState()
    data class Success(val boletos: List<BoletoDto>) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
}

/**
 * Boletos de la última compra, para la pantalla de "¡Pago exitoso!".
 * Vive en memoria: si la app se cierra, los boletos siguen en "Mis boletos".
 */
object UltimaCompra {
    var boletos: List<BoletoDto> = emptyList()
}

/**
 * ViewModel que gestiona la compra de boletos de un evento.
 *
 * Valida la tarjeta (pago simulado: los datos de la tarjeta no se mandan al servidor) y crea una
 * compra por categoría con `POST /boletos/comprar`; el backend calcula el monto oficial.
 */
class CheckoutViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState

    fun procesarPago(
        eventoId: String,
        vipTickets: Int,
        generalTickets: Int,
        titular: String,
        tarjetaNumero: String,
        tarjetaVencimiento: String,
        tarjetaCVV: String
    ) {
        FormatoPago.validar(titular, tarjetaNumero, tarjetaVencimiento, tarjetaCVV)?.let {
            _checkoutState.value = CheckoutState.Error(it)
            return
        }
        val token = tokenManager.getToken() ?: run {
            _checkoutState.value = CheckoutState.Error("Tu sesión terminó. Inicia sesión de nuevo para comprar.")
            return
        }

        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Processing
            try {
                // Una compra por categoría (#52): el backend cobra cada una con su precio oficial.
                val boletos = listOf("VIP" to vipTickets, "GENERAL" to generalTickets)
                    .filter { (_, cantidad) -> cantidad > 0 }
                    .map { (categoria, cantidad) ->
                        api.comprarBoleto(
                            "Bearer $token",
                            CompraBoletoDto(eventoId = eventoId, categoria = categoria, cantidad = cantidad, metodoPago = "TARJETA_CREDITO")
                        )
                    }
                UltimaCompra.boletos = boletos
                _checkoutState.value = CheckoutState.Success(boletos)
            } catch (e: retrofit2.HttpException) {
                _checkoutState.value = CheckoutState.Error(
                    when (e.code()) {
                        401 -> "Tu sesión terminó. Inicia sesión de nuevo para comprar."
                        400 -> "Este concierto ya no está a la venta."
                        404 -> "El concierto ya no existe."
                        else -> "No se pudo completar la compra. Intenta de nuevo."
                    }
                )
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Error("Sin conexión con el servidor. Revisa tu internet e intenta de nuevo.")
            }
        }
    }

    /** Quita el mensaje de error (por ejemplo, cuando el usuario corrige un campo). */
    fun limpiarError() {
        if (_checkoutState.value is CheckoutState.Error) _checkoutState.value = CheckoutState.Idle
    }

    fun resetState() {
        _checkoutState.value = CheckoutState.Idle
    }
}
