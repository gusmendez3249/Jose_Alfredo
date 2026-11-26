package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.ui.theme.*

/** Secciones de la app de TV, en el orden del menú. */
enum class SeccionTv(val titulo: String) {
    INICIO("Inicio"),
    PROGRAMACION("Programación"),
    EN_VIVO("Transmisión en vivo"),
    BIOGRAFIA("Biografía y música"),
    MAPA("Mapa"),
    GALERIA("Galería"),
    COMUNIDAD("Comunidad"),
    BOLETOS("Mis boletos"),
    AJUSTES("Ajustes")
}

/**
 * Menú lateral único para todas las pantallas de la TV (antes cada pantalla tenía su copia).
 *
 * @param seccion Sección activa.
 * @param enVivo Marca "EN VIVO" junto a la transmisión cuando de verdad hay una.
 * @param usuario Nombre de la sesión; null = invitado.
 * @param onComprarBoletos Abre el QR para comprar boletos desde el celular.
 */
@Composable
fun TvSidebar(
    seccion: SeccionTv,
    onSeccion: (SeccionTv) -> Unit,
    enVivo: Boolean,
    usuario: String?,
    onComprarBoletos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(FestivalSidebarBg)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("Festival José Alfredo Jiménez", color = FestivalGold, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
        Text("Dolores Hidalgo, Gto.", color = FestivalTextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // Las secciones ocupan el espacio libre y se desplazan si la pantalla es baja:
        // el usuario y "Comprar boletos" siempre quedan visibles abajo.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            val iconos = mapOf(
                SeccionTv.INICIO to Icons.Default.Home,
                SeccionTv.PROGRAMACION to Icons.Default.Event,
                SeccionTv.EN_VIVO to Icons.Default.LiveTv,
                SeccionTv.BIOGRAFIA to Icons.Default.LibraryMusic,
                SeccionTv.MAPA to Icons.Default.Map,
                SeccionTv.GALERIA to Icons.Default.Collections,
                SeccionTv.COMUNIDAD to Icons.Default.Forum,
                SeccionTv.BOLETOS to Icons.Default.ConfirmationNumber,
                SeccionTv.AJUSTES to Icons.Default.Settings
            )
            SeccionTv.entries.forEach { item ->
                Box {
                    SidebarMenuItem(
                        label = item.titulo,
                        icon = iconos.getValue(item),
                        isSelected = seccion == item,
                        onClick = { onSeccion(item) },
                        alto = 38.dp
                    )
                    if (item == SeccionTv.EN_VIVO && enVivo) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(FestivalBadgeLive)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    usuario ?: "Invitado",
                    color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onComprarBoletos,
                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPRAR BOLETOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
