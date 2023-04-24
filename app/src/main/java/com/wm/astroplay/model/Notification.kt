package com.wm.astroplay.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String? = null,
    val title: String? = null,
    val message: String? = null,
    val timestamp: Timestamp? = null,
    val type: String? = null, // tipo de notificación, por ejemplo: "new_release", "news", etc.
    val targetUsers: List<String>? = null, // lista de ID de usuarios a los que va dirigida la notificación
    val link: String? = null
)
