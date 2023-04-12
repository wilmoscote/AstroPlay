package com.wm.astroplay.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.wm.astroplay.databinding.ItemNotificationBinding
import com.wm.astroplay.model.Notification
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification)
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.apply {
                //iconNotification.setImageResource(notification.iconRes) // Reemplaza esto con el ícono que corresponda a la notificación
                titleNotification.text = notification.title
                messageNotification.text = notification.message
                dateNotification.text = "Hace ${notification.timestamp?.let { getFriendlyTime(it) }}"

                optionsIcon.setOnClickListener {
                    // Aquí puedes mostrar un menú de opciones si es necesario, pero en este caso solo vamos a eliminar la notificación

                }
            }

        }
    }

    fun getFriendlyTime(timestamp: Timestamp): String {
        val now = DateTime()
        val notificationTime = DateTime(timestamp.toDate())
        val period = Period(notificationTime, now)

        val formatter = PeriodFormatterBuilder()
            .appendDays().appendSuffix(" día", " días")
            .appendSeparator(", ")
            .appendHours().appendSuffix(" hora", " horas")
            .appendSeparator(" y ")
            .appendMinutes().appendSuffix(" minuto", " minutos")
            .toFormatter()

        return formatter.print(period)
    }
}
