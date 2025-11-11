package com.example.cityapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun CityScreen(modifier: Modifier = Modifier) {
    val userLocation = org.osmdroid.util.GeoPoint(51.2303, 4.4161)
    var selectedCity by remember { mutableStateOf<City?>(null) }

    var searchQuery by remember { mutableStateOf("") }

    val db = Firebase.firestore
    val defaultCities = sampleCities()
    var cities by remember { mutableStateOf(defaultCities) }

    LaunchedEffect(Unit) {
        db.collection("cities")
            .get()
            .addOnSuccessListener { result ->
                val firebaseCities = result.documents.mapNotNull { it.toObject(City::class.java) }
                cities = (defaultCities + firebaseCities).distinctBy { it.name }
            }
    }
    val filteredCities = cities.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }
    if (selectedCity != null) {
        CityDetailScreen(city = selectedCity!!, userLocation = userLocation) {
            selectedCity = null
        }
    } else {
        Scaffold(
            modifier = modifier,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { /* TODO: Deze knop doet nog niets */ },
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

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCities) { city ->
                        val distanceText = "${calculateDistanceKm(userLocation, org.osmdroid.util.GeoPoint(city.latitude, city.longitude))} km"
                        CityCard(city, distanceText) {
                            selectedCity = city
                        }
                    }
                }
            }
        }
    }
}


fun sampleCities() = listOf(
    City("Antwerpen", "https://cdn.pixabay.com/photo/2016/01/19/17/37/antwerp-1151143_1280.jpg", 51.2194, 4.4025),
    City("Leuven", "https://cdn.pixabay.com/photo/2017/03/14/08/09/leuven-2148647_1280.jpg", 50.8798, 4.7005),
    City("Amsterdam", "https://cdn.pixabay.com/photo/2016/11/29/04/17/amsterdam-1866781_1280.jpg", 52.3676, 4.9041)
)

