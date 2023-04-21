package com.wm.astroplay.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.wm.astroplay.R
import com.wm.astroplay.databinding.ActivityEditProfileBinding
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.viewmodel.AuthenticationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var userPreferences: UserPreferences
    private var currentUser: User? = null
    private val viewModel: AuthenticationViewModel by viewModels()
    private var imageUrl = ""
    private var imageUri: Uri? = null
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){ uri ->
        if( uri != null) {
            try {
                Glide.with(this@EditProfileActivity).load(uri).circleCrop().transition(
                    DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.default_user).into(binding.profileImage)
                imageUri = uri
            } catch (e:Exception){
                e.printStackTrace()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUser().collect { user ->
                currentUser = user
                imageUrl = user?.photo ?: ""
                withContext(Dispatchers.Main) {
                    try {
                        Glide.with(this@EditProfileActivity).load(user?.photo).circleCrop().transition(
                            DrawableTransitionOptions.withCrossFade())
                            .error(R.drawable.default_user).into(binding.profileImage)
                        binding.usernameEditText.setText(user?.name)
                    } catch (e: Exception) {
                        //
                    }
                }
            }
        }

        binding.profileImage.setOnClickListener {
            selectImage()
        }

        binding.saveButton.setOnClickListener {
            val newName = binding.usernameEditText.text.toString()

            if (newName.isNotBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    currentUser?.let {
                        viewModel.updateProfile(it, imageUri, newName)
                    }
                }
            } else {
                Toast.makeText(this@EditProfileActivity, getString(R.string.new_name_error), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.userUpdated.observe(this) {
            Toast.makeText(this@EditProfileActivity, getString(R.string.profile_updated_success), Toast.LENGTH_SHORT).show()
            finish()
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.loading.isVisible = isLoading
        }
    }

    private fun selectImage(){
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}