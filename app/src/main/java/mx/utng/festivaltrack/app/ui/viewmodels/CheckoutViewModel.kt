package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.CompraBoletoDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/**
 * Estado de pago — patrón Sealed Class para representar el flujo de checkout.
 *
 * La UI observa [CheckoutViewModel.checkoutState] y reacciona a cada estado:
 * - [Idle]: pantalla en reposo, esperando que el usuario inicie el pago.
 * - [Processing]: pago en proceso, muestra un indicador de carga.
 * - [Success]: pago completado con éxito, navegación a pantalla de éxito.
 * - [Error]: muestra un mensaje de error si el pago falla.
 */
sealed class CheckoutState {
    /** Estado inicial o en reposo. */
    object Idle : CheckoutState()
    
    /** El pago está siendo procesado por el servidor. */
    object Processing : CheckoutState()
    
    /** Pago exitoso. */
    object Success : CheckoutState()
    
    /**
     * Error durante el proceso de pago.
     * @property message Mensaje de error para mostrar al usuario.
     */
    data class Error(val message: String) : CheckoutState()
}

/**
 * ViewModel que gestiona la lógica de compra de boletos para el evento.
 *
 * Responsabilidades:
 * - Validar los datos de la tarjeta (simulación local).
 * - Enviar la petición de compra al backend llamando a `POST /boletos/comprar`.
 * - Exponer el estado actual del checkout mediante [StateFlow].
 *
 * @param application La instancia de la aplicación, requerida para [TokenManager].
 */
class CheckoutViewModel(application: Application) : AndroidViewModel(application) {
    
    /** Gestiona el token JWT. */
    private val tokenManager = TokenManager(application)
    
    /** Cliente HTTP Retrofit para llamadas al backend. */
    private val api = FestivalApiService.create()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    
    /** Estado de checkout observable por la UI. */
    val checkoutState: StateFlow<CheckoutState> = _checkoutState

    /**
     * Procesa el pago de boletos validando la tarjeta y llamando al API.
     *
     * @param eventoId ID del evento para el cual se compran boletos.
     * @param categoria Categoría del boleto (e.g., VIP, General).
     * @param cantidad Cantidad de boletos a comprar.
     * @param precioTotal Monto total de la compra.
     * @param tarjetaNumero Número de la tarjeta de crédito/débito.
     * @param tarjetaVencimiento Fecha de expiración de la tarjeta.
     * @param tarjetaCVV Código de seguridad de la tarjeta.
     */
    fun procesarPago(
        eventoId: String,
        categoria: String,
        cantidad: Int,
        precioTotal: Int,
        tarjetaNumero: String,
        tarjetaVencimiento: String,
        tarjetaCVV: String
    ) {
        if (tarjetaNumero.isBlank() || tarjetaVencimiento.isBlank() || tarjetaCVV.isBlank()) {
            _checkoutState.value = CheckoutState.Error("Llena todos los datos de la tarjeta")
            return
        }
        
        // Simular validación básica
        if (tarjetaNumero.length < 15) {
            _checkoutState.value = CheckoutState.Error("Número de tarjeta inválido")
            return
        }

        val token = tokenManager.getToken()
        if (token == null) {
            _checkoutState.value = CheckoutState.Error("Sesión no encontrada. Inicia sesión nuevamente.")
            return
        }

        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Processing
            try {
                // Al ser un simulador, usamos cualquier método
                val dto = CompraBoletoDto(
                    eventoId = eventoId,
                    categoria = categoria,
                    cantidad = cantidad,
                    precioTotal = precioTotal,
                    metodoPago = "TARJETA_CREDITO"
                )
                api.comprarBoleto("Bearer $token", dto)
                _checkoutState.value = CheckoutState.Success
            } catch (e: Exception) {
                val detail = e.localizedMessage ?: "Error desconocido"
                _checkoutState.value = CheckoutState.Error("Error al procesar compra: $detail")
            }
        }
    }
    
    /**
     * Reinicia el estado de checkout a [CheckoutState.Idle].
     */
    fun resetState() {
        _checkoutState.value = CheckoutState.Idle
    }
}
