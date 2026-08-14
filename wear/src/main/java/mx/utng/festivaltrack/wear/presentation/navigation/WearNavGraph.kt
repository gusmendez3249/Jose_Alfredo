package mx.utng.festivaltrack.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import mx.utng.festivaltrack.wear.presentation.screens.*

/**
 * Rutas de navegación selladas para Wear OS.
 * @property route El identificador de la ruta en la navegación.
 */
sealed class WearScreen(val route: String) {
    /** Pantalla inicial tipo Splash. */
    object Splash       : WearScreen("splash")
    /** Pantalla que simula una carátula o menú principal. */
    object WatchFace    : WearScreen("watch_face")
    /** Pantalla de listado de próximos eventos. */
    object Proximos     : WearScreen("proximos")
    /** Pantalla de listado completo de eventos. */
    object ProgramaCompleto : WearScreen("programa_completo")
    /**
     * Pantalla de alerta de un evento próximo.
     * @property eventoId ID del evento.
     */
    object Alerta       : WearScreen("alerta/{eventoId}") {
        fun createRoute(eventoId: String) = "alerta/$eventoId"
    }
    /**
     * Pantalla de opciones de navegación a un escenario.
     * @property eventoId ID del evento.
     */
    object NavEscenario : WearScreen("nav_escenario/{eventoId}") {
        fun createRoute(eventoId: String) = "nav_escenario/$eventoId"
    }
    /**
     * Pantalla del mapa interactivo para acceder al evento.
     * @property eventoId ID del evento.
     */
    object MapaAcceso   : WearScreen("mapa_acceso/{eventoId}") {
        fun createRoute(eventoId: String) = "mapa_acceso/$eventoId"
    }
}

/**
 * Gráfico de navegación principal para la aplicación en Wear OS.
 * Utiliza [SwipeDismissableNavHost] que es el estándar en relojes para deslizar hacia atrás.
 */
@Composable
fun WearNavGraph() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearScreen.Splash.route
    ) {
        composable(WearScreen.Splash.route) {
            SplashScreen(onSplashFinished = {
                navController.navigate(WearScreen.WatchFace.route) {
                    popUpTo(WearScreen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(WearScreen.WatchFace.route) {
            WatchFaceScreen(onTap = { navController.navigate(WearScreen.Proximos.route) })
        }
        composable(WearScreen.Proximos.route) {
            ProximosScreen(
                onEventoClick = { id -> navController.navigate(WearScreen.Alerta.createRoute(id)) },
                onProgramaCompletoClick = { navController.navigate(WearScreen.ProgramaCompleto.route) }
            )
        }
        composable(WearScreen.ProgramaCompleto.route) {
            ProgramaCompletoScreen(
                onEventoClick = { id -> navController.navigate(WearScreen.Alerta.createRoute(id)) }
            )
        }
        composable(WearScreen.Alerta.route) { backStack ->
            val eventoId = backStack.arguments?.getString("eventoId") ?: return@composable
            AlertaScreen(
                eventoId = eventoId,
                onVerMapa = { navController.navigate(WearScreen.NavEscenario.createRoute(eventoId)) }
            )
        }
        composable(WearScreen.NavEscenario.route) { backStack ->
            val eventoId = backStack.arguments?.getString("eventoId") ?: return@composable
            NavEscenarioScreen(
                eventoId = eventoId,
                onLlegarAhora = { navController.navigate(WearScreen.MapaAcceso.createRoute(eventoId)) }
            )
        }
        composable(WearScreen.MapaAcceso.route) { backStack ->
            val eventoId = backStack.arguments?.getString("eventoId") ?: return@composable
            MapaAccesoScreen(
                eventoId = eventoId,
                onBack = { navController.popBackStack(WearScreen.WatchFace.route, false) }
            )
        }
    }
}
