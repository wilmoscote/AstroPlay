package com.wm.astroplay.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.wm.astroplay.R
import com.wm.astroplay.databinding.ActivityRequestBinding
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.viewmodel.MoviesViewModel
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUser().collect { user ->
                currentUser = user
                if ((user?.role ?: 0) < 2) {
                    isRequestAllowed()
                }
            }
        }

        viewModel.loading.observe(this) { loading ->
            binding.loading.isVisible = loading
        }

        viewModel.requestSent.observe(this) { requestSuccess ->
            if (requestSuccess) {
                lifecycleScope.launch(Dispatchers.IO) {
                    userPreferences.saveLastRequestTime(System.currentTimeMillis())
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RequestActivity, getString(R.string.request_sent), Toast.LENGTH_SHORT).show()
                        onBackPressedDispatcher.onBackPressed()
                    }
                }

            } else {
                Toast.makeText(this, getString(R.string.request_error_sent), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSubmit.setOnClickListener {
            if (binding.etTitle.text.toString().isEmpty()) {
                Toast.makeText(this, getString(R.string.title_required_error), Toast.LENGTH_SHORT).show()
            } else {
                if (currentUser != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.sendRequest(binding.etTitle.text.toString(), binding.etDescription.text.toString(), currentUser!!)
                    }
                }
            }
        }
    }


    private suspend fun isRequestAllowed() {
        userPreferences.getLastRequestTime().collect { lastRequestTime ->
            val currentTime = System.currentTimeMillis()
            val oneDayInMillis = 24 * 60 * 60 * 1000
            val isAllowToRequest = currentTime - (lastRequestTime ?: 0L) >= oneDayInMillis
            //Log.d("AstroDebug","IsAllowed: $isAllowToRequest")
            runOnUiThread {
                if(!isAllowToRequest){
                    showCannotResquestDialog()
                }
            }
        }
    }

    private fun showCannotResquestDialog() {
        try {
            Blurry.with(this)
                .radius(10)
                .sampling(8)
                .async()
                .onto(binding.root)
            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.info))
                .setMessage(getString(R.string.cannot_request_mesage))
                .setPositiveButton(getString(R.string.understand)) { _, _ ->
                    onBackPressedDispatcher.onBackPressed()
                }
                .setCancelable(false)
                .create()

            dialog.show()
        } catch (e:Exception){
            //
        }
    }
}