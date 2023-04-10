package com.wmsoftware.astroplay.view

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.wmsoftware.astroplay.BuildConfig

import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.databinding.ActivityMainBinding
import com.wmsoftware.astroplay.model.Movie
import com.wmsoftware.astroplay.model.MovieProvider
import com.wmsoftware.astroplay.model.User
import com.wmsoftware.astroplay.model.UserPreferences
import com.wmsoftware.astroplay.view.fragments.ExploreFragment
import com.wmsoftware.astroplay.view.fragments.FavoritesFragment
import com.wmsoftware.astroplay.view.fragments.HomeFragment
import com.wmsoftware.astroplay.view.fragments.ProfileFragment
import com.wmsoftware.astroplay.viewmodel.MoviesViewModel
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MoviesViewModel by viewModels()
    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val favoritesFragment = FavoritesFragment()
    private val exploreFragment = ExploreFragment()
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
                } catch (e:Exception){
                    //
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, homeFragment).commit()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId){
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
                        firebaseAnalytics.logEvent("menu_favorites",parameters)
                    }
                }
                R.id.explorePage -> {
                    if (item.itemId != binding.bottomNavigation.selectedItemId) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, exploreFragment).commit()
                        val parameters = Bundle().apply {
                            this.putString("action", "screen")
                        }
                        firebaseAnalytics.logEvent("menu_explore",parameters)
                    }
                }
                R.id.profilePage -> {
                    if (item.itemId != binding.bottomNavigation.selectedItemId) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, profileFragment).commit()
                        val parameters = Bundle().apply {
                            this.putString("action", "screen")
                        }
                        firebaseAnalytics.logEvent("menu_profile",parameters)
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

        CoroutineScope(Dispatchers.IO).launch {
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this@MainActivity) { task ->
                    if (task.isSuccessful) {
                        val appVersion = Firebase.remoteConfig.getDouble("version")
                        //Log.d(TAG, appVersion.toString())
                        if (appVersion.toInt() > BuildConfig.VERSION_CODE) {
                            forceUpdate()
                        }
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
                    Log.w(TAG, "Aplicando cambios ", error)
                    CoroutineScope(Dispatchers.IO).launch{
                        userPreferences.saveUser(user)
                    }
                    if (user.disabled == true){

                        showAccountDisabledDialog()
                    }
                }
            }
        }
    }

    private fun showAccountDisabledDialog() {
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
}