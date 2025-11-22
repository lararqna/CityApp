package com.example.cityapp.models

data class Location(
    val id: String = "",
    val name: String,
    val category: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double
){
    constructor() : this(
        id = "",
        name = "",
        category = "",
        imageUrl = "",
        latitude = 0.0,
        longitude = 0.0
    )
}