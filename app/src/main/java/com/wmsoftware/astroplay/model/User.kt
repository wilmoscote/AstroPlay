package com.wmsoftware.astroplay.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class User(
    val id:String? = null,
    val name:String? = null,
    val email:String? = null,
    val photo:String? = null,
    val favorites: List<String> = listOf(),
    val role: Int? = null,
    val disabled: Boolean? = false
)
