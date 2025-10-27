package com.example.cityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cityapp.ui.LoginScreen
import com.example.cityapp.ui.RegisterScreen
import com.example.cityapp.ui.HomeScreen
import com.example.cityapp.ui.theme.CityAppTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = Firebase.auth
    NavHost(navController = navController, startDestination = "register") {
        composable("register") {
            RegisterScreen(navController = navController, auth = auth)
        }

        composable("login") {
            LoginScreen(navController = navController, auth = auth)
        }
        composable("home") {
            HomeScreen(navController = navController, auth = auth)
        }
    }
}
