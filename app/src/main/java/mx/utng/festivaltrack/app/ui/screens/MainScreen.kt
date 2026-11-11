package mx.utng.festivaltrack.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import mx.utng.festivaltrack.app.playback.PlaybackManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    object Concerts : BottomNavItem("concerts", Icons.Default.Audiotrack, "Conciertos")
    object Tickets : BottomNavItem("tickets", Icons.Default.ConfirmationNumber, "Comprar")
    object Gallery : BottomNavItem("gallery", Icons.Default.PhotoLibrary, "Galería")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Boletos")
}

@Composable
fun MainScreen(
    eventosViewModel: EventosViewModel? = null,
    onNavigateToCheckout: (eventoId: String, vip: Int, general: Int) -> Unit = { _, _, _ -> },
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    // Concierto elegido desde su detalle; la compra se hace para ese evento.
    var ticketsEventId by remember { mutableStateOf<String?>(null) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Biography,
        BottomNavItem.Map,
        BottomNavItem.Concerts,
        BottomNavItem.Tickets,
        BottomNavItem.Gallery,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Reproductor global (#25): la pantalla "Reproduciendo" y el permiso de notificaciones (Android 13+)
    val playerSheet = rememberPlayerSheetState()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    // El permiso se pide una sola vez, la primera vez que suena algo; si se niega no se insiste.
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        if (isPlaying && !notificationAsked && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationAsked = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Los errores de reproducción se avisan en cualquier pantalla, no solo en el concierto.
    val snackbar = remember { SnackbarHostState() }
    val playbackError by PlaybackManager.error.collectAsState()
    LaunchedEffect(playbackError) {
        playbackError?.let {
            snackbar.showSnackbar(it)
            PlaybackManager.dismissError()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val screenHeightPx = constraints.maxHeight.toFloat()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // Acceso al módulo AR desde Inicio
            if (currentRoute == BottomNavItem.Home.route) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("ar") },
                    icon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = null) },
                    text = { Text("Explorar AR") },
                    containerColor = PrimaryGold,
                    contentColor = Color.Black
                )
            }
        },
        bottomBar = {
            // La cámara AR usa pantalla completa
            if (currentRoute?.startsWith("ar") == true) return@Scaffold
            Column {
            // Se ve en toda la app mientras haya una demo en la cola
            GlobalMiniPlayer(playerSheet, screenHeightPx)
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Color.White
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 9.sp, letterSpacing = 0.sp, maxLines = 1, softWrap = false) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (item == BottomNavItem.Tickets) ticketsEventId = null
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryGold,
                            selectedTextColor = PrimaryGold,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
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
                        },
                        onNavigateToLive = { eventoId ->
                            navController.navigate("live/$eventoId")
                        },
                        onNavigateToBiography = {
                            navController.navigate(BottomNavItem.Biography.route)
                        },
                        onNavigateToGallery = {
                            navController.navigate(BottomNavItem.Gallery.route)
                        },
                        onNavigateToEvent = { eventoId ->
                            navController.navigate("event/$eventoId")
                        }
                    )
                }
                composable(BottomNavItem.Biography.route) {
                    BiographyScreen()
                }
                composable(BottomNavItem.Map.route) {
                    MapScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onElegirDestino = { navController.navigate("destinos") }
                    )
                }
                composable("destinos") {
                    ElegirDestinoScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(BottomNavItem.Concerts.route) {
                    ConcertsScreen(
                        eventosViewModel = eventosViewModel,
                        onOpenEvent = { eventoId -> navController.navigate("event/$eventoId") }
                    )
                }
                composable(BottomNavItem.Tickets.route) {
                    TicketsScreen(
                        eventosViewModel = eventosViewModel,
                        eventoPreseleccionado = ticketsEventId,
                        onNavigateToCheckout = { eventoId, vip, general -> onNavigateToCheckout(eventoId, vip, general) }
                    )
                }
                composable("event/{eventoId}") { backStackEntry ->
                    val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                    EventDetailScreen(
                        eventoId = eventoId,
                        eventosViewModel = eventosViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onBuyTickets = { id ->
                            ticketsEventId = id
                            navController.navigate(BottomNavItem.Tickets.route) { launchSingleTop = true }
                        }
                    )
                }
                composable(BottomNavItem.Gallery.route) {
                    GalleryScreen()
                }
                composable(BottomNavItem.Profile.route) {
                    mx.utng.festivaltrack.app.ui.screens.ProfileScreen(
                        onNavigateBack = { navController.navigate(BottomNavItem.Home.route) },
                        onLogout = onLogout
                    )
                }
                composable("ar") {
                    ArScannerScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onVerHistorial = { navController.navigate("ar/historial") },
                        onVerGaleria = { navController.navigate(BottomNavItem.Gallery.route) }
                    )
                }
                composable("ar/historial") {
                    ArHistoryScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("live/{eventoId}") { backStackEntry ->
                    val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                    UserLiveStreamScreen(
                        eventoId = eventoId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
    // Pantalla completa del reproductor: va encima de todo, incluida la barra inferior.
    NowPlayingScreen(
        sheet = playerSheet,
        screenHeightPx = screenHeightPx,
        onOpenConcert = { eventoId -> navController.navigate("event/$eventoId") { launchSingleTop = true } },
        onBuyTickets = { eventoId ->
            ticketsEventId = eventoId
            navController.navigate(BottomNavItem.Tickets.route) { launchSingleTop = true }
        }
    )
    }
}
