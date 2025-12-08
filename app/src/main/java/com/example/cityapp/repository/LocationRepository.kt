package com.example.cityapp.repository

import com.example.cityapp.dao.CityDao
import com.example.cityapp.dao.LocationDao
import com.example.cityapp.entity.CityEntity
import com.example.cityapp.entity.LocationEntity
import com.example.cityapp.models.City
import com.example.cityapp.models.Location
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LocationRepository(
    private val cityDao: CityDao,
    private val locationDao: LocationDao,
    private val db: FirebaseFirestore
) {


    private fun CityEntity.toModel() = City(
        id = id,
        name = name,
        imageUrl = imageUrl,
        latitude = latitude,
        longitude = longitude
    )

    private fun LocationEntity.toModel() = Location(
        id = id,
        name = name,
        categories = if (categories.isBlank()) emptyList() else categories.split(";"),
        imageUrl = imageUrl,
        address = address,
        latitude = latitude,
        longitude = longitude,
        initialReview = initialReview,
        initialRating = initialRating,
        initialUsername = initialUsername,
        initialUserId = initialUserId
    )


    fun getCities(): Flow<List<City>> =
        cityDao.getAllCities()
            .map { list -> list.map { it.toModel() } }

    suspend fun refreshCities() {
        val snapshot = db.collection("cities").get().await()

        val entities = snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null

            CityEntity(
                id = doc.id,
                name = data["name"] as? String ?: "",
                imageUrl = data["imageUrl"] as? String ?: "",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0
            )
        }

        cityDao.clearCities()
        cityDao.insertCities(entities)
    }


    fun getLocationsForCity(cityId: String): Flow<List<Location>> =
        locationDao.getLocationsForCity(cityId)
            .map { list -> list.map { it.toModel() } }

    suspend fun refreshLocationsForCity(cityId: String) {
        val snapshot = db.collection("cities")
            .document(cityId)
            .collection("locations")
            .get()
            .await()

        val entities = snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null

            val categories = when {
                data["categories"] is List<*> ->
                    (data["categories"] as List<*>).filterIsInstance<String>()
                data["category"] is String ->
                    listOf(data["category"] as String)
                else -> emptyList()
            }

            LocationEntity(
                id = doc.id,
                cityId = cityId,
                name = data["name"] as? String ?: "",
                categories = categories.joinToString(";"),
                imageUrl = data["imageUrl"] as? String ?: "",
                address = data["address"] as? String,
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                initialReview = data["initialReview"] as? String,
                initialRating = (data["initialRating"] as? Number)?.toInt(),
                initialUsername = data["initialUsername"] as? String,
                initialUserId = data["initialUserId"] as? String
            )
        }

        locationDao.clearLocationsForCity(cityId)
        locationDao.insertLocations(entities)
    }

    suspend fun refreshAllCitiesAndLocations() {
        val snapshot = db.collection("cities").get().await()

        val cityEntities = mutableListOf<CityEntity>()
        val locationEntities = mutableListOf<LocationEntity>()

        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val cityId = doc.id

            cityEntities += CityEntity(
                id = cityId,
                name = data["name"] as? String ?: "",
                imageUrl = data["imageUrl"] as? String ?: "",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0
            )

            val locSnap = db.collection("cities")
                .document(cityId)
                .collection("locations")
                .get()
                .await()

            locSnap.documents.forEach { locDoc ->
                val locData = locDoc.data ?: return@forEach

                val categories = when {
                    locData["categories"] is List<*> ->
                        (locData["categories"] as List<*>).filterIsInstance<String>()
                    locData["category"] is String ->
                        listOf(locData["category"] as String)
                    else -> emptyList()
                }

                locationEntities += LocationEntity(
                    id = locDoc.id,
                    cityId = cityId,
                    name = locData["name"] as? String ?: "",
                    categories = categories.joinToString(";"),
                    imageUrl = locData["imageUrl"] as? String ?: "",
                    address = locData["address"] as? String,
                    latitude = (locData["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (locData["longitude"] as? Number)?.toDouble() ?: 0.0,
                    initialReview = locData["initialReview"] as? String,
                    initialRating = (locData["initialRating"] as? Number)?.toInt(),
                    initialUsername = locData["initialUsername"] as? String,
                    initialUserId = locData["initialUserId"] as? String
                )
            }
        }

        cityDao.clearCities()
        cityDao.insertCities(cityEntities)
        locationDao.insertLocations(locationEntities)
    }
}
