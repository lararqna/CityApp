package com.example.cityapp.ui

import com.example.cityapp.models.City
import android.Manifest
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.net.URL
import java.net.URLEncoder
import java.util.*
import com.example.cityapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(onBack: () -> Unit) {
    var cityName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var geocodingLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = Firebase.firestore
    val storage = Firebase.storage
    val scope = rememberCoroutineScope()

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
    fun requestCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    val userPoint = GeoPoint(lastLoc.latitude, lastLoc.longitude)
                    selectedLocation = userPoint
                    scope.launch {
                        val foundAddress = reverseGeocode(userPoint)
                        if (foundAddress != null) {
                            address = foundAddress
                        }
                    }
                } else {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { currentLoc ->
                        if (currentLoc != null) {
                            val userPoint = GeoPoint(currentLoc.latitude, currentLoc.longitude)
                            selectedLocation = userPoint
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
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestCurrentLocation()
        } else {
            Toast.makeText(context, "Locatie-permissie geweigerd", Toast.LENGTH_SHORT).show()
        }
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



    fun uploadImageAndSaveCity(imageUri: Uri, location: GeoPoint) {
        isLoading = true
        val storageRef = storage.reference.child("city_images/${UUID.randomUUID()}.jpg")

        storageRef.putFile(imageUri)
            .continueWithTask { storageRef.downloadUrl }
            .addOnSuccessListener { downloadUri ->

                val cityId = db.collection("cities").document().id

                val newCity = mapOf(
                    "id" to cityId,
                    "name" to cityName,
                    "imageUrl" to downloadUri.toString(),
                    "latitude" to location.latitude,
                    "longitude" to location.longitude
                )

                db.collection("cities")
                    .document(cityId)
                    .set(newCity)
                    .addOnSuccessListener {
                        isLoading = false
                        Toast.makeText(context, "Stad toegevoegd!", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                    .addOnFailureListener {
                        isLoading = false
                        Toast.makeText(context, "Firestore fout: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Upload fout: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nieuwe Stad Toevoegen") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Terug") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Algemene Informatie")
            OutlinedTextField(
                value = cityName,
                onValueChange = { cityName = it },
                label = { Text("Naam van de stad") },
                placeholder = { Text("bv. Antwerpen") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            SectionTitle("Afbeelding van de stad")
            ImagePicker(
                selectedImageUri = selectedImageUri,
                onImageSelected = { selectedImageUri = it },
                modifier = Modifier.height(100.dp)
            )
            Spacer(Modifier.height(24.dp))

            SectionTitle("Kies Locatie")
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text("bv. Antwerpen, België") },
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
                            else {
                                selectedLocation = point
                            }
                        }
                    },
                    enabled = !geocodingLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (geocodingLoading) CircularProgressIndicator(Modifier.size(20.dp))
                    else Text("Zoek locatie")
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
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
                selectedPoint = selectedLocation
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (cityName.isNotBlank() && selectedImageUri != null && selectedLocation != null) {
                        uploadImageAndSaveCity(selectedImageUri!!, selectedLocation!!)
                    } else {
                        Toast.makeText(context, "Vul alle velden in en kies een locatie/afbeelding.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                else Text("Opslaan")
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}



@Composable
fun ImagePicker(
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> onImageSelected(uri) }
    )

    Box(
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF4F5F6))
            .border(
                width = 2.dp,
                color = if (selectedImageUri != null) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { launcher.launch("image/*") },
        contentAlignment = Alignment.Center
    ) {
        if (selectedImageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selectedImageUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
                Text("Selecteer een afbeelding", color = Color.Gray)
            }
        }
    }
}