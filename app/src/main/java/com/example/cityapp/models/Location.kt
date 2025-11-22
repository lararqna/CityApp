package com.example.cityapp.models

data class Location(
    val id: String = "",
    val name: String,
    val category: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double
)