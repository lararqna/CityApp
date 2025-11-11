package com.example.cityapp.ui

import City
import android.annotation.SuppressLint
import com.example.cityapp.R
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.*

data class Attraction(
    val name: String,
    val category: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val userLocation = GeoPoint(51.2303, 4.4161)

    var cities by remember { mutableStateOf<List<City>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = cities.filter { it.name.contains(searchQuery, ignoreCase = true) }

    var expanded by remember { mutableStateOf(true) }
    val sheetHeight by animateDpAsState(targetValue = if (expanded) 550.dp else 90.dp)
    var selectedCity by remember { mutableStateOf<City?>(null) }

    if (selectedCity != null) {
        CityDetailScreen(city = selectedCity!!, userLocation = userLocation) {
            selectedCity = null
        }
    }
    else {
        val mapView = remember {
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(15.0)
                val centeredPoint = GeoPoint(userLocation.latitude - 0.02, userLocation.longitude)
                controller.setCenter(centeredPoint)
                setMultiTouchControls(true)
                overlays.clear()

                val userMarker = Marker(this).apply {
                    position = userLocation
                    icon = context.getDrawable(R.drawable.ic_own_location)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    infoWindow = null
                }
                overlays.add(userMarker)
            }
        }

        LaunchedEffect(Unit) {
            db.collection("cities")
                .get()
                .addOnSuccessListener { result ->
                    val firebaseCities = result.documents.mapNotNull { it.toObject(City::class.java) }
                    cities = firebaseCities

                    for (city in firebaseCities) {
                        val cityMarker = Marker(mapView).apply {
                            position = GeoPoint(city.latitude, city.longitude)
                            title = city.name
                            icon = context.getDrawable(R.drawable.ic_location_pin)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                selectedCity = city
                                true 
                            }
                        }
                        mapView.overlays.add(cityMarker)
                    }
                    mapView.invalidate()
                }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 12.dp)
                    .zIndex(3f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoomButton("+") { mapView.controller.zoomIn() }
                ZoomButton("–") { mapView.controller.zoomOut() }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(10f)
                    .offset(y = (-35).dp)
                    .height(sheetHeight)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, drag ->
                            if (drag < -8) expanded = true
                            if (drag > 8) expanded = false
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp)
                            .width(50.dp)
                            .height(6.dp)
                            .background(Color.LightGray, RoundedCornerShape(50))
                    )

                    if (expanded) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Zoek naar steden", color = Color.Gray) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF4F5F6),
                                unfocusedContainerColor = Color(0xFFF4F5F6),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )

                        LazyColumn(contentPadding = PaddingValues(bottom = 60.dp)) {
                            items(filteredCities) { city ->
                                val distanceText =
                                    "${calculateDistanceKm(userLocation, GeoPoint(city.latitude, city.longitude))} km"
                                CityCard(city, distanceText) {
                                    expanded = false
                                    // Animate to city location and select it
                                    mapView.controller.animateTo(GeoPoint(city.latitude, city.longitude))
                                    selectedCity = city
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
            overlays.clear()
            val marker = Marker(this).apply {
                position = GeoPoint(city.latitude, city.longitude)
                icon = context.getDrawable(R.drawable.ic_location_pin)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            overlays.add(marker)
        }
    }

    val attractions = when (city.name) {
        "Antwerpen" -> listOf(
            Attraction("Kathedraal van Antwerpen", "Cultuur", "https://assets.antwerpen.be/srv/assets/api/image/1ee41d36-f4bb-4125-9a27-553c75ed8fc0/CAGM_olv_vooraanzicht.jpg", 51.2206, 4.4009),
            Attraction("Grote Markt", "Historisch", "https://www.stedentrippers.nl/wp-content/uploads/2022/04/de-grote-markt-in-antwerpen.jpg", 51.2212, 4.3997)
        )
        "Leuven" -> listOf(
            Attraction("Stadhuis van Leuven", "Historisch", "https://img.static-rmg.be/a/view/q75/w618/h397/5430316/4b9161ed7fa27e73a8618718ee5e2190-jpg.jpg", 50.8799, 4.7005),
            Attraction("Oude Markt", "Eten & Drinken", "https://cdn.shopify.com/s/files/1/0501/6751/3239/files/BF842046-8E4C-49C8-BEFB-8BC332AD3B76-FE25F89A-D01E-41A2-A2BE-BEB41F3FA9A6_2_1024x1024.jpg?v=1666087226", 50.8788, 4.7012)
        )
        "Amsterdam" -> listOf(
            Attraction("Rijksmuseum", "Cultuur", "https://cdn-imgix.headout.com/media/images/a4d93bc58c9528951ed3124f77d268e4-544-amsterdam-003-amsterdam-%7C-rijksmuseum-02.jpg", 52.3599, 4.8852),
            Attraction("RAI Hotel", "Verblijf", "https://www.qurails.nl/wp-content/uploads/2022/04/Nhow-bewerkt2-min.jpg", 52.3441, 4.8925)
        )
        else -> emptyList()
    }

    var search by remember { mutableStateOf("") }

    var selectedCategories by remember { mutableStateOf(setOf("Alles")) }
    val categories = listOf("Alles", "Eten & Drinken", "Cultuur", "Winkelen", "Verblijf", "Historisch")
    val filteredAttractions = attractions.filter { attraction ->
        val searchMatch = attraction.name.contains(search, ignoreCase = true)
        val categoryMatch = selectedCategories.contains("Alles") || selectedCategories.contains(attraction.category)
        searchMatch && categoryMatch
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { /*TODO*/ },
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
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                forEachGesture {
                                    awaitPointerEventScope {
                                        awaitPointerEvent()
                                        currentEvent.changes.forEach { it.consume() }
                                    }
                                }
                            },
                        factory = { mapView },
                    )
                }

                LazyColumn(Modifier.padding(16.dp)) {
                    items(filteredAttractions) { attr ->
                        val distance = calculateDistanceKm(userLocation, GeoPoint(attr.latitude, attr.longitude))
                        Card(
                            Modifier
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
                                    Text("${attr.category} • ${distance} km", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ZoomButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CityCard(city: City, distanceText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(city.imageUrl),
                contentDescription = city.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(city.name, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(distanceText, color = Color.Gray)
            }
        }
    }
}

fun calculateDistanceKm(from: GeoPoint, to: GeoPoint): Int {
    val r = 6371
    val lat1 = from.latitude.toRadians()
    val lon1 = from.longitude.toRadians()
    val lat2 = to.latitude.toRadians()
    val lon2 = to.longitude.toRadians()
    val dLat = lat2 - lat1
    val dLon = lon2 - lon1
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (r * c).roundToInt()
}

private fun Double.toRadians() = this * Math.PI / 180
