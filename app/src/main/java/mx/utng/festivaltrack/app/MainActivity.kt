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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FestivalTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToDashboard = { 
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onNavigateToLogin = { navController.popBackStack() },
                                onNavigateToDashboard = { 
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("main") {
                            MainScreen(
                                onNavigateToCheckout = { total, count ->
                                    navController.navigate("checkout/$total/$count")
                                }
                            )
                        }
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
                        composable("success") {
                            mx.utng.festivaltrack.app.ui.screens.TicketSuccessScreen(
                                onNavigateHome = {
                                    navController.popBackStack("main", inclusive = false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
