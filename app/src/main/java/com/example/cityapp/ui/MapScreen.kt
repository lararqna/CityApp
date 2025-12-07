package com.example.cityapp.ui

import com.example.cityapp.models.City

import android.annotation.SuppressLint
import android.view.View
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
                controller.setZoom(19.0)
                controller.setCenter(userLocation)
                setMultiTouchControls(true)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isTilesScaledToDpi = true
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
                    val firebaseCities = result.documents.map { doc ->
                        City(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0
                        )
                    }
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
