package com.wm.astroplay.view.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.wm.astroplay.R
import com.wm.astroplay.databinding.FragmentNotificationsBinding
import com.wm.astroplay.model.MovieProvider
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.model.interfaces.FragmentNavigationListener
import com.wm.astroplay.view.adapters.MovieAdapter
import com.wm.astroplay.view.adapters.NotificationAdapter
import com.wm.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


class NotificationsFragment : Fragment() {

    private lateinit var binding: FragmentNotificationsBinding
    private val viewModel: MoviesViewModel by viewModels()
    private lateinit var userPreferences: UserPreferences
    private var navigationListener: FragmentNavigationListener? = null

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
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUser().collect { user ->
                withContext(Dispatchers.Main){
                    try {
                        Glide.with(this@NotificationsFragment).load(user?.photo).circleCrop()
                            .error(R.drawable.default_user).into(binding.profileImg)


                    } catch (e:Exception){
                        //
                    }
                }
                viewModel.getUserNotifications(user?.id ?: "")
            }
        }

        binding.btnBack.setOnClickListener {
            navigationListener?.onNavigateTo("home")
        }

        viewModel.userNotifications.observe(viewLifecycleOwner){ notifications ->
            val notificationsAdapter = NotificationAdapter(notifications)
            binding.rvNotifications.adapter = notificationsAdapter
            binding.rvNotifications.layoutManager =
                LinearLayoutManager(this.requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        return binding.root
    }
}