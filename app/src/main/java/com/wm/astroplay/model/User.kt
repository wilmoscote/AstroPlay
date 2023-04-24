package com.wm.astroplay.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id:String? = null,
    val name:String? = null,
    val email:String? = null,
    val photo:String? = null,
    val favorites: List<String>? = listOf(),
    val role: Int? = null,
    val deviceId: String? = null,
    val fcmToken: String? = null,
    val disabled: Boolean? = false,
    val createdAt: Long? = null
)
