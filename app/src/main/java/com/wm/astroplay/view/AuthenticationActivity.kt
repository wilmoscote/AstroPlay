package com.wm.astroplay.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wm.astroplay.R
import com.wm.astroplay.databinding.ActivityAuthenticationBinding
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.viewmodel.AuthenticationViewModel
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AuthenticationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthenticationBinding
    private val viewModel: AuthenticationViewModel by viewModels()
    private val GOOGLE = 100
    private lateinit var auth: FirebaseAuth
    private lateinit var userPreferences: UserPreferences
    lateinit var account: GoogleSignInAccount
    private var deviceId: String? = null
    val db = Firebase.firestore
    val TAG = "AstroDebug"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        auth = Firebase.auth
        deviceId = getDeviceId()

        lifecycleScope.launch(Dispatchers.IO) {
            checkDeviceBlocked()
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnGoogle.setOnClickListener {
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()
            val googleClient = GoogleSignIn.getClient(this@AuthenticationActivity, googleConf)
            googleClient.signOut()

            lifecycleScope.launch(Dispatchers.Main) {
                googleSignInLauncher.launch(googleClient.signInIntent)
                binding.loading.isVisible = true
            }
        }
    }

    private var googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            lifecycleScope.launch {
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        account = task.getResult(ApiException::class.java)!!
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        auth.signInWithCredential(credential).addOnCompleteListener {
                            if (it.isSuccessful) {
                                val userId = account.id
                                val userRef = db.collection("users").document(userId.toString())

                                userRef.get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            lifecycleScope.launch {
                                                document.toObject(User::class.java)?.let { user ->
                                                    userPreferences.saveUser(user)
                                                }
                                                withContext(Dispatchers.Main) {
                                                    binding.loading.isVisible = false
                                                }
                                                startActivity(
                                                    Intent(
                                                        this@AuthenticationActivity,
                                                        MainActivity::class.java
                                                    )
                                                )
                                                finish()
                                            }
                                        } else {
                                            val user = User(
                                                userId ?: UUID.randomUUID().toString(),
                                                account.displayName ?: "User",
                                                account.email ?: "user@user.com",
                                                account.photoUrl.toString(),
                                                listOf(),
                                                1,
                                                deviceId ?: "",
                                                "",
                                                false,
                                                System.currentTimeMillis()
                                            )
                                            userRef.set(user)
                                                .addOnSuccessListener { _ ->
                                                    lifecycleScope.launch {
                                                        userPreferences.saveUser(user)
                                                        withContext(Dispatchers.Main) {
                                                            binding.loading.isVisible = false
                                                        }
                                                        startActivity(
                                                            Intent(
                                                                this@AuthenticationActivity,
                                                                MainActivity::class.java
                                                            )
                                                        )
                                                        finish()
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(
                                                        this@AuthenticationActivity,
                                                        "Error al iniciar sesión ${e.message.toString()}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    }
                            } else {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        binding.loading.isVisible = false
                                    }
                                    Toast.makeText(
                                        this@AuthenticationActivity,
                                        "Error al iniciar sesión ${it.exception?.message.toString()}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        //
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.loading.isVisible = false
                    }
                }
            }
        }


    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        val contentResolver = applicationContext.contentResolver
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private suspend fun checkDeviceBlocked() {
        db.collection("blockedDevices").whereEqualTo("deviceId", getDeviceId()).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    runOnUiThread {
                        try {
                            showBlockedDeviceInfo()
                        } catch (e: Exception) {
                            //
                        }
                    }
                }
            }
    }

    private fun showBlockedDeviceInfo() {
        try {
            Blurry.with(this)
                .radius(10)
                .sampling(8)
                .async()
                .onto(binding.root)
            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.blocked_device_title))
                .setMessage(getString(R.string.blocked_device_info))
                .setPositiveButton(getString(R.string.understand)) { _, _ ->
                    finishAffinity()
                }
                .setCancelable(false)
                .create()
            dialog.show()
        } catch (e:Exception){
            finishAffinity()
        }
    }
}