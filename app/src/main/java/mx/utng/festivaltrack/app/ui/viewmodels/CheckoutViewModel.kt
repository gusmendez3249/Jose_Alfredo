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

sealed class CheckoutState {
    object Idle : CheckoutState()
    object Processing : CheckoutState()
    object Success : CheckoutState()
    data class Error(val message: String) : CheckoutState()
}

class CheckoutViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState

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
    
    fun resetState() {
        _checkoutState.value = CheckoutState.Idle
    }
}
