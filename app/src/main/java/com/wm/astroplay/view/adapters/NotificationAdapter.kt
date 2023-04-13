package com.wm.astroplay.view.adapters

import android.animation.Animator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.firebase.Timestamp
import com.wm.astroplay.R
import com.wm.astroplay.databinding.ItemNotificationBinding
import com.wm.astroplay.model.Notification
import com.wm.astroplay.model.interfaces.NotificationInterface
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

class NotificationAdapter(private val notifications: MutableList<Notification>,val listener: NotificationInterface) :
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
                val iconToLoad = when(notification.type){
                    "news" -> R.drawable.news
                    "premiere" -> R.drawable.estreno
                    else -> R.drawable.dab
                }
                Glide.with(binding.iconNotification.context)
                    .load(iconToLoad)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.iconNotification)
                optionsIcon.setOnClickListener {
                    // Aquí puedes mostrar un menú de opciones si es necesario, pero en este caso solo vamos a eliminar la notificación
                    simulateSwipe(notification)
                }
            }

        }
        private fun simulateSwipe(notification: Notification) {
            // Deslizar un poco a la izquierda
            YoYo.with(Techniques.SlideOutLeft)
                .duration(500)
                .pivotX(50f)
                .pivotY(50f)
                .interpolate(AccelerateDecelerateInterpolator())
                .withListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        // Deslizar de vuelta a la posición inicial
                        try {
                            notifications.removeAt(position)
                            notifyItemRemoved(position)
                            listener.onItemDeleted(notification)
                        } catch (e:Exception){
                            listener.onItemDeleted(notification)
                            notifyItemRemoved(0)
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}
                })
                .playOn(itemView)
        }


    }

    fun getNotificationIdAt(position: Int): String? {
        return notifications[position].id
    }

    fun removeNotificationAt(position: Int) {
        try {
            notifications.removeAt(position)
            notifyItemRemoved(position)
        } catch (e:Exception){
            notifyItemRemoved(0)
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
