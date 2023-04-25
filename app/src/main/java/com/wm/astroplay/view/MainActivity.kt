package com.wm.astroplay.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.wm.astroplay.BuildConfig

import com.wm.astroplay.R
import com.wm.astroplay.databinding.ActivityMainBinding
import com.wm.astroplay.model.Movie
import com.wm.astroplay.model.MovieProvider
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.model.interfaces.FragmentNavigationListener
import com.wm.astroplay.view.fragments.*
import com.wm.astroplay.viewmodel.AuthenticationViewModel
import com.wm.astroplay.viewmodel.MoviesViewModel
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainActivity : AppCompatActivity(), FragmentNavigationListener {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MoviesViewModel by viewModels()
    private val userViewModel: AuthenticationViewModel by viewModels()
    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val favoritesFragment = FavoritesFragment()
    private val exploreFragment = ExploreFragment()
    private val notificationsFragment = NotificationsFragment()
    private val db = Firebase.firestore
    private lateinit var userPreferences: UserPreferences
    var doubleBackToExitPressedOnce = false
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object {
        val TAG = "AstroDebug"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            //
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        firebaseAnalytics = Firebase.analytics
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        askNotificationPermission()
        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.getUser().collect { user ->
                try {
                    listenToUserChanges(user?.id ?: "")
                    updateUserToken(user?.email ?: "")
                } catch (e: Exception) {
                    //
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, homeFragment).commit()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homePage -> {
                    if (item.itemId != binding.bottomNavigation.selectedItemId) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, homeFragment).commit()
                    }
                }
                R.id.favoritesPage -> {
                    if (item.itemId != binding.bottomNavigation.selectedItemId) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, favoritesFragment).commit()

                        val parameters = Bundle().apply {
                            this.putString("action", "screen")
                        }
                        firebaseAnalytics.logEvent("menu_favorites", parameters)
                    }
                }
                R.id.explorePage -> {
                    if (item.itemId != binding.bottomNavigation.selectedItemId) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, exploreFragment).commit()
                        val parameters = Bundle().apply {
                            this.putString("action", "screen")
                        }
                        firebaseAnalytics.logEvent("menu_explore", parameters)
                    }
                }
                R.id.profilePage -> {
                    if (item.itemId != binding.bottomNavigation.selectedItemId) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, profileFragment).commit()
                        val parameters = Bundle().apply {
                            this.putString("action", "screen")
                        }
                        firebaseAnalytics.logEvent("menu_profile", parameters)
                    }
                }
            }
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finish()
                    return
                }
                doubleBackToExitPressedOnce = true
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.press_twice_text),
                    Toast.LENGTH_LONG
                ).show()
                Handler(Looper.getMainLooper()).postDelayed(kotlinx.coroutines.Runnable {
                    doubleBackToExitPressedOnce = false
                }, 5000)
            }
        })
        lifecycleScope.launch(Dispatchers.IO) {
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this@MainActivity) { task ->
                    if (task.isSuccessful) {
                        val appVersion = Firebase.remoteConfig.getDouble("version")
                        val appDisabled = Firebase.remoteConfig.getBoolean("app_disabled")
                      //  Log.d("AstroDebug", "Version Server: $appVersion")
                       // Log.d("AstroDebug", "Is Disabled?: ${appDisabled.toString()}")
                       // Log.d(TAG, appVersion.toString())
                        if (appVersion.toInt() > BuildConfig.VERSION_CODE) {
                            forceUpdate()
                        }
                      //  Log.d(TAG, appDisabled.toString())
                        if (appDisabled) {
                            showAppDisableDialog()
                        }
                    }

                }
            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    //Log.d(TAG, "Updated keys: " + configUpdate.updatedKeys);

                    if (configUpdate.updatedKeys.contains("app_disabled")) {
                        remoteConfig.activate().addOnCompleteListener {
                            val appDisabled = Firebase.remoteConfig.getBoolean("app_disabled")
                            if (appDisabled) {
                                showAppDisableDialog()
                            }
                        }
                    }

                    if (configUpdate.updatedKeys.contains("version")) {
                        remoteConfig.activate().addOnCompleteListener {
                            val appVersion = Firebase.remoteConfig.getDouble("version")
                            if (appVersion.toInt() > BuildConfig.VERSION_CODE) {
                                forceUpdate()
                            }
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    //Log.w(TAG, "Config update error with code: " + error.code, error)
                }
            })
            checkDeviceBlocked()
        }

        try {
            lifecycleScope.launch {
                handleDeepLink(intent)
            }
        } catch (e: Exception) {
            //
        }
    }

    private fun updateUserToken(email:String) {
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getFcmToken().collect { token ->
                try {
                    userViewModel.updateUserToken(email, token ?: "")
                } catch (e: Exception) {
                    //
                }
            }
        }
    }

    private fun listenToUserChanges(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                   // Log.w(TAG, "Aplicando cambios ", error)
                    CoroutineScope(Dispatchers.IO).launch {
                        userPreferences.saveUser(user)
                    }
                    if (user.disabled == true) {
                        try {
                            showAccountDisabledDialog()
                        } catch (e: Exception) {
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun showAccountDisabledDialog() {
        try {
            Blurry.with(this)
                .radius(10)
                .sampling(8)
                .async()
                .onto(binding.root)
            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.disabled_account_title))
                .setMessage(getString(R.string.disabled_account_message))
                .setPositiveButton(getString(R.string.understand)) { _, _ ->
                    Firebase.auth.signOut()
                    CoroutineScope(Dispatchers.IO).launch {
                        userPreferences.clearDataStore()
                        startActivity(Intent(this@MainActivity, AuthenticationActivity::class.java))
                        finish()
                    }
                }
                .setCancelable(false)
                .create()

            dialog.show()
        } catch (e: Exception) {
            //
        }
    }

    private fun forceUpdate() {
        Blurry.with(this)
            .radius(10)
            .sampling(8)
            .async()
            .onto(binding.root)
        val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            .setTitle(getString(R.string.update_app_title))
            .setMessage(getString(R.string.update_string))
        dialog.setCancelable(false)
        dialog.setPositiveButton(
            getString(R.string.but_update)
        ) { dlg, _ ->
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            }
            finish()
        }
        dialog.show()
    }

    private fun showAppDisableDialog() {
        Blurry.with(this)
            .radius(10)
            .sampling(8)
            .async()
            .onto(binding.root)
        val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            .setTitle(getString(R.string.app_disabled_title))
            .setMessage(getString(R.string.app_disabled_message))
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                //
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNavigateTo(fragment: String) {
        when (fragment) {
            "notifications" -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, notificationsFragment).commit()
            }
            "home" -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFragment).commit()
            }
            "profile" -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, profileFragment).commit()
                binding.bottomNavigation.selectedItemId = R.id.profilePage
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
                        showBlockedDeviceInfo()
                    }
                }
            }
    }

    private fun showBlockedDeviceInfo() {
        Blurry.with(this)
            .radius(10)
            .sampling(8)
            .async()
            .onto(binding.root)
        val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            .setTitle(getString(R.string.blocked_device_title))
            .setMessage(getString(R.string.blocked_device_info))
            .setPositiveButton(getString(R.string.understand)) { _, _ ->
                Firebase.auth.signOut()
                CoroutineScope(Dispatchers.IO).launch {
                    userPreferences.clearDataStore()
                    finishAffinity()
                }
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private suspend fun handleDeepLink(intent: Intent?) {
        //Log.d("AstroDebug", "Looking Movie!")
        val deepLink: Uri? = intent?.data
        val movieId = deepLink?.getQueryParameter("id")
        //Log.d("AstroDebug", "Movie id: $movieId")

        if (movieId != null) {
            viewModel.getMovieFromFirestore(movieId).catch { e ->
                // Handle error
            }.collect { movie ->
                openMovieDetailActivity(movie)
            }
        }
    }

    private fun openMovieDetailActivity(movie: Movie) {
        val intent = Intent(this, MovieDetailActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("movie", movie)
        startActivity(intent)
    }
}