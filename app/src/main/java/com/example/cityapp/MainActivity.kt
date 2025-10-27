package com.example.cityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cityapp.ui.LoginScreen // Maak dit bestand later aan
import com.example.cityapp.ui.RegisterScreen
import com.example.cityapp.ui.HomeScreen // Maak dit bestand later aan
import com.example.cityapp.ui.theme.CityAppTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityAppTheme {
                // Roep hier je hoofdnavigatie aan
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    // 1. Maak een NavController om de navigatie te beheren
    val navController = rememberNavController()
    // 2. Haal de Firebase authenticatie instantie op
    val auth = Firebase.auth

    // 3. Definieer de NavHost, die de schermen toont
    // startDestination bepaalt welk scherm als eerste zichtbaar is.
    NavHost(navController = navController, startDestination = "register") {

        // Definieer de "register" route
        composable("register") {
            // Toon het RegisterScreen en geef de benodigde parameters mee
            RegisterScreen(navController = navController, auth = auth)
        }

        // Definieer de "login" route
        composable("login") {
            // Hier komt je LoginScreen. Je moet dit bestand nog aanmaken.
            LoginScreen(navController = navController, auth = auth)
        }

        // Definieer de "home" route (voor na het inloggen)
        composable("home") {
            // Hier komt je hoofdscherm van de app.
            HomeScreen(navController = navController)
        }
    }
}
