package com.example.cityapp.models

import androidx.annotation.Keep

@Keep
data class Chat(
    val id: String = "",
    val users: List<String> = emptyList()
)