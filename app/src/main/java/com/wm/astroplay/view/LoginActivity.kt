package com.wm.astroplay.view

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wm.astroplay.BuildConfig
import com.wm.astroplay.R
import com.wm.astroplay.databinding.ActivityLoginBinding
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userPreferences: UserPreferences
    val db = Firebase.firestore
    private var isRegister = false
    private var deviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        userPreferences = UserPreferences(this)
        deviceId = getDeviceId()

        binding.versionInfo.setOnLongClickListener {
            Toast.makeText(this, "\uD83D\uDC9B\uD83D\uDC99❤️", Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.txtEmail.text.toString().trim()
            val password = binding.txtPass.text.toString().trim()

            if (email.isEmpty() || !isValidEmail(email)) {
                binding.txtEmail.error = getString(R.string.email_error)
                binding.txtEmail.requestFocus()
            } else if (password.isEmpty()) {
                binding.txtPass.error = getString(R.string.pass_error)
                binding.txtPass.requestFocus()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        binding.loading.isVisible = true
                    }
                    signIn(email, password)
                }
            }
        }

        binding.btnLoginBack.setOnClickListener {
            isRegister = false
            binding.layoutLogin.isVisible = true
            binding.layoutRegister.isVisible = false
        }

        binding.btnSendRegister.setOnClickListener {
            val email = binding.txtEmailRegister.text.toString().trim()
            val password = binding.txtPassRegister.text.toString().trim()
            val confirmPassword = binding.txtPassConfirmRegister.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (isValidEmail(email)) {
                    if (password == confirmPassword) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            registerUser(email, password)
                            withContext(Dispatchers.Main) {
                                binding.loading.isVisible = true
                            }
                        }
                    } else {
                        binding.txtPassConfirmRegister.error = getString(R.string.pass_confirm_error)
                        binding.txtPassConfirmRegister.requestFocus()
                    }
                } else {
                    binding.txtEmailRegister.error = getString(R.string.email_error)
                    binding.txtEmailRegister.requestFocus()
                }
            } else {
                Toast.makeText(this@LoginActivity, getString(R.string.empty_fields_error), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegister.setOnClickListener {
            isRegister = true
            binding.layoutLogin.isVisible = false
            binding.layoutRegister.isVisible = true
        }

        binding.versionInfo.text = getString(R.string.version_info, BuildConfig.VERSION_NAME)
    }


    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z\\d._%+-]+@[A-Za-z\\d.-]+\\.[A-Z]{2,6}$"
        return email.matches(Regex(emailPattern, RegexOption.IGNORE_CASE))
    }

    private suspend fun signIn(email: String, password: String) {
        withContext(Dispatchers.IO){
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val account = task.result.user
                            val userId = task.result.user?.uid
                            val userRef = db.collection("users").document(userId.toString())

                            userRef.get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            document.toObject(User::class.java)
                                                ?.let { it1 ->
                                                    userPreferences.saveUser(it1)
                                                }
                                            runOnUiThread {
                                                binding.loading.isVisible = false
                                            }
                                            startActivity(
                                                Intent(
                                                    this@LoginActivity,
                                                    MainActivity::class.java
                                                )
                                            )
                                            finish()
                                        }
                                    } else {
                                        val user = User(
                                            userId,
                                            account?.displayName ?: "Usuario",
                                            account?.email,
                                            (account?.photoUrl ?: "https://firebasestorage.googleapis.com/v0/b/astroplay.appspot.com/o/profiles%2Fdefault_user.webp?alt=media&token=706cc7a0-4e4d-4d81-b914-99e6c946ed30").toString(),
                                            listOf(),
                                            1,
                                            deviceId ?: "",
                                            "",
                                            false,
                                            System.currentTimeMillis()
                                        )
                                        userRef.set(user)
                                            .addOnSuccessListener {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    document.toObject(User::class.java)
                                                        ?.let { it1 -> userPreferences.saveUser(it1) }

                                                    runOnUiThread {
                                                        binding.loading.isVisible = false
                                                    }
                                                    startActivity(
                                                        Intent(
                                                            this@LoginActivity,
                                                            MainActivity::class.java
                                                        )
                                                    )
                                                    finishAffinity()
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                               // Log.w("AstroDebug", "Error al agregar usuario", e)
                                            }
                                    }
                                }
                        }
                    } else {
                        runOnUiThread {
                            binding.loading.isVisible = false
                            Toast.makeText(this@LoginActivity,getString(R.string.login_error,task.exception?.message),Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    private suspend fun registerUser(email: String, password: String) {
        withContext(Dispatchers.IO){
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val account = task.result.user
                            val userId = task.result.user?.uid
                            val userRef = db.collection("users").document(userId.toString())

                            userRef.get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            document.toObject(User::class.java)
                                                ?.let { it1 ->
                                                    userPreferences.saveUser(it1)
                                                }
                                            runOnUiThread {
                                                binding.loading.isVisible = false
                                            }
                                            startActivity(
                                                Intent(
                                                    this@LoginActivity,
                                                    MainActivity::class.java
                                                )
                                            )
                                            finish()
                                        }
                                    } else {
                                        val user = User(
                                            userId,
                                            account?.displayName ?: "Usuario",
                                            account?.email,
                                            (account?.photoUrl ?: "https://firebasestorage.googleapis.com/v0/b/astroplay.appspot.com/o/profiles%2Fdefault_user.webp?alt=media&token=706cc7a0-4e4d-4d81-b914-99e6c946ed30").toString(),
                                            listOf(),
                                            1,
                                            deviceId ?: "",
                                            "",
                                            false,
                                            System.currentTimeMillis()
                                        )
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            userPreferences.saveUser(user)
                                        }
                                        userRef.set(user)
                                            .addOnSuccessListener {
                                                lifecycleScope.launch(Dispatchers.IO) {
                                                    document.toObject(User::class.java)
                                                        ?.let { it1 ->
                                                            userPreferences.saveUser(it1)
                                                        }

                                                    runOnUiThread {
                                                        binding.loading.isVisible = false
                                                    }
                                                    startActivity(
                                                        Intent(
                                                            this@LoginActivity,
                                                            MainActivity::class.java
                                                        )
                                                    )
                                                    finishAffinity()
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                //Log.w("AstroDebug", "Error al agregar usuario", e)
                                            }
                                    }
                                }
                        }
                    } else {
                        runOnUiThread {
                            binding.loading.isVisible = false
                            Toast.makeText(this@LoginActivity,getString(R.string.user_created_error,task.exception?.message),Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        val contentResolver = applicationContext.contentResolver
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

}