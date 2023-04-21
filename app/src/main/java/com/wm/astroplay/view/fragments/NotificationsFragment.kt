package com.wm.astroplay.view.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.wm.astroplay.R
import com.wm.astroplay.databinding.FragmentNotificationsBinding
import com.wm.astroplay.model.*
import com.wm.astroplay.model.interfaces.FragmentNavigationListener
import com.wm.astroplay.model.interfaces.NotificationInterface
import com.wm.astroplay.view.adapters.MovieAdapter
import com.wm.astroplay.view.adapters.NotificationAdapter
import com.wm.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


class NotificationsFragment : Fragment(), NotificationInterface {

    private lateinit var binding: FragmentNotificationsBinding
    private val viewModel: MoviesViewModel by viewModels()
    private lateinit var userPreferences: UserPreferences
    private var navigationListener: FragmentNavigationListener? = null
    private var currentUser: User? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentNavigationListener) {
            navigationListener = context
        } else {
            //throw RuntimeException("$context must implement FragmentNavigationListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(layoutInflater)
        userPreferences = UserPreferences(this.requireContext())

        setupProfileImage()

        setupBackButton()

        observeUserNotifications()

        return binding.root
    }

    private fun setupProfileImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userPreferences.getUser().collect { user ->
                    currentUser = user
                    loadProfileImage(user)
                    viewModel.getUserNotifications(user?.id ?: "")
                }
            }
        }
    }

    private fun loadProfileImage(user: User?) {
        Glide.with(this@NotificationsFragment).load(user?.photo).circleCrop()
            .error(R.drawable.default_user).into(binding.profileImg)
        binding.profileImg.setOnClickListener {
            navigationListener?.onNavigateTo("profile")
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            navigationListener?.onNavigateTo("home")
        }
    }

    private fun observeUserNotifications() {
        viewModel.userNotifications.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isNotEmpty()) {
                setupNotificationsRecyclerView(notifications)
            } else {
                binding.rvNotifications.isVisible = false
                binding.emptyNotificationLayout.isVisible = true
            }
        }
    }

    private fun setupNotificationsRecyclerView(notifications: List<Notification>) {
        val notificationsAdapter = NotificationAdapter(notifications.toMutableList(), this)
        binding.rvNotifications.apply {
            adapter = notificationsAdapter
            layoutManager = LinearLayoutManager(this@NotificationsFragment.requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        val swipeHandler = SwipeToDeleteCallback(notificationsAdapter) { position ->
            handleNotificationDeletion(position, notificationsAdapter)
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvNotifications)
    }

    private fun handleNotificationDeletion(position: Int, notificationsAdapter: NotificationAdapter) {
        val notificationId = notificationsAdapter.getNotificationIdAt(position)
        viewLifecycleOwner.lifecycleScope.launch {
            if (currentUser != null) {
                viewModel.removeUserFromNotificationTarget(currentUser?.id.toString(), notificationId.toString())
            }
        }
        notificationsAdapter.removeNotificationAt(position)
    }

    private fun setupProfileImageClickListener() {
        binding.profileImg.setOnClickListener {
            navigationListener?.onNavigateTo("profile")
        }
    }


    override fun onItemDeleted(notification: Notification) {
        CoroutineScope(Dispatchers.IO).launch {
            if (currentUser != null){
                viewModel.removeUserFromNotificationTarget(currentUser?.id.toString(),
                    notification.id.toString()
                )
            }
        }
    }
}