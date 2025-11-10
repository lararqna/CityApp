package com.example.cityapp.models

data class Attraction(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    val rating: Double = 0.0
)
