package com.example.cityapp.ui

import City
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.util.GeoPoint

@Composable
fun CityScreen(modifier: Modifier = Modifier) {
    val userLocation = GeoPoint(51.2303, 4.4161)
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var showAddCityForm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val db = Firebase.firestore
    var cities by remember { mutableStateOf<List<City>>(emptyList()) }

    val refreshKey by rememberUpdatedState(showAddCityForm)

    LaunchedEffect(refreshKey) {
        if (!showAddCityForm) {
            db.collection("cities")
                .get()
                .addOnSuccessListener { result ->
                    cities = result.documents.map { doc ->
                        City(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0
                        )
                    }
                }
        }
    }

    val filteredCities = cities.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    when {
        showAddCityForm -> {
            AddCityScreen { showAddCityForm = false }
        }

        selectedCity != null -> {
            CityDetailScreen(city = selectedCity!!, userLocation = userLocation) {
                selectedCity = null
            }
        }

        else -> {
            Scaffold(
                modifier = modifier,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddCityForm = true },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Stad Toevoegen")
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Steden",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                    )

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Zoek een stad...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Zoeken") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF4F5F6),
                            unfocusedContainerColor = Color(0xFFF4F5F6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredCities) { city ->
                            val distanceText = "${calculateDistanceKm(userLocation, GeoPoint(city.latitude, city.longitude))} km"
                            CityCard(city, distanceText) {
                                selectedCity = city
                            }
                        }
                    }
                }
            }
        }
    }
}
