package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.viewmodels.AuthViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }
    
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val role = (authState as AuthState.Success).role
            if (role == "ADMINISTRADOR") {
                onNavigateToAdmin()
            } else {
                onNavigateToDashboard()
            }
        }
    }

    val scrollState = rememberScrollState()
    val fieldColor = Color(0xFF1E2720)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "José Alfredo Jiménez",
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryGold,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Crea tu Cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "Sé parte de la leyenda.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("NOMBRE COMPLETO") },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = PrimaryGold,
                unfocusedLabelColor = Color.Gray,
                containerColor = fieldColor
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("CORREO ELECTRÓNICO") },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = PrimaryGold,
                unfocusedLabelColor = Color.Gray,
                containerColor = fieldColor
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("CONTRASEÑA") },
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = PrimaryGold,
                unfocusedLabelColor = Color.Gray,
                containerColor = fieldColor
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )
        
        Text(
            text = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
        )

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.register(name.trim(), email.trim(), password)
            },
            enabled = authState !is AuthState.Loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGold,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("REGISTRARSE AHORA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿Ya tienes una cuenta?", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Inicia Sesión",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
