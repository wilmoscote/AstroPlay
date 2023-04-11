package com.wm.astroplay.model

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("AstroDebug","Messae received")
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            //
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {

        }
    }

    override fun onNewToken(token: String) {
        //
    }
}