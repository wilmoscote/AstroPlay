package com.wm.astroplay.model

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
    val id: String? = null,
    val title: String = "",
    val originalTitle: String? = null,
    val year: String? = null,
    val director: String? = null,
    val actors: List<String>? = null,
    val genre: List<String>? = null,
    val runtime: String? = null,
    val plot: String? = null,
    val quality: String? = null,
    val poster: String? = null,
    val banner: String? = null,
    val imdbRating: String? = null,
    val appRating: Float? = null,
    val views: Int? = null,
    val new: Boolean? = false,
    val language: String? = null,
    val ageRating: String? = null,
    val url: String = "",
    val numVotes: Int? = null,
    val isPremiere: Boolean? = false,
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Timestamp? = null
) : Parcelable
