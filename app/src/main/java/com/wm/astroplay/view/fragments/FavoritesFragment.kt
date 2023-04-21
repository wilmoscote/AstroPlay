package com.wm.astroplay.view.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.wm.astroplay.R
import com.wm.astroplay.databinding.FragmentFavoritesBinding
import com.wm.astroplay.model.Movie
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.view.adapters.MovieAdapter
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loadFavorites()
            }
        }

        return binding.root
    }

    private suspend fun loadFavorites() {
        userPreferences.getFavorites().collect { favorites ->
            withContext(Dispatchers.Main) {
                try {
                    setupFavoritesView(favorites)
                } catch (e: Exception) {
                    // Handle the exception
                }
            }
        }
    }

    private fun setupFavoritesView(favorites: List<Movie>) {
        binding.notFavorites.isVisible = favorites.isEmpty()
        val favoritesAdapter = MovieAdapter(favorites)
        binding.favoritesMovieView.apply {
            adapter = favoritesAdapter
            layoutManager = GridLayoutManager(
                this@FavoritesFragment.activity?.applicationContext,
                2
            )
        }
    }


}