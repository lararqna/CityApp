package com.example.cityapp.ui

import com.example.cityapp.models.City
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.example.cityapp.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.*
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.net.URL
import java.net.URLEncoder
import java.util.*
import android.Manifest
import com.google.android.gms.location.LocationServices
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationScreen(
    city: City,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val storage = Firebase.storage
    val auth = Firebase.auth

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var selectedCategory by remember { mutableStateOf<List<String>>(emptyList()) }
    var geocodingLoading by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf("") }
    var initialRating by remember { mutableStateOf(0f) }

    val categories = listOf(
        "Eten & Drinken",
        "Café & Bar",
        "Cultuur",
        "Natuur & Parken",
        "Bezienswaardigheid",
        "Historisch",
        "Winkelen",
        "Verblijf",
        "Sport & Recreatie",
        "Musea",
        "Uitgaan & Nachtleven",
        "Gezin & Kinderen",
        "Wellness & Ontspanning",
        "Evenementen",
        "Overig"
    )

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun reverseGeocode(point: GeoPoint): String? = withContext(Dispatchers.IO) {
        try {
            val url =
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=${point.latitude}&lon=${point.longitude}"
            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "CityApp/1.0")
            val response = connection.getInputStream().bufferedReader().readText()
            val obj = org.json.JSONObject(response)
            obj.getString("display_name")
        } catch (_: Exception) {
            null
        }
    }

    val scope = rememberCoroutineScope()

    fun requestCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    val userPoint = GeoPoint(lastLoc.latitude, lastLoc.longitude)
                    selectedPoint = userPoint
                    scope.launch {
                        val foundAddress = reverseGeocode(userPoint)
                        if (foundAddress != null) {
                            address = foundAddress
                        }
                    }
                } else {
                    fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { currentLoc ->
                        if (currentLoc != null) {
                            val userPoint = GeoPoint(currentLoc.latitude, currentLoc.longitude)
                            selectedPoint = userPoint
                            scope.launch {
                                val foundAddress = reverseGeocode(userPoint)
                                if (foundAddress != null) {
                                    address = foundAddress
                                }
                            }
                        } else {
                            Toast.makeText(context, "Kon de locatie niet bepalen.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Beveiligingsfout bij ophalen locatie.", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) requestCurrentLocation()
            else Toast.makeText(context, "Locatie-permissie geweigerd", Toast.LENGTH_SHORT).show()
        }
    )

    LaunchedEffect(true) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    suspend fun geocode(query: String): GeoPoint? = withContext(Dispatchers.IO) {
        try {
            val url =
                "https://nominatim.openstreetmap.org/search?format=json&q=" +
                        URLEncoder.encode(query, "UTF-8")
            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "CityApp/1.0")
            val response = connection.getInputStream().bufferedReader().readText()
            val arr = JSONArray(response)
            if (arr.length() == 0) return@withContext null
            val obj = arr.getJSONObject(0)
            GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
        } catch (_: Exception) {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nieuwe locatie", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(
                        "Annuleren",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable { onCancel() }
                    )
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text("Naam", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text("Categorieën", fontWeight = FontWeight.SemiBold)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory.contains(cat),
                        onClick = {
                            selectedCategory =
                                if (selectedCategory.contains(cat)) selectedCategory - cat
                                else selectedCategory + cat
                        },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Adres", fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text("bv. Grand Place, 1000 Brussel") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (address.isBlank()) {
                            Toast.makeText(context, "Voer een adres in!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        geocodingLoading = true
                        scope.launch {
                            val point = geocode(address)
                            geocodingLoading = false
                            if (point == null)
                                Toast.makeText(context, "Adres niet gevonden!", Toast.LENGTH_LONG).show()
                            else selectedPoint = point
                        }
                    },
                    enabled = !geocodingLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (geocodingLoading) CircularProgressIndicator(Modifier.size(20.dp))
                    else Text("Zoek adres")
                }

                IconButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Gebruik mijn huidige locatie",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LocationPickerMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp)),
                selectedPoint = selectedPoint
            )

            Spacer(Modifier.height(20.dp))

            Text("Foto", fontWeight = FontWeight.SemiBold)

            ImagePicker(
                selectedImageUri = selectedImage,
                onImageSelected = { selectedImage = it },
                modifier = Modifier.height(200.dp)
            )

            Spacer(Modifier.height(32.dp))

            Text("Jouw beoordeling", fontWeight = FontWeight.SemiBold)

            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (star <= initialRating) Color(0xFFFFC107) else Color.LightGray,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { initialRating = star.toFloat() }
                    )
                }
            }

            Text("Eerste recensie", fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = review,
                onValueChange = { review = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Schrijf een korte eerste recensie...") },
                minLines = 3
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (
                        name.isBlank() ||
                        selectedCategory.isEmpty() ||
                        selectedPoint == null ||
                        review.isBlank() ||
                        initialRating == 0f
                    ) {
                        Toast.makeText(context, "Vul alle velden in!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    val user = auth.currentUser
                    if (user == null) {
                        Toast.makeText(context, "Niet ingelogd", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@Button
                    }

                    db.collection("users").document(user.uid).get()
                        .addOnSuccessListener { doc ->

                            val firstName = doc.getString("firstName") ?: ""
                            val lastName = doc.getString("lastName") ?: ""
                            val fullName = "$firstName $lastName".trim()

                            val location = mutableMapOf(
                                "name" to name,
                                "categories" to selectedCategory,
                                "imageUrl" to "",
                                "address" to address,
                                "latitude" to selectedPoint!!.latitude,
                                "longitude" to selectedPoint!!.longitude,
                                "initialReview" to review,
                                "initialRating" to initialRating.toInt(),
                                "initialUsername" to fullName
                            )

                            fun save(url: String?) {
                                if (url != null) location["imageUrl"] = url

                                db.collection("cities")
                                    .document(city.id!!)
                                    .collection("locations")
                                    .add(location)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Locatie toegevoegd!", Toast.LENGTH_SHORT).show()
                                        onCancel()
                                    }
                                    .addOnCompleteListener { isLoading = false }
                            }

                            if (selectedImage != null) {
                                val ref =
                                    storage.reference.child("location_images/${UUID.randomUUID()}.jpg")
                                ref.putFile(selectedImage!!)
                                    .continueWithTask { ref.downloadUrl }
                                    .addOnSuccessListener { save(it.toString()) }
                            } else save(null)
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White)
                else Text("Locatie opslaan")
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun LocationPickerMap(
    modifier: Modifier = Modifier,
    selectedPoint: GeoPoint?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        Configuration.getInstance().userAgentValue = "CityApp"
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(51.2194, 4.4025))
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, e ->
                when (e) {
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    else -> {}
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedPoint) {
        if (selectedPoint != null) {
            mapView.overlays.clear()
            val marker = org.osmdroid.views.overlay.Marker(mapView).apply {
                position = selectedPoint
                icon = context.getDrawable(R.drawable.ic_location_pin)
                setAnchor(
                    org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                    org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM
                )
                infoWindow = null
            }
            mapView.overlays.add(marker)
            mapView.controller.animateTo(selectedPoint)
            mapView.invalidate()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView }
    )
}
