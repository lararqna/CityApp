package com.example.cityapp.ui

import com.example.cityapp.models.City
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cityapp.ServiceLocator
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { ServiceLocator.provideLocationRepository(context) }
    val scope = rememberCoroutineScope()

    val cities by repo.getCities().collectAsState(initial = emptyList())

    val userLocation = GeoPoint(51.2303, 4.4161)
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var showAddCityForm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCities = cities.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    when {
        showAddCityForm -> {
            AddCityScreen { showAddCityForm = false }
        }

        selectedCity != null -> {
            CityDetailScreen(
                city = selectedCity!!,
                userLocation = userLocation
            ) {
                selectedCity = null
            }
        }

        else -> {
            Scaffold(
                modifier = modifier,

                /** 🔁 TOP BAR MET REFRESH **/
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Steden",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        repo.refreshAllCitiesAndLocations()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh"
                                )
                            }
                        }
                    )
                },

                /** ➕ ADD BUTTON **/
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

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Zoek een stad...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Zoeken"
                            )
                        },
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
                            val distanceText =
                                "${calculateDistanceKm(userLocation, GeoPoint(city.latitude, city.longitude))} km"

                            CityCard(
                                city = city,
                                distanceText = distanceText
                            ) {
                                selectedCity = city
                            }
                        }
                    }
                }
            }
        }
    }
}
