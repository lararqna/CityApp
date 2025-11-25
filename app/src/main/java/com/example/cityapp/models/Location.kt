package com.example.cityapp.models

data class Location(
    val id: String = "",
    val name: String,
    val categories: List<String> = emptyList(),
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double
){
    constructor() : this(
        id = "",
        name = "",
        categories = emptyList(),
        imageUrl = "",
        latitude = 0.0,
        longitude = 0.0
    )
}