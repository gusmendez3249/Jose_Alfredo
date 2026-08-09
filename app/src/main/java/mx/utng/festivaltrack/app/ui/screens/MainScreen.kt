package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Inicio")
    object Biography : BottomNavItem("biography", Icons.Default.Book, "Biografía")
    object Map : BottomNavItem("map", Icons.Default.Map, "Mapa")
    object Audio : BottomNavItem("audio", Icons.Default.Audiotrack, "Audio")
    object Tickets : BottomNavItem("tickets", Icons.Default.ConfirmationNumber, "Boletos")
}

@Composable
fun MainScreen(
    eventosViewModel: EventosViewModel? = null,
    onNavigateToCheckout: (Int, Int) -> Unit = { _, _ -> }
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Biography,
        BottomNavItem.Map,
        BottomNavItem.Audio,
        BottomNavItem.Tickets
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryGold,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            NavHost(navController = navController, startDestination = BottomNavItem.Home.route) {
                composable(BottomNavItem.Home.route) {
                    DashboardScreen(
                        eventosViewModel = eventosViewModel,
                        onNavigateToTickets = {
                            navController.navigate(BottomNavItem.Tickets.route)
                        }
                    )
                }
                composable(BottomNavItem.Biography.route) {
                    BiographyScreen()
                }
                composable(BottomNavItem.Map.route) {
                    MapScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(BottomNavItem.Audio.route) {
                    AudioScreen()
                }
                composable(BottomNavItem.Tickets.route) {
                    TicketsScreen(
                        eventosViewModel = eventosViewModel,
                        onNavigateToCheckout = { total, count -> onNavigateToCheckout(total, count) }
                    )
                }
            }
        }
    }
}
