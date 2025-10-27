package com.example.cityapp.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun RegisterScreen(
    navController: NavController,
    auth: FirebaseAuth?
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

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

                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(80.dp))

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
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Voornaam") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(5.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Achternaam") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(5.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        auth?.createUserWithEmailAndPassword(email, password)
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("RegisterScreen", "Auth gebruiker succesvol aangemaakt.")
                                    val firebaseUser = task.result?.user
                                    if (firebaseUser != null) {
                                        val userMap = hashMapOf(
                                            "firstName" to firstName.trim(),
                                            "lastName" to lastName.trim(),
                                            "email" to email.trim().lowercase()
                                        )

                                        val db = Firebase.firestore
                                        db.collection("users").document(firebaseUser.uid)
                                            .set(userMap)
                                            .addOnSuccessListener {
                                                Log.d("RegisterScreen", "Firestore document succesvol aangemaakt voor UID: ${firebaseUser.uid}")
                                                isLoading = false
                                                Toast.makeText(context, "Registratie gelukt!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("home") {
                                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.w("RegisterScreen", "Fout bij aanmaken Firestore document", e)
                                                isLoading = false
                                                Toast.makeText(context, "Fout bij opslaan gegevens: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    }
                                } else {
                                    isLoading = false
                                    val exception = task.exception?.message ?: "Onbekende fout bij registratie"
                                    Log.e("RegisterScreen", "Auth registratie mislukt: $exception")
                                    Toast.makeText(context, "Registratie mislukt: $exception", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Vul alle velden in a.u.b.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = "Maak account aan",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
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
