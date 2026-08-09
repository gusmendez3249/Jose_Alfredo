package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

data class ScheduleItem(val time: String, val title: String, val stage: String, val artist: String, val isCurrent: Boolean = false)

@Composable
fun TvScheduleScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    var activeDialogText by remember { mutableStateOf<String?>(null) }

    val scheduleList = remember {
        listOf(
            ScheduleItem("18:00 HRS", "Serenata de Bienvenida", "Mausoleo José Alfredo", "Mariachi Femenil", isCurrent = false),
            ScheduleItem("19:30 HRS", "Gran Gala Mariachi", "Escenario Principal", "Mariachi Sol de México", isCurrent = true),
            ScheduleItem("21:00 HRS", "Homenaje Cuerdas de Dolores", "Teatro del Pueblo", "Orquesta Guanajuato", isCurrent = false),
            ScheduleItem("22:30 HRS", "Cierre Estelar: El Rey", "Escenario Principal", "Voces Magistrales", isCurrent = false)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // SIDEBAR
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Festival 2024", color = FestivalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("DOLORES HIDALGO", color = FestivalTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(32.dp))

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.LiveTv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = currentNavIndex == index,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // MAIN PROGRAMMING CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Programación del Festival",
                        color = FestivalGold,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Cartelera de Actos y Horarios en Dolores Hidalgo", color = FestivalTextSecondary, fontSize = 12.sp)
                }

                Surface(
                    color = FestivalCardBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("VIERNES 23 DE NOVIEMBRE", color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Schedule Timeline List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(scheduleList) { item ->
                    ScheduleCard(item = item, onClick = { activeDialogText = "Detalles del Acto:\n'${item.title}' por ${item.artist}\nEscenario: ${item.stage}\nHora: ${item.time}" })
                }
            }
        }
    }

    if (activeDialogText != null) {
        AlertDialog(
            onDismissRequest = { activeDialogText = null },
            title = { Text("Programación Smart TV", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text(activeDialogText!!, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { activeDialogText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = FestivalCardBg
        )
    }
}

@Composable
fun ScheduleCard(item: ScheduleItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCurrent) Color(0xFF2C2214) else if (isFocused) Color(0xFF2E3D30) else FestivalCardBg
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isFocused) 3.dp else if (item.isCurrent) 1.dp else 0.dp,
                color = if (isFocused) FestivalGold else if (item.isCurrent) FestivalGold.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (item.isCurrent) FestivalGold else FestivalSidebarBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.time,
                        color = if (item.isCurrent) Color.Black else FestivalGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (item.isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                Text("EN CURSO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${item.artist} • ${item.stage}", color = FestivalTextSecondary, fontSize = 12.sp)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FestivalGold)
        }
    }
}
