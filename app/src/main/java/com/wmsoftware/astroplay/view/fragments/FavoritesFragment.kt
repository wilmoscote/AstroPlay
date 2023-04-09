package com.wmsoftware.astroplay.view.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.databinding.FragmentFavoritesBinding
import com.wmsoftware.astroplay.model.UserPreferences
import com.wmsoftware.astroplay.view.adapters.MovieAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FavoritesFragment : Fragment() {
    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(layoutInflater)
        userPreferences = UserPreferences(this.requireContext())
        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.getFavorites().collect { favorites ->
                withContext(Dispatchers.Main) {
                    try {
                        binding.notFavorites.isVisible = favorites.isEmpty()
                        val favoritesAdapter =
                            MovieAdapter(favorites) // Reemplaza 'listOf()' con tus datos de películas
                        binding.favoritesMovieView.adapter = favoritesAdapter
                        binding.favoritesMovieView.layoutManager = GridLayoutManager(
                            this@FavoritesFragment.activity?.applicationContext,
                            2
                        )
                    } catch (e: Exception) {
                        //
                    }
                }
            }
        }

        return binding.root
    }

}