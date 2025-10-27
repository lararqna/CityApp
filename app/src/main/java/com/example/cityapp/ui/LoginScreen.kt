package com.example.cityapp.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavController, auth: FirebaseAuth?) {
    Text("Dit is het Login Scherm")
}
    