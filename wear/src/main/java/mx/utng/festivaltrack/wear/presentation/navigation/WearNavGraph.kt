package mx.utng.festivaltrack.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import mx.utng.festivaltrack.wear.presentation.screens.*

sealed class WearScreen(val route: String) {
    object Splash       : WearScreen("splash")
    object WatchFace    : WearScreen("watch_face")
    object Proximos     : WearScreen("proximos")
    object Alerta       : WearScreen("alerta/{eventoId}") {
        fun createRoute(eventoId: String) = "alerta/$eventoId"
    }
    object NavEscenario : WearScreen("nav_escenario/{eventoId}") {
        fun createRoute(eventoId: String) = "nav_escenario/$eventoId"
    }
    object MapaAcceso   : WearScreen("mapa_acceso/{eventoId}") {
        fun createRoute(eventoId: String) = "mapa_acceso/$eventoId"
    }
}

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
