package com.wm.astroplay.model.interfaces

import com.wm.astroplay.model.Notification

interface NotificationInterface {
    fun onItemDeleted(notification: Notification)
}