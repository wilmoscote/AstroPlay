package com.wm.astroplay.model

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wm.astroplay.R
import com.wm.astroplay.view.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        //Log.d("AstroDebug","Message received")
        // Check if message contains a data payload.
        remoteMessage.data["movie_link"]?.let { movieLink ->
            //Log.d("AstroDebug","Notify MovieLink: $movieLink")
            createNotificationChannel()
            sendNotification(movieLink,remoteMessage.notification?.title ?: "AstroPlay", remoteMessage.notification?.body ?: "Tenemos esto para ti :)")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("astroplay_notifications", name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(movieLink: String, title:String, message: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.data = Uri.parse(movieLink)

        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, pendingIntentFlags
        )

        val notificationBuilder = NotificationCompat.Builder(this, "astroplay_notifications")
            .setSmallIcon(R.drawable.ic_notify)
            .setColor(ContextCompat.getColor(this, R.color.palette_red))
            .setContentTitle(title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        val db = Firebase.firestore
        val userPreferences = UserPreferences(this)

        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.saveFcmToken(token)
            userPreferences.getUser().collect { user ->
                try {
                    val updatedUser = User(
                        id = user?.id,
                        name = user?.name,
                        email = user?.email,
                        photo = user?.photo,
                        favorites = user?.favorites,
                        role = user?.role,
                        deviceId = user?.deviceId,
                        fcmToken = token,
                        disabled = user?.disabled,
                        createdAt = user?.createdAt
                    )
                    db.collection("users").document(user?.id.toString()).set(updatedUser).await()
                } catch (e:Exception){
                    //
                }
            }
        }
    }
}