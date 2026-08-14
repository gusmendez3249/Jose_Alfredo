package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.R
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Elemento de datos que representa una imagen en la galería histórica.
 *
 * @property id Identificador único.
 * @property title Título de la obra o fotografía.
 * @property category Categoría (ej. "Primeros Años", "Época de Oro").
 * @property drawableId Recurso estático de la imagen local.
 * @property isHighlighted Indica si el elemento debe ocupar más espacio/destacar.
 */
data class GalleryItem(
    val id: String,
    val title: String,
    val category: String,
    val drawableId: Int,
    val isHighlighted: Boolean = false
)

/**
 * Pantalla que muestra una galería de fotos e historia en un formato de grilla.
 * Permite filtrar por categorías (pestañas) usando los botones superiores.
 * Cada elemento de la grilla es navegable usando las flechas direccionales del control remoto.
 *
 * @param currentNavIndex Índice actual de la navegación en el menú lateral.
 * @param onNavSelect Callback para manejar la navegación del sidebar.
 */
@Composable
fun TvGalleryScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var activeDialogText by remember { mutableStateOf<String?>(null) }
    val tabs = listOf("Todo", "Primeros Años", "Época de Oro", "Legado")

    val allGalleryItems = remember {
        listOf(
            GalleryItem(
                "1",
                "Concierto en la Cuna de la Independencia",
                "Legado",
                R.drawable.hero_dolores_hidalgo,
                isHighlighted = true
            ),
            GalleryItem(
                "2",
                "El Atuendo Charro",
                "Época de Oro",
                R.drawable.jose_alfredo_portrait
            ),
            GalleryItem(
                "3",
                "La Voz del Pueblo",
                "Primeros Años",
                R.drawable.mariachi_gala_stage
            ),
            GalleryItem(
                "4",
                "Composición & Guitarra",
                "Época de Oro",
                R.drawable.ranchera_guitar
            ),
            GalleryItem(
                "5",
                "Dolores Hidalgo, 1954",
                "Primeros Años",
                R.drawable.hero_dolores_hidalgo
            ),
            GalleryItem(
                "6",
                "La Guitarra del Rey",
                "Legado",
                R.drawable.ranchera_guitar
            )
        )
    }

    val filteredItems = remember(selectedTab) {
        if (selectedTab == 0) allGalleryItems
        else allGalleryItems.filter { it.category == tabs[selectedTab] }
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

        // MAIN CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            // Header & Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Galería Histórica",
                    color = FestivalGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { idx, tabTitle ->
                        val isSelected = selectedTab == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = idx },
                            label = { Text(tabTitle) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FestivalGold,
                                selectedLabelColor = Color.Black,
                                containerColor = FestivalCardBg,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid of Gallery Items with Local Drawables
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    GalleryCard(item = item, onClick = { activeDialogText = "Visualizando pieza histórica: '${item.title}' (${item.category})" })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Card (Explora el Legado)
            Card(
                colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Explora el Legado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Usa el control para navegar por las fotografías históricas", color = FestivalTextSecondary, fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { activeDialogText = "Reproduciendo Documental Histórico de José Alfredo Jiménez en Smart TV." },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ver Documental", color = Color.White)
                        }

                        Button(
                            onClick = { activeDialogText = "Pase VIP Activado para la gala en Dolores Hidalgo." },
                            colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Obtener Pase VIP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (activeDialogText != null) {
        AlertDialog(
            onDismissRequest = { activeDialogText = null },
            title = { Text("Galería Histórica Smart TV", color = FestivalGold, fontWeight = FontWeight.Bold) },
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

/**
 * Componente interactivo (focusable) para mostrar una imagen de la galería.
 *
 * Cuando obtiene el foco ([Modifier.onFocusChanged]), se añade un borde dorado
 * para que el usuario sepa dónde está ubicado.
 *
 * @param item El [GalleryItem] que provee datos y la imagen a mostrar.
 * @param onClick Acción que se ejecuta al pulsar el botón principal sobre la tarjeta.
 */
@Composable
fun GalleryCard(item: GalleryItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2E3D30) else FestivalCardBg
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (item.isHighlighted) 160.dp else 130.dp)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) FestivalGold else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Local Drawable Background Image
            Image(
                painter = painterResource(id = item.drawableId),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark Gradient Overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    if (item.isHighlighted) {
                        Surface(
                            color = FestivalGold,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "DESTACADO",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.category,
                        color = FestivalGold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
