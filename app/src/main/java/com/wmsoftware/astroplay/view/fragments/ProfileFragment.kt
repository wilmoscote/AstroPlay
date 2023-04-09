package com.wmsoftware.astroplay.view.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.wmsoftware.astroplay.BuildConfig
import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.databinding.FragmentProfileBinding
import com.wmsoftware.astroplay.model.UserPreferences
import com.wmsoftware.astroplay.view.AuthenticationActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        userPreferences = UserPreferences(this.requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUser().collect { user ->
                withContext(Dispatchers.Main){
                    Glide.with(this@ProfileFragment).load(user?.photo).circleCrop().transition(
                        DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.ic_person).into(binding.profileImage)
                    binding.userName.text = user?.name
                    when(user?.role){
                        1 -> {
                            binding.userRole.setBackgroundResource(R.drawable.role_badge)
                            binding.userRole.text = getString(R.string.role_user_1)
                        }
                        2 -> {
                            binding.userRole.setBackgroundResource(R.drawable.role_badge_2)
                            binding.userRole.text = getString(R.string.role_user_2)
                        }
                        3 -> {
                            binding.userRole.setBackgroundResource(R.drawable.role_badge_3)
                            binding.userRole.text = getString(R.string.role_user_3)
                        }
                    }
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this.requireContext(),R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.logout)) { dialog, which ->
                    Firebase.auth.signOut()
                    CoroutineScope(Dispatchers.IO).launch {
                        userPreferences.clearDataStore()
                        startActivity(Intent(this@ProfileFragment.requireContext(), AuthenticationActivity::class.java))
                        requireActivity().finish()
                    }
                }
                .show()
        }

        binding.versionInfo.setOnLongClickListener {
            //Toast.makeText(this, "S \uD83D\uDC9B", Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }

        binding.versionInfo.text = getString(R.string.version_info, BuildConfig.VERSION_NAME)
        return binding.root
    }

    companion object {

    }
}