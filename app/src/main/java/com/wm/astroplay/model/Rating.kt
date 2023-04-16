package com.wm.astroplay.model

import kotlinx.serialization.Serializable

@Serializable
data class Rating(
    val userId: String? = null,
    val movieId: String? = null,
    val rating: Float? = null
)

