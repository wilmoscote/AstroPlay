package com.wmsoftware.astroplay.view.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.databinding.FragmentExploreBinding
import com.wmsoftware.astroplay.model.MovieProvider
import com.wmsoftware.astroplay.view.adapters.CategoryAdapter


class ExploreFragment : Fragment() {
    private lateinit var binding: FragmentExploreBinding
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
        val categoryAdapter = CategoryAdapter(MovieProvider.getCategories()) { category ->
            // Manejar el evento de clic aquí (por ejemplo, navegar a una nueva pantalla con películas de esta categoría)
        }

        // Configura el RecyclerView
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = categoryAdapter
        }

        // Maneja la acción de búsqueda en el teclado
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                //performSearch()
                true
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
        binding.searchInputLayout.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(300).setListener(null)
        }
    }

    private fun hideSearch() {
        binding.searchInputLayout.apply {
            animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
        }
    }

    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            // Realiza la búsqueda aquí
            // ...

            // Oculta el teclado después de realizar la búsqueda
            val inputMethodManager =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        }
    }
}