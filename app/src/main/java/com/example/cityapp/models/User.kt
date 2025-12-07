package com.example.cityapp.models

import androidx.annotation.Keep

@Keep
data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val birthDate: String = "",
    val profilePictureUrl: String = ""
)
