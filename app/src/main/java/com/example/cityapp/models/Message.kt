package com.example.cityapp.models

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val usersInChat: List<String> = emptyList(),
    val read: Boolean = false
)