package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    onNavigateToCheckout: (Int, Int) -> Unit = { _, _ -> }
) {
    var selectedDate by remember { mutableStateOf("21") }
    var vipTickets by remember { mutableStateOf(1) }
    var generalTickets by remember { mutableStateOf(0) }

    val vipPrice = 4500
    val generalPrice = 1200

    val totalTickets = vipTickets + generalTickets
    val totalPrice = (vipTickets * vipPrice) + (generalTickets * generalPrice)

    val scrollState = rememberScrollState()
    
    val fieldColor = Color(0xFF1E2720)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boletos", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Go back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = "Boletos", tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            // Checkout Bar
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("TOTAL A PAGAR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$${totalPrice}", color = PrimaryGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Text(" MXN", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                            }
                        }
                        Text("$totalTickets BOLETO${if(totalTickets != 1) "S" else ""}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onNavigateToCheckout(totalPrice, totalTickets) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = totalTickets > 0
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("CONTINUAR COMPRA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Continuar")
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
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Selecciona tu fecha", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dates = listOf("21", "22", "23", "24")
                dates.forEach { date ->
                    val isSelected = selectedDate == date
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(
                                color = if (isSelected) PrimaryGold else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDate = date },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NOV", color = if (isSelected) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(date, color = if (isSelected) Color.Black else Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // VIP Ticket Card
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = fieldColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "VIP", tint = PrimaryGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Acceso VIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Box(
                            modifier = Modifier.background(PrimaryGold, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("LIMITADO", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Experiencia premium frente al escenario, barra libre y zona lounge.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = PrimaryGold.copy(alpha = 0.3f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$4,500", color = PrimaryGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(" MXN", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                        }
                        
                        // Counter
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (vipTickets > 0) vipTickets-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = PrimaryGold)
                            }
                            Text(vipTickets.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { vipTickets++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = PrimaryGold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // General Ticket Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = fieldColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = "General", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("General", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Entrada al festival con acceso a todas las áreas de comida y mercadito.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$1,200", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(" MXN", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                        }
                        
                        // Counter
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (generalTickets > 0) generalTickets-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = Color.White)
                            }
                            Text(generalTickets.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { generalTickets++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
