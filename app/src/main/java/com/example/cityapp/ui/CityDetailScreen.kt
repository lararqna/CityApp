package com.example.cityapp.ui

import com.example.cityapp.models.City
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.cityapp.R
import com.example.cityapp.models.Location
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun CityDetailScreen(city: City, userLocation: GeoPoint, onBack: () -> Unit) {

    val context = LocalContext.current
    val db = Firebase.firestore

    var locations by remember { mutableStateOf<List<Location>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var showAddLocation by remember { mutableStateOf(false) }

    var mapReady by remember { mutableStateOf(false) }
    var mapKey by remember { mutableStateOf(0) }

    val mapView = remember(mapKey) {
        org.osmdroid.config.Configuration.getInstance()
            .load(context, context.getSharedPreferences("osm", 0))

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(city.latitude, city.longitude))
            setMultiTouchControls(true)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isTilesScaledToDpi = true
        }
    }

    LaunchedEffect(showAddLocation) {
        val cityId = city.id ?: return@LaunchedEffect

        if (!showAddLocation) {
            db.collection("cities")
                .document(cityId)
                .collection("locations")
                .get()
                .addOnSuccessListener { result ->
                    locations = result.documents.mapNotNull { doc ->

                        val data = doc.data ?: return@mapNotNull null

                        val name = data["name"] as? String ?: ""
                        val imageUrl = data["imageUrl"] as? String ?: ""
                        val latitude = when (val lat = data["latitude"]) {
                            is Double -> lat
                            is Long -> lat.toDouble()
                            is Int -> lat.toDouble()
                            is String -> lat.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        val longitude = when (val lon = data["longitude"]) {
                            is Double -> lon
                            is Long -> lon.toDouble()
                            is Int -> lon.toDouble()
                            is String -> lon.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        val categories: List<String> =
                            when {
                                data["categories"] is List<*> ->
                                    (data["categories"] as List<*>).filterIsInstance<String>()

                                data["category"] is String ->
                                    listOf(data["category"] as String)

                                else -> emptyList()
                            }

                        Location(
                            id = doc.id,
                            name = name,
                            categories = categories,
                            imageUrl = imageUrl,
                            address = data["address"] as? String,
                            latitude = latitude,
                            longitude = longitude,
                            initialReview = data["initialReview"] as? String,
                            initialRating = (data["initialRating"] as? Long)?.toInt(),
                            initialUsername = data["initialUsername"] as? String
                        )
                    }
                }
        }
    }

    LaunchedEffect(locations, mapReady, mapKey) {
        if (!mapReady) return@LaunchedEffect
        if (mapView.handler == null) return@LaunchedEffect

        mapView.overlays.clear()

        mapView.overlays.add(
            Marker(mapView).apply {
                position = GeoPoint(city.latitude, city.longitude)
                icon = context.getDrawable(R.drawable.ic_location_pin)
                title = city.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
        )

        locations.forEach { loc ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(loc.latitude, loc.longitude)
                    icon = context.getDrawable(R.drawable.ic_location_pin)
                    title = loc.name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
        }

        mapView.invalidate()
    }

    if (selectedLocation != null) {
        LocationDetailScreen(
            location = selectedLocation!!,
            userLocation = userLocation,
            onBack = {
                selectedLocation = null
                mapReady = false
                mapKey++
            }
        )
        return
    }


    if (showAddLocation) {
        AddLocationScreen(
            city = city,
            onCancel = {
                showAddLocation = false
                mapReady = false
                mapKey++
            }
        )
        return
    }

    var search by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf("Alles")) }

    val categories = remember(locations) {
        listOf("Alles") + locations.flatMap { it.categories }.distinct().sorted()
    }

    val filteredLocations = locations.filter { loc ->
        val searchOK = loc.name.contains(search, ignoreCase = true)
        val catOK =
            selectedCategories.contains("Alles") ||
                    loc.categories.any { selectedCategories.contains(it) }

        searchOK && catOK
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLocation = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(bottom = 90.dp, end = 12.dp)
                    .zIndex(3f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Location", tint = Color.White)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Ontdek ${city.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            TextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Zoek naar locaties") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF4F5F6),
                    unfocusedContainerColor = Color(0xFFF4F5F6),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )

            LazyRow(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .heightIn(min = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategories.contains(category),
                        onClick = {
                            val currentSelection = selectedCategories.toMutableSet()

                            if (category == "Alles") {
                                currentSelection.clear()
                                currentSelection.add("Alles")
                            } else {
                                currentSelection.remove("Alles")
                                if (currentSelection.contains(category))
                                    currentSelection.remove(category)
                                else
                                    currentSelection.add(category)

                                if (currentSelection.isEmpty())
                                    currentSelection.add("Alles")
                            }

                            selectedCategories = currentSelection
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        mapReady = true
                        mapView
                    },
                    onRelease = {
                        it.onPause()
                        it.onDetach()
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(filteredLocations) { loc ->

                    val distance = calculateDistanceKm(
                        userLocation,
                        GeoPoint(loc.latitude, loc.longitude)
                    )

                    Card(
                        onClick = { selectedLocation = loc },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {

                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Image(
                                painter = rememberAsyncImagePainter(loc.imageUrl),
                                contentDescription = loc.name,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(loc.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${loc.categories.joinToString(" • ")} • $distance km",
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

private fun Double.toRadians() = this * Math.PI / 180
