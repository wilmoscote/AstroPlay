package com.wmsoftware.astroplay.view.fragments

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.databinding.FragmentExploreBinding
import com.wmsoftware.astroplay.model.MovieProvider
import com.wmsoftware.astroplay.model.interfaces.OnCategorySelectedListener
import com.wmsoftware.astroplay.view.adapters.CategoryAdapter
import com.wmsoftware.astroplay.view.adapters.MovieAdapter
import com.wmsoftware.astroplay.viewmodel.MoviesViewModel
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
        // Maneja el evento de clic del botón de búsqueda
        binding.btnSearch.setOnClickListener {
            toggleSearch()
        }

        // Crea el adaptador del RecyclerView
        val categoryAdapter = CategoryAdapter(MovieProvider.getCategories(), this@ExploreFragment)

        // Configura el RecyclerView
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = categoryAdapter
        }

        viewModel.searchResult.observe(viewLifecycleOwner){ movies ->
            val movieResultAdapter = MovieAdapter(movies) // Reemplaza 'listOf()' con tus datos de películas
            binding.moviesResultView.adapter = movieResultAdapter
            binding.moviesResultView.layoutManager = GridLayoutManager(this.requireContext(), 2) // El segundo parámetro es el número de columnas
        }
        viewModel.searching.observe(viewLifecycleOwner){
            binding.searching.isVisible = it
        }
        viewModel.searchingGenre.observe(viewLifecycleOwner){
            binding.searchingGenre.isVisible = it
        }
        viewModel.searchGenreResult.observe(viewLifecycleOwner){ movies ->
            val movieGenreResultAdapter = MovieAdapter(movies) // Reemplaza 'listOf()' con tus datos de películas
            binding.moviesResultGenreView.adapter = movieGenreResultAdapter
            binding.moviesResultGenreView.layoutManager = GridLayoutManager(this.requireContext(), 2) // El segundo parámetro es el número de columnas
        }

        binding.btnBackSearch.setOnClickListener {
            binding.rvCategories.isVisible = true
            binding.exploreTitle.isVisible = true
            binding.btnSearch.isVisible = true
            binding.searchGenreLayout.isVisible = false
        }
        // Maneja la acción de búsqueda en el teclado
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchTerm = binding.searchEditText.text.toString()

                // Realiza la búsqueda utilizando corutinas
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.searchMovies(searchTerm)
                }

                // Oculta el teclado después de realizar la búsqueda
                val imm = this.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)

                // Devuelve 'true' para indicar que se ha manejado la acción
                return@setOnEditorActionListener true
            } else {
                false
            }
        }

        return binding.root
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
        Log.d("AstroDebug","Realizar busqueda de: $category en Fragment!")
    }
}