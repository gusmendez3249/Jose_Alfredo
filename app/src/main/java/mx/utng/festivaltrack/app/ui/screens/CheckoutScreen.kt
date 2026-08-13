package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.CheckoutViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.CheckoutState

/**
 * Pantalla de Procesamiento de Pago y Compra de Boletos ([CheckoutScreen]).
 *
 * Permite ingresar la información de pago con tarjeta de crédito/débito para finalizar
 * la compra de accesos al festival. Se conecta con [CheckoutViewModel] para enviar
 * la petición `POST /boletos/comprar` al backend REST.
 *
 * @param eventoId ID del evento seleccionado (ej. "EVT-001"). Si está vacío usa "EVT-001" como fallback.
 * @param totalPrice Monto total acumulado en pesos mexicanos (MXN).
 * @param totalTickets Cantidad total de boletos a comprar.
 * @param onNavigateBack Callback de navegación para regresar al paso anterior.
 * @param onPaymentSuccess Callback ejecutado cuando el backend confirma el pago con éxito.
 * @param viewModel Instancia del ViewModel para la lógica de negocio del checkout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    eventoId: String = "",
    totalPrice: Int = 4500,
    totalTickets: Int = 1,
    onNavigateBack: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {},
    viewModel: CheckoutViewModel = viewModel()
) {
    // Estados locales para los campos de texto del formulario de pago
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }

    // Observa los estados del Checkout ViewModel (Idle, Processing, Success, Error)
    val checkoutState by viewModel.checkoutState.collectAsState()

    // Escucha cambios de estado: al tener éxito, reinicia el estado y navega a TicketSuccessScreen
    LaunchedEffect(checkoutState) {
        if (checkoutState is CheckoutState.Success) {
            viewModel.resetState()
            onPaymentSuccess()
        }
    }

    val scrollState = rememberScrollState()
    val fieldColor = Color(0xFF1E2720)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pago Seguro", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Muestra mensajes de error devueltos por la validación o el backend
                    if (checkoutState is CheckoutState.Error) {
                        Text(
                            text = (checkoutState as CheckoutState.Error).message,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.procesarPago(
                                eventoId = if (eventoId.isNotBlank()) eventoId else "EVT-001",
                                categoria = "VIP",
                                cantidad = totalTickets,
                                precioTotal = totalPrice,
                                tarjetaNumero = cardNumber,
                                tarjetaVencimiento = expiryDate,
                                tarjetaCVV = cvv
                            )
                        },
                        enabled = checkoutState !is CheckoutState.Processing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (checkoutState is CheckoutState.Processing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "CONFIRMAR PAGO - $$totalPrice MXN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Resumen de la Orden
            Card(
                colors = CardDefaults.cardColors(containerColor = fieldColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumen del Pedido", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Accesos ($totalTickets x VIP)", color = Color.White)
                        Text("$$totalPrice MXN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cargos por servicio", color = Color.Gray, fontSize = 12.sp)
                        Text("Incluidos", color = PrimaryGold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Formulario de Tarjeta Bancaria
            Text("Detalles de la Tarjeta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Nombre del Titular
            OutlinedTextField(
                value = cardHolder,
                onValueChange = { cardHolder = it },
                label = { Text("Nombre del Titular") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Número de Tarjeta
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { if (it.length <= 16) cardNumber = it },
                label = { Text("Número de Tarjeta (16 dígitos)") },
                trailingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = PrimaryGold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Fecha de Expiración
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { if (it.length <= 5) expiryDate = it },
                    label = { Text("MM/AA") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldColor,
                        unfocusedContainerColor = fieldColor,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Código CVV
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { if (it.length <= 4) cvv = it },
                    label = { Text("CVV") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldColor,
                        unfocusedContainerColor = fieldColor,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Leyenda de Pago Encriptado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transacción encriptada con tecnología SSL de 256 bits.", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
