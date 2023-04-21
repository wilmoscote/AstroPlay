package com.wm.astroplay.view.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.wm.astroplay.R
import com.wm.astroplay.databinding.FragmentExploreBinding
import com.wm.astroplay.model.MovieProvider
import com.wm.astroplay.model.interfaces.OnCategorySelectedListener
import com.wm.astroplay.view.adapters.CategoryAdapter
import com.wm.astroplay.view.adapters.MovieAdapter
import com.wm.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ExploreFragment : Fragment(), OnCategorySelectedListener {
    private lateinit var binding: FragmentExploreBinding
    private val viewModel: MoviesViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExploreBinding.inflate(layoutInflater)

        setupSearchButton()

        setupRecyclerView()

        setupSearchResultObserver()

        setupSearchGenreResultObserver()

        setupSearchingObservers()

        setupSearchEditText()

        setupBackButton()

        return binding.root
    }

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener {
            toggleSearch()
        }
    }

    private fun setupRecyclerView() {
        val categoryAdapter = CategoryAdapter(MovieProvider.getCategories(), this@ExploreFragment)
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = categoryAdapter
        }
    }

    private fun setupSearchResultObserver() {
        viewModel.searchResult.observe(viewLifecycleOwner) { movies ->
            val movieResultAdapter = MovieAdapter(movies)
            binding.moviesResultView.apply {
                adapter = movieResultAdapter
                layoutManager = GridLayoutManager(this@ExploreFragment.requireContext(), 2)
            }
        }
    }

    private fun setupSearchGenreResultObserver() {
        viewModel.searchGenreResult.observe(viewLifecycleOwner) { movies ->
            val movieGenreResultAdapter = MovieAdapter(movies)
            binding.moviesResultGenreView.apply {
                adapter = movieGenreResultAdapter
                layoutManager = GridLayoutManager(this@ExploreFragment.requireContext(), 2)
            }
        }
    }

    private fun setupSearchingObservers() {
        viewModel.searching.observe(viewLifecycleOwner) {
            binding.searching.isVisible = it
        }
        viewModel.searchingGenre.observe(viewLifecycleOwner) {
            binding.searchingGenre.isVisible = it
        }
    }

    private fun setupSearchEditText() {
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchTerm = binding.searchEditText.text.toString()
                performSearch(searchTerm)
                hideKeyboard(binding.searchEditText)
                return@setOnEditorActionListener true
            } else {
                false
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBackSearch.setOnClickListener {
            binding.rvCategories.isVisible = true
            binding.exploreTitle.isVisible = true
            binding.btnSearch.isVisible = true
            binding.searchGenreLayout.isVisible = false
        }
    }

    private fun performSearch(searchTerm: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchMovies(searchTerm)
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = this.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    private fun toggleSearch() {
        val isSearchVisible = binding.searchInputLayout.visibility == View.VISIBLE
        // Cambiar el icono con animación
        val rotation = if (!isSearchVisible) 90f else -90f
        binding.btnSearch.animate().rotationBy(rotation).setDuration(200).start()

        // Cambiar el icono del botón de búsqueda
        val newIcon = if (!isSearchVisible) R.drawable.ic_close else R.drawable.ic_search
        binding.btnSearch.setImageResource(newIcon)
        if (isSearchVisible) {
            hideSearch()
        } else {
            showSearch()
        }
    }

    private fun showSearch() {
        binding.searchMovieLayout.isVisible = true
        binding.rvCategories.isVisible = false
        binding.searchInputLayout.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(300).setListener(null)
        }
    }

    private fun hideSearch() {
        binding.searchMovieLayout.isVisible = false
        binding.rvCategories.isVisible = true
        binding.searchInputLayout.apply {
            animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
        }
    }

    override fun onCategorySelected(category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.searchMoviesByGenre(category)
            withContext(Dispatchers.Main){
                binding.rvCategories.isVisible = false
                binding.exploreTitle.isVisible = false
                binding.btnSearch.isVisible = false
                binding.searchGenreLayout.isVisible = true
                binding.searchGenreTitle.text = category
            }
        }
    }
}