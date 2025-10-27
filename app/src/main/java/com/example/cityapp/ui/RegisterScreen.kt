package com.example.cityapp.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }

    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }

    val goToFirstPage = { currentPage = 1 }

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

            Crossfade(targetState = currentPage, label = "ToggleToBackArrow") { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(43.dp)
                ) {
                    if (page == 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    AppTheme.extendedColors.toggleBackground,
                                    RoundedCornerShape(5.dp)
                                ),
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
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(5.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Registreren",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        TextButton(
                            onClick = goToFirstPage,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Terug naar vorige stap"
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Terug")
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    val enter = slideInHorizontally { width -> width } + fadeIn()
                    val exit = slideOutHorizontally { width -> -width } + fadeOut()
                    enter togetherWith exit
                }, label = "page_animation"
            ) { page ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(24.dp))
                    when (page) {
                        1 -> AccountCreationPage(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            onNextClicked = {

                                val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

                                if (isEmailValid && password.isNotBlank()) {
                                    currentPage = 2
                                } else if (!isEmailValid) {
                                    Toast.makeText(context, "Voer een geldig e-mailadres in.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Vul een wachtwoord in.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        2 -> ProfileDetailsPage(
                            firstName = firstName,
                            onFirstNameChange = { firstName = it },
                            lastName = lastName,
                            onLastNameChange = { lastName = it },
                            birthDate = birthDate,
                            onBirthDateChange = { birthDate = it },
                            isLoading = isLoading,
                            onFinishClicked = {

                                if (firstName.isNotBlank() && lastName.isNotBlank()) {
                                    isLoading = true

                                    auth?.createUserWithEmailAndPassword(email, password)
                                        ?.addOnCompleteListener { authTask ->
                                            if (authTask.isSuccessful) {
                                                Log.d("RegisterScreen", "Auth gebruiker succesvol aangemaakt.")
                                                val firebaseUser = authTask.result?.user

                                                val userMap = hashMapOf(
                                                    "firstName" to firstName.trim(),
                                                    "lastName" to lastName.trim(),
                                                    "email" to email.trim().lowercase(),
                                                    "birthDate" to birthDate.trim()
                                                )


                                                firebaseUser?.let { user ->
                                                    Firebase.firestore.collection("users").document(user.uid)
                                                        .set(userMap)
                                                        .addOnSuccessListener {
                                                            Log.d("RegisterScreen", "Firestore document succesvol aangemaakt.")
                                                            Toast.makeText(context, "Welkom, $firstName!", Toast.LENGTH_SHORT).show()
                                                            navController.navigate("home") { popUpTo(navController.graph.startDestinationId) { inclusive = true } }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.w("RegisterScreen", "Fout bij opslaan Firestore data", e)
                                                            Toast.makeText(context, "Fout bij opslaan profiel: ${e.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                        .addOnCompleteListener { isLoading = false } // Stop laden na Firestore
                                                } ?: run {
                                                    isLoading = false
                                                    Toast.makeText(context, "Onbekende fout opgetreden.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                isLoading = false
                                                val error = authTask.exception?.message ?: "Onbekende fout bij registratie"
                                                Log.e("RegisterScreen", "Auth registratie mislukt: $error")
                                                Toast.makeText(context, "Registratie mislukt: $error", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "Voornaam en achternaam zijn verplicht.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}


@Composable
fun AccountCreationPage(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    onNextClicked: () -> Unit
) {
    var isPasswordFocused by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("E-mailadres") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(5.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Wachtwoord") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,


            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isPasswordFocused = focusState.isFocused
                },
            shape = RoundedCornerShape(5.dp)
        )

        if (isPasswordFocused) {
            Spacer(modifier = Modifier.height(16.dp))
            PasswordRequirementHint()
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNextClicked,
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Volgende", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PasswordRequirementHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Een sterk wachtwoord bevat letters, cijfers en symbolen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun ProfileDetailsPage(
    firstName: String, onFirstNameChange: (String) -> Unit,
    lastName: String, onLastNameChange: (String) -> Unit,
    birthDate: String, onBirthDateChange: (String) -> Unit,
    isLoading: Boolean,
    onFinishClicked: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Bijna klaar! Vertel ons meer over jezelf.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(value = firstName, onValueChange = onFirstNameChange, label = { Text("Voornaam*") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = lastName, onValueChange = onLastNameChange, label = { Text("Achternaam*") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = birthDate, onValueChange = onBirthDateChange, label = { Text("Geboortedatum (optioneel)") }, placeholder = { Text("dd-mm-jjjj") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onFinishClicked,
            enabled = !isLoading,
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
            } else {
                Text("Registratie voltooien", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
