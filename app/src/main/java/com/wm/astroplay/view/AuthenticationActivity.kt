package com.wm.astroplay.view

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.wm.astroplay.R
import com.wm.astroplay.databinding.ActivityAuthenticationBinding
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.viewmodel.AuthenticationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AuthenticationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthenticationBinding
    private val viewModel: AuthenticationViewModel by viewModels()
    private val GOOGLE = 100
    private lateinit var auth: FirebaseAuth
    private lateinit var userPreferences: UserPreferences
    lateinit var account: GoogleSignInAccount
    val db = Firebase.firestore
    val TAG = "AstroDebug"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        CoroutineScope(Dispatchers.IO).launch {
            auth = Firebase.auth
        }
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnGoogle.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.web_client_id))
                    .requestEmail()
                    .build()
                val googleClient = GoogleSignIn.getClient(this@AuthenticationActivity, googleConf)
                googleClient.signOut()
                googleSignInLauncher.launch(googleClient.signInIntent)
                runOnUiThread {
                    binding.loading.isVisible = true
                }
            }
        }
    }

    private var googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    account = task.getResult(ApiException::class.java)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val userId = account.id
                                val userRef = db.collection("users").document(userId.toString())

                                userRef.get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                document.toObject(User::class.java)
                                                    ?.let { it1 ->
                                                        userPreferences.saveUser(it1)
                                                    }
                                                Log.d(
                                                    TAG,
                                                    "El usuario ya existe, no es necesario crearlo en Firestore"
                                                )
                                                runOnUiThread {
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
                                                userId,
                                                account.displayName,
                                                account.email,
                                                account.photoUrl.toString(),
                                                listOf(),
                                                1,
                                                false,
                                                System.currentTimeMillis()
                                            )
                                            userRef.set(user)
                                                .addOnSuccessListener { _ ->
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        userPreferences.saveUser(user)
                                                        Log.d(TAG, "Usuario agregado exitosamente")
                                                        runOnUiThread {
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
                                                    Log.w(TAG, "Error al agregar usuario", e)
                                                }
                                        }
                                    }
                            }
                        } else {
                            runOnUiThread {
                                binding.loading.isVisible = false
                            }
                            Toast.makeText(
                                this@AuthenticationActivity,
                                "Error al iniciar sesión.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    //
                }
            } else {
                runOnUiThread {
                    binding.loading.isVisible = false
                }
            }
        }
}