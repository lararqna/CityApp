package com.example.cityapp.ui

import Review
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.cityapp.models.Location
import com.example.cityapp.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import org.osmdroid.util.GeoPoint
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    location: Location,
    onBack: () -> Unit,
    userLocation: GeoPoint?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val auth = Firebase.auth

    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var myReviewText by remember { mutableStateOf("") }
    var myRating by remember { mutableStateOf(0f) }
    var isPosting by remember { mutableStateOf(false) }
    var currentUserData by remember { mutableStateOf<User?>(null) }


    LaunchedEffect(location.id) {
        db.collection("attractions").document(location.id)
            .collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    reviews = snapshot.toObjects(Review::class.java)
                }
            }
    }

    LaunchedEffect(auth.currentUser) {
        val user = auth.currentUser ?: return@LaunchedEffect
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    currentUserData = doc.toObject(User::class.java)
                }
            }
    }

    fun postReview() {
        val user = auth.currentUser
        if (user == null || currentUserData == null) {
            Toast.makeText(context, "Log eerst in!", Toast.LENGTH_SHORT).show()
            return
        }
        if (myReviewText.isBlank() || myRating == 0f) {
            Toast.makeText(context, "Vul alles in!", Toast.LENGTH_SHORT).show()
            return
        }

        isPosting = true
        val displayName = "${currentUserData!!.firstName} ${currentUserData!!.lastName}".trim()

        val newReview = Review(
            userId = user.uid,
            username = displayName,
            rating = myRating,
            text = myReviewText,
            timestamp = Timestamp.now()
        )

        db.collection("attractions").document(location.id)
            .collection("reviews").add(newReview)
            .addOnSuccessListener {
                myReviewText = ""
                myRating = 0f
                isPosting = false
                Toast.makeText(context, "Bedankt voor je review!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                isPosting = false
                Toast.makeText(context, "Fout opgetreden", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(location.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = rememberAsyncImagePainter(location.imageUrl),
                contentDescription = location.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                val averageRating by remember(reviews) {
                    derivedStateOf {
                        if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
                    }
                }

                val reviewCount by remember(reviews) { derivedStateOf { reviews.size } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (reviewCount == 0) {
                            "Nog geen beoordelingen"
                        } else {
                            "%.1f (%d %s)".format(
                                averageRating,
                                reviewCount,
                                if (reviewCount == 1) "beoordeling" else "beoordelingen"
                            )
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    "Een populaire bezienswaardigheid in de stad. Perfect voor een dagje uit!",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Afstand vanaf jouw locatie",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = calculateDistanceKm(userLocation, GeoPoint(location.latitude, location.longitude)),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Divider()

                Text("Beoordelingen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Wat vond jij ervan?", fontWeight = FontWeight.Bold)

                        Row {
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = if (star <= myRating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { myRating = star.toFloat() }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = myReviewText,
                            onValueChange = { myReviewText = it },
                            label = { Text("Deel je ervaring...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )

                        Button(
                            onClick = { postReview() },
                            enabled = !isPosting && myRating > 0f && myReviewText.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (isPosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Plaatsen")
                            }
                        }
                    }
                }

                if (reviews.isEmpty()) {
                    Text("Nog geen beoordelingen. Wees de eerste!", color = Color.Gray)
                } else {
                    reviews.forEach { review ->
                        ReviewCard(review = review)
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(review.username, fontWeight = FontWeight.Bold)
                Text(
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(review.timestamp.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Row(modifier = Modifier.padding(vertical = 6.dp)) {
                (1..5).forEach { i ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (i <= review.rating) Color(0xFFFFC107) else Color.LightGray
                    )
                }
            }

            Text(review.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun calculateDistanceKm(userLocation: GeoPoint?, target: GeoPoint): String {
    if (userLocation == null) return "Locatie onbekend"

    val earthRadius = 6371.0

    val dLat = Math.toRadians(target.latitude - userLocation.latitude)
    val dLon = Math.toRadians(target.longitude - userLocation.longitude)

    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(userLocation.latitude)) *
            cos(Math.toRadians(target.latitude)) *
            sin(dLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val distance = earthRadius * c

    return when {
        distance < 1 -> "${(distance * 1000).toInt()} m"
        distance < 10 -> "%.1f km".format(distance)
        else -> "%.0f km".format(distance)
    }
}