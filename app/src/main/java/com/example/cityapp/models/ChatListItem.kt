package com.example.cityapp.models

data class ChatListItem(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String,
    val hasUnread: Boolean
)