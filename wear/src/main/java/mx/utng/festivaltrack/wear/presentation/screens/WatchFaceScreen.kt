package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.jose_alfredo.presentation.theme.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun WatchFaceScreen(onTap: () -> Unit) {
    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FestivalBackground)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Star",
                tint = FestivalGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Festival José\nAlfredo Jiménez",
                color = FestivalTextPrimary,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(FestivalGold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DOLORES HIDALGO",
                color = FestivalTextSecondary,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            Chip(
                onClick = onTap,
                label = { Text("VER EVENTOS", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "Lista",
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = FestivalGold,
                    contentColor = FestivalTextOnGold,
                    iconColor = FestivalTextOnGold
                )
            )
        }
    }
}
