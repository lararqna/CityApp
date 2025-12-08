package com.example.cityapp.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double
)
