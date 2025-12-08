package com.example.cityapp.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val cityId: String,
    val name: String,
    val categories: String,
    val imageUrl: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val initialReview: String?,
    val initialRating: Int?,
    val initialUsername: String?,
    val initialUserId: String?
)
