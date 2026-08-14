package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.ui.theme.FestivalGold

/**
 * Componente [Composable] para los elementos del menú en el Sidebar de la interfaz para Android TV.
 * Renderiza el ícono, el texto y reacciona al enfoque (D-Pad) cambiando el color y el fondo.
 *
 * @param label El texto a mostrar para este elemento del menú.
 * @param icon El [ImageVector] que representa a este elemento de forma visual.
 * @param isSelected Indica si este elemento es el que está actualmente activo/seleccionado.
 * @param onClick La función que se ejecutará al pulsar "OK" en el D-Pad sobre este elemento.
 */
@Composable
fun SidebarMenuItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) FestivalGold else if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
        contentColor = if (isSelected) Color.Black else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.Black else if (isFocused) FestivalGold else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
