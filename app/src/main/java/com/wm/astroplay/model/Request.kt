package com.wm.astroplay.model

import com.google.firebase.Timestamp

data class Request(
    val id: String? = null,
    val user: User,
    val title: String,
    val description: String? = null,
    val timestamp: Timestamp = Timestamp.now()
)

