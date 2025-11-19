package com.example.cityapp.ui

import City
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationScreen(
    city: City,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val storage = Firebase.storage

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val categories = listOf("Eten & Drinken", "Cultuur", "Winkelen", "Verblijf", "Historisch")
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nieuwe locatie", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(
                        "Annuleren",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { onCancel() }
                    )
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState())
        )
        {

            Spacer(Modifier.height(10.dp))

            Text("Naam", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Naam van de locatie") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text("Categorie", fontWeight = FontWeight.SemiBold)
            LazyRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Beschrijving", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Beschrijving van de locatie") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text("Foto", fontWeight = FontWeight.SemiBold)
            ImagePicker(
                selectedImageUri = selectedImage,
                onImageSelected = { selectedImage = it },
                modifier = Modifier.height(180.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text("Kies positie", fontWeight = FontWeight.SemiBold)
            LocationPickerMap(
                modifier = Modifier
                    .height(220.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                onLocationSelected = { selectedPoint = it }
            )

            selectedPoint?.let {
                Text(
                    "Lat: %.4f, Lon: %.4f".format(it.latitude, it.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    if (name.isBlank() || selectedCategory == null || selectedPoint == null) {
                        Toast.makeText(context, "Vul alle velden in!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (city.id == null) {
                        Toast.makeText(context, "Error: city.id is null", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    fun saveLocation(imageUrl: String?) {
                        val newLocation = mapOf(
                            "name" to name,
                            "category" to selectedCategory,
                            "description" to description,
                            "imageUrl" to imageUrl,
                            "latitude" to selectedPoint!!.latitude,
                            "longitude" to selectedPoint!!.longitude
                        )

                        db.collection("cities")
                            .document(city.id!!)
                            .collection("locations")
                            .add(newLocation)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Locatie toegevoegd!", Toast.LENGTH_SHORT).show()
                                onCancel()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                            .addOnCompleteListener { isLoading = false }
                    }

                    if (selectedImage != null) {
                        val ref = storage.reference.child("location_images/${UUID.randomUUID()}.jpg")
                        ref.putFile(selectedImage!!)
                            .continueWithTask { ref.downloadUrl }
                            .addOnSuccessListener { saveLocation(it.toString()) }
                    } else {
                        saveLocation(null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White)
                else Text("Locatie opslaan")
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
