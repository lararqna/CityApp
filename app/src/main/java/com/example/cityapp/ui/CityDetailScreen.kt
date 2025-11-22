package com.example.cityapp.ui

import City
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.forEachGesture
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.cityapp.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.*


data class Attraction(
    val id: String = "",
    val name: String,
    val category: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double
)

data class Location(
    val name: String = "",
    val category: String = "",
    val imageUrl: String? = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@Composable
fun CityDetailScreen(city: City, userLocation: GeoPoint, onBack: () -> Unit) {
    val context = LocalContext.current
    val mapView = remember {
        org.osmdroid.config.Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(city.latitude, city.longitude))
            setMultiTouchControls(true)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isTilesScaledToDpi = true
            overlays.clear()

            val cityMarker = Marker(this).apply {
                position = GeoPoint(city.latitude, city.longitude)
                icon = context.getDrawable(R.drawable.ic_location_pin)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            overlays.add(cityMarker)
        }
    }


    var attractions by remember { mutableStateOf<List<Attraction>>(emptyList()) }
    val db = Firebase.firestore


    LaunchedEffect(city.id) {
        if (!city.id.isNullOrEmpty()) {
            db.collection("cities")
                .document(city.id)
                .collection("locations")
                .get()
                .addOnSuccessListener { result ->
                    attractions = result.documents.mapNotNull { doc ->
                        doc.toObject(Location::class.java)?.let { loc ->
                            Attraction(
                                id = doc.id,
                                name = loc.name,
                                category = loc.category,
                                imageUrl = loc.imageUrl ?: "",
                                latitude = loc.latitude,
                                longitude = loc.longitude
                            )
                        }
                    }
                }
        }
    }

    LaunchedEffect(attractions) {
        val cityGeoPoint = GeoPoint(city.latitude, city.longitude)
        mapView.overlays.removeAll { it is Marker && it.position != cityGeoPoint }

        for (attr in attractions) {
            val marker = Marker(mapView).apply {
                position = GeoPoint(attr.latitude, attr.longitude)
                title = attr.name
                icon = context.getDrawable(R.drawable.ic_location_pin)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    var search by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf("Alles")) }

    val categories = remember(attractions) {
        listOf("Alles") + attractions.map { it.category }.distinct().sorted()
    }

    val filteredAttractions = attractions.filter { attraction ->
        val searchMatch = attraction.name.contains(search, ignoreCase = true)
        val categoryMatch = selectedCategories.contains("Alles") || selectedCategories.contains(attraction.category)
        searchMatch && categoryMatch
    }


    var selectedAttraction by remember { mutableStateOf<Attraction?>(null) }
    var showAddLocation by remember { mutableStateOf(false) }

    if (selectedAttraction != null) {
        LocationDetailScreen(
            attraction = selectedAttraction!!,
            userLocation = userLocation,
            onBack = { selectedAttraction = null }
        )
        return
    }

    if (showAddLocation) {
        AddLocationScreen(
            city = city,
            onCancel = { showAddLocation = false }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddLocation = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 90.dp, end = 12.dp)
                        .zIndex(3f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Location", tint = Color.White)
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
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
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
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
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    if (currentSelection.contains(category)) {
                                        currentSelection.remove(category)
                                    } else {
                                        currentSelection.add(category)
                                    }
                                    if (currentSelection.isEmpty()){
                                        currentSelection.add("Alles")
                                    }
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
                        factory = { mapView },
                    )
                }

                LazyColumn(Modifier.padding(16.dp)) {
                    items(filteredAttractions) { attr ->
                        val distance = calculateDistanceKm(userLocation, GeoPoint(attr.latitude, attr.longitude))
                        Card(
                            onClick = { selectedAttraction = attr },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = rememberAsyncImagePainter(attr.imageUrl),
                                    contentDescription = attr.name,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(attr.name, fontWeight = FontWeight.Bold)
                                    Text("${attr.category} • $distance km", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun Double.toRadians() = this * Math.PI / 180
