package com.wmsoftware.astroplay.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

import java.util.*

@Serializable
@Parcelize
data class Movie(
    val title: String = "",
    val originalTitle: String? = null,
    val year: String? = null,
    val director: String? = null,
    val actors: List<String>? = null,
    val genre: List<String>? = null,
    val runtime: String? = null,
    val plot: String? = null,
    val poster: String? = null,
    val imdbRating: String? = null,
    val appRating: String? = null,
    val views: Int? = null,
    val language: String? = null,
    val ageRating: String? = null,
    val url: String = "",
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Timestamp? = null
) : Parcelable
