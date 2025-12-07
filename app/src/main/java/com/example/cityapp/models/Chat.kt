package com.example.cityapp.models

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class Chat(
    val id: String = "",
    val users: List<String> = emptyList(),
    val lastMessageText: String? = null,
    val lastMessageTimestamp: Timestamp? = null,
    val lastMessageSenderId: String? = null
)