package com.wmsoftware.astroplay.view.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.databinding.FragmentHomeBinding
import com.wmsoftware.astroplay.model.MovieProvider
import com.wmsoftware.astroplay.model.UserPreferences
import com.wmsoftware.astroplay.view.adapters.MovieAdapter
import com.wmsoftware.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MoviesViewModel by viewModels()
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        userPreferences = UserPreferences(this.requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUser().collect { user ->
                withContext(Dispatchers.Main){
                    try {
                        Glide.with(this@HomeFragment).load(user?.photo).circleCrop()
                            .error(R.drawable.default_user).into(binding.profileImg)
                        binding.welcomeText.text = getString(
                            R.string.home_welcome_text,
                            user?.name?.split(" ")?.get(0) ?: "Usuario"
                        )
                        val quotes = MovieProvider.getQuotes()
                        val randomIndex = Random.nextInt(quotes.size)
                        binding.randomText.text = quotes[randomIndex]
                    } catch (e:Exception){
                        //
                    }
                }
            }
        }
        viewModel.init()
        binding.btnNotification.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                //viewModel.saveMoviesToFirestore()
            }
        }

        binding.textSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.homeFeedLayout.isVisible = p0.isNullOrBlank()
                binding.searchMovieLayout.isVisible = !p0.isNullOrBlank()
            }

            override fun afterTextChanged(p0: Editable?) { }
        })

            binding.textSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val searchTerm = binding.textSearch.text.toString()

                    // Realiza la búsqueda utilizando corutinas
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.searchMovies(searchTerm)
                    }

                    // Oculta el teclado después de realizar la búsqueda
                    val imm = this.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.textSearch.windowToken, 0)

                    // Devuelve 'true' para indicar que se ha manejado la acción
                    return@setOnEditorActionListener true
                }

                // Devuelve 'false' para que el sistema maneje la acción predeterminada
                false
            }

        binding.swipeRefresh.setOnRefreshListener {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.fetchPopularMovies()
                    viewModel.fetchRecentsMovies()
                    viewModel.fetchRandomMovies()
                }
            } catch (e: Exception) {
                //Log.e("SaludDebug", e.message.toString())
            }
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.searching.observe(viewLifecycleOwner){
            binding.searching.isVisible = it
        }

        viewModel.searchResult.observe(viewLifecycleOwner){ movies ->
            val movieResultAdapter = MovieAdapter(movies) // Reemplaza 'listOf()' con tus datos de películas
            binding.moviesResultView.adapter = movieResultAdapter
            binding.moviesResultView.layoutManager = GridLayoutManager(this.requireContext(), 2) // El segundo parámetro es el número de columnas
        }
        viewModel.popularMovieList.observe(viewLifecycleOwner) { movies ->
            val movieAdapter = MovieAdapter(movies)
            binding.popularMoviesView.adapter = movieAdapter
            binding.popularMoviesView.layoutManager =
                LinearLayoutManager(this.requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.popularMoviesView.isVisible = true
            binding.popularMoviesViewLoading.isVisible = false
        }

        viewModel.recentMovieList.observe(viewLifecycleOwner) { movies ->
            val movieAdapter = MovieAdapter(movies)
            binding.recentMoviesView.adapter = movieAdapter
            binding.recentMoviesView.layoutManager =
                LinearLayoutManager(this.requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.recentMoviesView.isVisible = true
            binding.recentMoviesViewLoading.isVisible = false
        }

        viewModel.randomMovieList.observe(viewLifecycleOwner){ movies ->
            val randomMovieAdapter = MovieAdapter(movies)
            binding.randomMoviesView.adapter = randomMovieAdapter
            binding.randomMoviesView.layoutManager =
                LinearLayoutManager(this.requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.randomMoviesView.isVisible = true
            binding.randomMoviesViewLoading.isVisible = false
        }


        return binding.root
    }
}