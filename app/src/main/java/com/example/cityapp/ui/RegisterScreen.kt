package com.example.cityapp.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.cityapp.ui.theme.AppTheme
import com.example.cityapp.ui.theme.CityAppTheme
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RegisterScreen(
    navController: NavController,
    auth: FirebaseAuth?
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Place Icon",
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welkom bij\nStad Ontdekker",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Registreer om je avontuur te beginnen.",
                color = AppTheme.extendedColors.inactiveText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(30.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(43.dp)
                    .background(AppTheme.extendedColors.toggleBackground, RoundedCornerShape(5.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Inloggen", color = AppTheme.extendedColors.inactiveText)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Registreren",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mailadres") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(5.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Wachtwoord") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(5.dp)
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
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Maak account aan",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=393dp,height=852dp,dpi=480")
@Composable
fun RegisterScreenPreview() {
    CityAppTheme {
        RegisterScreen(
            navController = rememberNavController(),
            auth = null
        )
    }
}
