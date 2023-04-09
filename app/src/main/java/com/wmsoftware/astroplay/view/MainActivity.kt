package com.wmsoftware.astroplay.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.databinding.ActivityMainBinding
import com.wmsoftware.astroplay.model.Movie
import com.wmsoftware.astroplay.view.fragments.ExploreFragment
import com.wmsoftware.astroplay.view.fragments.FavoritesFragment
import com.wmsoftware.astroplay.view.fragments.HomeFragment
import com.wmsoftware.astroplay.view.fragments.ProfileFragment
import com.wmsoftware.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MoviesViewModel by viewModels()
    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val favoritesFragment = FavoritesFragment()
    private val exploreFragment = ExploreFragment()
    companion object {
        val TAG = "AstroDebug"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CoroutineScope(Dispatchers.IO).launch {
            val user = Firebase.auth.currentUser
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
                    }
                }
                R.id.explorePage -> {
                    if (item.itemId != binding.bottomNavigation.selectedItemId) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, exploreFragment).commit()
                    }
                }
                R.id.profilePage -> {
                    if (item.itemId != binding.bottomNavigation.selectedItemId) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, profileFragment).commit()
                    }
                }
            }
            true
        }
    }


}