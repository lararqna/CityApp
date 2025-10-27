package com.example.cityapp.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RegisterScreen(
    navController: NavController,
    auth: FirebaseAuth?
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Maak een account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mailadres") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Wachtwoord") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    auth?.createUserWithEmailAndPassword(email, password)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("RegisterScreen", "Gebruiker succesvol geregistreerd.")
                                Toast.makeText(context, "Registratie gelukt!", Toast.LENGTH_SHORT).show()
                                navController.navigate("home") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            } else {
                                val exception = task.exception?.message ?: "Onbekende fout"
                                Log.e("RegisterScreen", "Registratie mislukt: $exception")
                                Toast.makeText(context, "Registratie mislukt: $exception", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Vul alle velden in.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registreren")
        }
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text("Heb je al een account? Log in")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    val navController = rememberNavController()
    RegisterScreen(
        navController = navController,
        auth = null
    )
}


