package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@Composable
fun AdminMainScreen(
    adminManageViewModel: mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel,
    onNavigateToCreateEvent: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.Gray
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGold,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = Color(0xFF2A2A2A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Subir") },
                    label = { Text("Subir") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGold,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = Color(0xFF2A2A2A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Gestionar") },
                    label = { Text("Gestionar") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGold,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = Color(0xFF2A2A2A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGold,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = Color(0xFF2A2A2A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> AdminDashboardScreen()
                1 -> AdminUploadScreen()
                2 -> AdminManageScreen(
                    viewModel = adminManageViewModel,
                    onNavigateToCreateEvent = onNavigateToCreateEvent
                )
                3 -> {
                    // Placeholder for profile
                    Button(onClick = onLogout, modifier = Modifier.padding(16.dp)) {
                        Text("Cerrar Sesión")
                    }
                }
            }
        }
    }
}
