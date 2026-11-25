package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.utils.FormularioEvento
import mx.utng.festivaltrack.app.ui.utils.LugarFestival
import mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.GuardadoEvento
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FondoCampo = Color(0xFF1E2720)

@Composable
private fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryGold,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = FondoCampo,
    unfocusedContainerColor = FondoCampo,
    focusedLabelColor = PrimaryGold,
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = Color.White,
    disabledContainerColor = FondoCampo,
    disabledBorderColor = Color.Transparent,
    disabledLabelColor = Color.Gray,
    disabledTrailingIconColor = PrimaryGold
)

/**
 * Alta y edición de conciertos del administrador.
 *
 * La fecha y la hora se eligen con selectores y se interpretan en hora de Ciudad de México.
 * El lugar se elige de los sitios del festival, así el concierto llega con sus coordenadas
 * reales al mapa y al modo automóvil. Al editar se conservan los datos que no se tocan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateEventScreen(
    evento: EventoEntity? = null,
    viewModel: AdminManageViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val editando = evento != null
    val inicial = remember(evento) { FormularioEvento.desdeIso(evento?.fechaHora) }

    var nombre by remember(evento) { mutableStateOf(evento?.nombre ?: "") }
    var fecha by remember(evento) { mutableStateOf(inicial?.first) }
    var hora by remember(evento) { mutableStateOf(inicial?.second ?: LocalTime.of(21, 0)) }
    var lugar by remember(evento) {
        mutableStateOf<LugarFestival?>(FormularioEvento.lugarPara(evento?.escenario, evento?.latitud, evento?.longitud))
    }
    var capacidad by remember(evento) { mutableStateOf(if (editando) "" else "1000") }
    var descripcion by remember(evento) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var verFecha by remember { mutableStateOf(false) }
    var verHora by remember { mutableStateOf(false) }
    var verLugares by remember { mutableStateOf(false) }

    val guardado by viewModel.guardado.collectAsState()
    LaunchedEffect(Unit) { viewModel.reiniciarGuardado() }
    LaunchedEffect(guardado) {
        when (val g = guardado) {
            GuardadoEvento.Listo -> { viewModel.reiniciarGuardado(); onNavigateBack() }
            is GuardadoEvento.Error -> error = g.mensaje
            else -> Unit
        }
    }

    if (verFecha) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (fecha ?: LocalDate.now(FormularioEvento.ZONA))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { verFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    verFecha = false
                }) { Text("Aceptar", color = PrimaryGold) }
            },
            dismissButton = { TextButton(onClick = { verFecha = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
    if (verHora) {
        val estado = rememberTimePickerState(initialHour = hora.hour, initialMinute = hora.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { verHora = false },
            confirmButton = {
                TextButton(onClick = { hora = LocalTime.of(estado.hour, estado.minute); verHora = false }) {
                    Text("Aceptar", color = PrimaryGold)
                }
            },
            dismissButton = { TextButton(onClick = { verHora = false }) { Text("Cancelar") } },
            text = { TimePicker(state = estado) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editando) "Editar concierto" else "Nuevo concierto", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp).navigationBarsPadding()) {
                    error?.let { Text(it, color = Color(0xFFFF8A80), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                    Button(
                        onClick = {
                            error = FormularioEvento.validar(nombre, fecha, hora, lugar, capacidad, editando)
                            if (error == null) {
                                viewModel.guardarEvento(
                                    token = TokenManager(context).getToken(),
                                    id = evento?.id,
                                    nombre = nombre,
                                    fechaIso = FormularioEvento.fechaIso(fecha!!, hora),
                                    lugar = lugar!!,
                                    capacidad = capacidad.toIntOrNull(),
                                    descripcion = descripcion
                                )
                            }
                        },
                        enabled = guardado !is GuardadoEvento.Guardando,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        if (guardado is GuardadoEvento.Guardando) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (editando) "GUARDAR CAMBIOS" else "PUBLICAR CONCIERTO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it.take(120); error = null },
                label = { Text("Nombre del concierto") },
                singleLine = true,
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            val formatoFecha = remember { DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale("es", "MX")) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoSelector(
                    etiqueta = "Fecha",
                    valor = fecha?.let { formatoFecha.format(it) } ?: "",
                    icono = { Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir fecha") },
                    onClick = { verFecha = true },
                    modifier = Modifier.weight(1.6f)
                )
                CampoSelector(
                    etiqueta = "Hora",
                    valor = "%02d:%02d".format(hora.hour, hora.minute),
                    icono = { Icon(Icons.Default.Schedule, contentDescription = "Elegir hora") },
                    onClick = { verHora = true },
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Hora del centro de México.", color = Color.Gray, fontSize = 12.sp)

            Box {
                CampoSelector(
                    etiqueta = "Lugar",
                    valor = lugar?.let { "${it.escenario} · ${it.ubicacion}" } ?: "",
                    icono = { Icon(Icons.Default.Place, contentDescription = "Elegir lugar") },
                    onClick = { verLugares = true },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = verLugares, onDismissRequest = { verLugares = false }) {
                    FormularioEvento.LUGARES.forEach { l ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(l.escenario, fontWeight = FontWeight.Bold)
                                    Text(l.ubicacion, fontSize = 12.sp)
                                }
                            },
                            onClick = { lugar = l; verLugares = false; error = null }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = capacidad,
                onValueChange = { capacidad = it.filter(Char::isDigit).take(6); error = null },
                label = { Text("Capacidad (personas)") },
                placeholder = { if (editando) Text("Déjalo vacío para conservar la actual") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it.take(500) },
                label = { Text(if (editando) "Nueva descripción (opcional)" else "Descripción (opcional)") },
                minLines = 3,
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                "Los boletos se venden con los precios oficiales del festival: VIP \$4,500 y General \$1,200.",
                color = Color.Gray, fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Campo de solo lectura que abre un selector al tocarlo. */
@Composable
private fun CampoSelector(
    etiqueta: String,
    valor: String,
    icono: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            label = { Text(etiqueta) },
            placeholder = { Text("Elegir") },
            enabled = false,
            trailingIcon = icono,
            singleLine = true,
            colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        // El campo deshabilitado no recibe toques: una capa encima abre el selector.
        Surface(color = Color.Transparent, onClick = onClick, modifier = Modifier.matchParentSize()) {}
    }
}
