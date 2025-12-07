package com.example.cityapp.models

data class Location(
    val id: String = "",
    val name: String,
    val categories: List<String> = emptyList(),
    val imageUrl: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val initialReview: String? = null,
    val initialRating: Int? = null,
    val initialUsername: String? = null
){
    constructor() : this(
        id = "",
        name = "",
        categories = emptyList(),
        imageUrl = "",
        address = "",
        latitude = 0.0,
        longitude = 0.0,
        initialReview = "",
        initialRating = 0,
        initialUsername = ""
    )
}