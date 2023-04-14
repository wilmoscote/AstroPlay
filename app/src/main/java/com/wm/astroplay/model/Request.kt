package com.wm.astroplay.model

import com.google.firebase.Timestamp

data class Request(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val title: String,
    val description: String? = null,
    val timestamp: Timestamp = Timestamp.now()
)

