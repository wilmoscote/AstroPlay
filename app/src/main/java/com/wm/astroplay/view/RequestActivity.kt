package com.wm.astroplay.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.wm.astroplay.databinding.ActivityRequestBinding
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RequestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRequestBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var currentUser: User? = null
    private lateinit var userPreferences: UserPreferences
    private val viewModel: MoviesViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this.applicationContext)


        CoroutineScope(Dispatchers.IO).launch {
            lifecycleScope.launch(Dispatchers.IO) {
                userPreferences.getUser().collect { user ->
                    currentUser = user
                }
            }
        }

        viewModel.loading.observe(this){ loading ->
            binding.loading.isVisible = loading
        }

        viewModel.requestSent.observe(this){ requestSuccess ->
            if(requestSuccess){
                Toast.makeText(this, "Solicitud enviada con éxito.", Toast.LENGTH_SHORT).show()
                onBackPressedDispatcher.onBackPressed()
            } else {
                Toast.makeText(this, "Error al enviar solicitud, intentelo nuevamente.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSubmit.setOnClickListener {
            if (binding.etTitle.text.toString().isEmpty()) {
                Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
            } else {
                if (currentUser != null){
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.sendRequest(binding.etTitle.text.toString(),binding.etDescription.text.toString(),currentUser!!)
                    }
                }
            }
        }
    }
}