package com.wm.astroplay.view.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.constants.AnimationTypes
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wm.astroplay.R
import com.wm.astroplay.databinding.FragmentHomeBinding
import com.wm.astroplay.model.Movie
import com.wm.astroplay.model.MovieProvider
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.model.interfaces.FragmentNavigationListener
import com.wm.astroplay.view.MovieDetailActivity
import com.wm.astroplay.view.adapters.MovieAdapter
import com.wm.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MoviesViewModel by viewModels()
    private lateinit var userPreferences: UserPreferences
    private var navigationListener: FragmentNavigationListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentNavigationListener) {
            navigationListener = context
        } else {
            //throw RuntimeException("$context must implement FragmentNavigationListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        navigationListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        userPreferences = UserPreferences(this.requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            //viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val user = userPreferences.getUser().firstOrNull()
                withContext(Dispatchers.Main) {
                    setupViews(user)
                }
                viewModel.getUserNotifications(user?.id ?: "")
                viewModel.init()
           // }
        }

        return binding.root
    }

    private fun setupViews(user: User?) {
        Glide.with(this@HomeFragment)
            .load(user?.photo)
            .circleCrop()
            .error(R.drawable.default_user)
            .into(binding.profileImg)

        binding.welcomeText.text = getString(
            R.string.home_welcome_text,
            user?.name?.split(" ")?.get(0) ?: "Usuario"
        )

        val quotes = MovieProvider.getQuotes()
        val randomIndex = Random.nextInt(quotes.size)
        binding.randomText.text = quotes[randomIndex]
        setupClickListeners()

        setupSwipeRefresh()

        observeViewModel()
    }

    private fun setupImageSlider(movies: List<Movie>){
        if(movies.isNotEmpty()){
            val imageList = ArrayList<SlideModel>() // Create image list
            movies.map { movie ->
                imageList.add(SlideModel(movie.banner, "Estreno: ${movie.title}"))
            }

            binding.imageSlider.setImageList(imageList, ScaleTypes.FIT)

            binding.imageSlider.setItemClickListener(object : ItemClickListener {
                override fun onItemSelected(position: Int) {
                    val intent = Intent(this@HomeFragment.requireContext(), MovieDetailActivity::class.java)
                    intent.putExtra("movie", movies[position] as Parcelable)
                    startActivity(intent)
                }

                override fun doubleClick(position: Int) {
                    //
                } })
        } else {
            binding.imageSlider.isVisible = false
        }

    }

    private fun setupClickListeners() {
        binding.btnNotification.setOnClickListener {
            navigationListener?.onNavigateTo("notifications")
        }

        binding.profileImg.setOnClickListener {
            navigationListener?.onNavigateTo("profile")
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            launchDataLoad()
        }
    }

    private fun observeViewModel() {
        viewModel.apply {
            premiereMovieList.observe(viewLifecycleOwner){ movies ->
                setupImageSlider(movies)
            }

            popularMovieList.observe(viewLifecycleOwner) { movies ->
                setupRecyclerView(binding.popularMoviesView, binding.popularMoviesViewLoading, movies)
            }

            ratedMovieList.observe(viewLifecycleOwner) { movies ->
                setupRecyclerView(binding.ratedMoviesView, binding.ratedMoviesViewLoading, movies)
            }

            recentMovieList.observe(viewLifecycleOwner) { movies ->
                setupRecyclerView(binding.recentMoviesView, binding.recentMoviesViewLoading, movies)
            }

            randomMovieList.observe(viewLifecycleOwner) { movies ->
                setupRecyclerView(binding.randomMoviesView, binding.randomMoviesViewLoading, movies)
            }

            userNotifications.observe(viewLifecycleOwner) { notifications ->
                binding.notifyIcon.isVisible = notifications.isNotEmpty()
            }
        }
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView, loadingView: ShimmerFrameLayout, movies: List<Movie>
    ) {
        val movieAdapter = MovieAdapter(movies)
        recyclerView.apply {
            adapter = movieAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            isVisible = true
        }
        loadingView.isVisible = false
    }

    private fun launchDataLoad() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                viewModel.fetchPremiereMovies()
                viewModel.fetchPopularMovies()
                viewModel.fetchRecentsMovies()
                viewModel.fetchRandomMovies()
                viewModel.fetchRatedMovies()
            } catch (e: Exception) {
                //Log.e("SaludDebug", e.message.toString())
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

}