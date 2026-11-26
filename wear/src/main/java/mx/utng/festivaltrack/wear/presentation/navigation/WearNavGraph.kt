package mx.utng.festivaltrack.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import mx.utng.festivaltrack.wear.presentation.screens.AgendaScreen
import mx.utng.festivaltrack.wear.presentation.screens.ConciertoScreen
import mx.utng.festivaltrack.wear.presentation.screens.MapaRutaScreen

/**
 * Rutas del reloj. El flujo es corto a propósito: Agenda → Concierto → Mapa
 * (dos toques para llegar a la ruta). Atrás se hace deslizando hacia la derecha.
 */
sealed class WearScreen(val route: String) {
    /** Agenda del festival agrupada por día (pantalla de inicio). */
    object Agenda : WearScreen("agenda")

    /** Detalle de un concierto: cuándo, dónde y a qué distancia estás. */
    object Concierto : WearScreen("concierto/{eventoId}") {
        fun createRoute(eventoId: String) = "concierto/$eventoId"
    }

    /** Mapa con la ruta a pie hasta el concierto. */
    object Mapa : WearScreen("mapa/{eventoId}") {
        fun createRoute(eventoId: String) = "mapa/$eventoId"
    }
}

/**
 * Navegación del reloj con [SwipeDismissableNavHost] (deslizar para regresar, estándar de Wear OS).
 *
 * @param conciertoInicial Concierto a abrir al iniciar (al tocar el aviso de 15 minutos).
 */
@Composable
fun WearNavGraph(conciertoInicial: String? = null) {
    val navController = rememberSwipeDismissableNavController()

    LaunchedEffect(conciertoInicial) {
        conciertoInicial?.let { navController.navigate(WearScreen.Concierto.createRoute(it)) }
    }

    SwipeDismissableNavHost(navController = navController, startDestination = WearScreen.Agenda.route) {
        composable(WearScreen.Agenda.route) {
            AgendaScreen(onConcierto = { id -> navController.navigate(WearScreen.Concierto.createRoute(id)) })
        }
        composable(WearScreen.Concierto.route) { backStack ->
            val eventoId = backStack.arguments?.getString("eventoId") ?: return@composable
            ConciertoScreen(
                eventoId = eventoId,
                onComoLlegar = { navController.navigate(WearScreen.Mapa.createRoute(eventoId)) }
            )
        }
        composable(WearScreen.Mapa.route) { backStack ->
            val eventoId = backStack.arguments?.getString("eventoId") ?: return@composable
            MapaRutaScreen(eventoId = eventoId, onBack = { navController.popBackStack() })
        }
    }
}
