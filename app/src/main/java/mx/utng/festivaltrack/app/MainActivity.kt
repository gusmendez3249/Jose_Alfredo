package mx.utng.festivaltrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.utng.festivaltrack.app.ui.screens.LoginScreen
import mx.utng.festivaltrack.app.ui.screens.RegisterScreen
import mx.utng.festivaltrack.app.ui.screens.MainScreen
import mx.utng.festivaltrack.app.ui.theme.FestivalTrackTheme

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel

/**
 * Actividad principal y única de la aplicación FestivalTrack.
 *
 * Responsabilidades:
 * - Inicializar el contenedor de dependencias (AppContainer) desde [FestivalTrackApplication].
 * - Proveer ViewModels compartidos entre pantallas usando `viewModel()`.
 * - Configurar el NavHost de Jetpack Compose Navigation con todas las rutas de la app.
 * - Gestionar la autenticación: según el rol del usuario (USER/ADMIN), navega
 *   al flujo correspondiente.
 *
 * Rutas disponibles:
 * - `login`               → [LoginScreen]
 * - `register`            → [RegisterScreen]
 * - `main`                → [MainScreen] (usuarios normales)
 * - `checkout/{total}/{count}` → [CheckoutScreen]
 * - `success`             → [TicketSuccessScreen]
 * - `live/{eventoId}`     → [UserLiveStreamScreen]
 * - `admin_dashboard`     → [AdminMainScreen] (administradores)
 * - `admin_create_event`  → [AdminCreateEventScreen]
 * - `admin_live_stream`   → [AdminLiveStreamScreen]
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtiene el contenedor de dependencias de la clase Application global
        val appContainer = (application as FestivalTrackApplication).container

        setContent {
            // ViewModel de eventos: comparte datos entre pantallas del usuario
            val eventosViewModel: EventosViewModel = viewModel(
                factory = EventosViewModel.provideFactory(appContainer.festivalRepository)
            )
            // ViewModel de gestión admin: CRUD de eventos para el panel administrativo
            val adminManageViewModel: AdminManageViewModel = viewModel(
                factory = AdminManageViewModel.provideFactory(appContainer.festivalRepository)
            )
            
            FestivalTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Grafo de navegación completo de la aplicación.
                    // La pantalla de inicio es "login" para garantizar que el usuario
                    // siempre pase por el flujo de autenticación al abrir la app.
                    NavHost(navController = navController, startDestination = "login") {

                        // ─── PANTALLA DE LOGIN ───────────────────────────────────────────
                        composable("login") {
                            LoginScreen(
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                },
                                // Navegación post-login para usuarios normales (rol USER)
                                onNavigateToDashboard = { 
                                    navController.navigate("main") {
                                        // Elimina "login" del backstack para que el botón Atrás no regrese
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                // Navegación post-login para administradores (rol ADMIN)
                                onNavigateToAdmin = {
                                    navController.navigate("admin_dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ─── PANTALLA DE REGISTRO ────────────────────────────────────────
                        composable("register") {
                            RegisterScreen(
                                onNavigateToLogin = { navController.popBackStack() },
                                onNavigateToDashboard = { 
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToAdmin = {
                                    navController.navigate("admin_dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ─── PANTALLA PRINCIPAL DEL USUARIO ─────────────────────────────
                        composable("main") {
                            mx.utng.festivaltrack.app.ui.screens.MainScreen(
                                eventosViewModel = eventosViewModel,
                                onNavigateToCheckout = { total, count ->
                                    // Pasa el total en pesos y cantidad de boletos como argumentos de ruta
                                    navController.navigate("checkout/$total/$count")
                                },
                                onLogout = {
                                    // Borra el token JWT antes de navegar para invalidar la sesión
                                    mx.utng.festivaltrack.app.data.TokenManager(this@MainActivity).clear()
                                    navController.navigate("login") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ─── CHECKOUT (COMPRA DE BOLETOS) ────────────────────────────────
                        // Recibe argumentos: total (precio total en MXN) y count (cantidad)
                        composable("checkout/{total}/{count}") { backStackEntry ->
                            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
                            val count = backStackEntry.arguments?.getString("count")?.toIntOrNull() ?: 0
                            mx.utng.festivaltrack.app.ui.screens.CheckoutScreen(
                                totalPrice = total,
                                totalTickets = count,
                                onNavigateBack = { navController.popBackStack() },
                                onPaymentSuccess = {
                                    navController.navigate("success") {
                                        popUpTo("main") { inclusive = false }
                                    }
                                }
                            )
                        }

                        // ─── CONFIRMACIÓN DE COMPRA EXITOSA ─────────────────────────────
                        composable("success") {
                            mx.utng.festivaltrack.app.ui.screens.TicketSuccessScreen(
                                onNavigateHome = {
                                    navController.popBackStack("main", inclusive = false)
                                }
                            )
                        }

                        // ─── TRANSMISIÓN EN VIVO (USUARIO) ──────────────────────────────
                        // Recibe eventoId para cargar el stream y chat del evento correcto
                        composable("live/{eventoId}") { backStackEntry ->
                            val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                            mx.utng.festivaltrack.app.ui.screens.UserLiveStreamScreen(
                                eventoId = eventoId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        // ─── RUTAS DEL ADMINISTRADOR ─────────────────────────────────────

                        // Panel principal del administrador con BottomNav
                        composable("admin_dashboard") {
                            mx.utng.festivaltrack.app.ui.screens.AdminMainScreen(
                                adminManageViewModel = adminManageViewModel,
                                onNavigateToCreateEvent = {
                                    navController.navigate("admin_create_event")
                                },
                                onEditEvent = { evento ->
                                    navController.navigate("admin_edit_event/${evento.id}")
                                },
                                onNavigateToLivePanel = {
                                    navController.navigate("admin_live_stream")
                                },
                                onLogout = {
                                    // Borra token y limpia TODA la pila de navegación (popUpTo(0))
                                    // para que el botón Atrás no regrese al panel admin tras cerrar sesión
                                    mx.utng.festivaltrack.app.data.TokenManager(this@MainActivity).clear()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Formulario de creación de nuevos eventos
                        composable("admin_create_event") {
                            mx.utng.festivaltrack.app.ui.screens.AdminCreateEventScreen(
                                viewModel = adminManageViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Formulario de edición de un evento existente
                        composable("admin_edit_event/{id}") { backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("id") ?: ""
                            val eventosList = adminManageViewModel.eventos.collectAsState().value
                            val eventToEdit = eventosList.find { it.id == eventId }

                            mx.utng.festivaltrack.app.ui.screens.AdminCreateEventScreen(
                                eventId = eventId,
                                initialTitle = eventToEdit?.nombre ?: "",
                                initialDate = eventToEdit?.fechaHora ?: "",
                                initialLocation = eventToEdit?.ubicacion ?: "",
                                initialPrice = "4500",
                                viewModel = adminManageViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Panel de transmisión en vivo del administrador (con cámara)
                        composable("admin_live_stream") {
                            mx.utng.festivaltrack.app.ui.screens.AdminLiveStreamScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
