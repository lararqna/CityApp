package com.example.cityapp.ui

import City
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(onBack: () -> Unit) {
    var cityName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = Firebase.firestore
    val storage = Firebase.storage

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
            // Sectie 1: Stadnaam
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
            Text(
                "Sleep de kaart tot de rode pin op de juiste plek staat.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            LocationPickerMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
                onLocationSelected = { selectedLocation = it }
            )
            selectedLocation?.let {
                Text(
                    text = "Lat: %.4f, Lon: %.4f".format(it.latitude, it.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
            Spacer(Modifier.height(16.dp))
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
fun LocationPickerMap(
    modifier: Modifier = Modifier,
    onLocationSelected: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        org.osmdroid.config.Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
        MapView(context).apply {
            controller.setCenter(GeoPoint(51.2194, 4.4025))
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(12.0)
            setMultiTouchControls(true)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isTilesScaledToDpi = true
            overlays.clear()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        onLocationSelected(view.mapCenter as GeoPoint)
                        return true
                    }
                    override fun onZoom(event: ZoomEvent?): Boolean {
                        onLocationSelected(view.mapCenter as GeoPoint)
                        return true
                    }
                })
            }
        )
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Kaart pin",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
        )
    }
}

@Composable
fun ImagePicker(
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            onImageSelected(uri)
        }
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
                contentDescription = "Geselecteerde afbeelding",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Selecteer afbeelding",
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
                Text("Selecteer een afbeelding", color = Color.Gray)
            }
        }
    }
}

