package com.wmsoftware.astroplay.view

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.databinding.ActivityMovieDetailBinding
import com.wmsoftware.astroplay.model.Movie
import com.wmsoftware.astroplay.model.User
import com.wmsoftware.astroplay.model.UserPreferences
import com.wmsoftware.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovieDetailActivity : AppCompatActivity() {
    private val viewModel: MoviesViewModel by viewModels()
    private lateinit var binding: ActivityMovieDetailBinding
    private var imageExpanded = false
    private lateinit var userPreferences: UserPreferences
    private var user: User? = null
    private var isFavorite = false
    private var userFavorites = mutableListOf<Movie>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        // Recibe el objeto de la película desde el Intent
        val movie = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("movie", Movie::class.java)
        } else {
            intent.getParcelableExtra<Movie>("movie")
        }
        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.getUser().collect { currentUser ->
                user = currentUser

            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.getFavorites().collect { favorites ->
                userFavorites = favorites.toMutableList()
                isFavorite = favorites.any { it.title == (movie?.title) }
                Log.d(
                    "AstroDebug",
                    "isFavorite ${isFavorite.toString()} Favorite list: ${favorites.toString()}"
                )
                if (isFavorite) binding.btnFavorite.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@MovieDetailActivity,
                        R.drawable.ic_favorite_added
                    )
                )
            }
        }

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val layoutParams = binding.appBar.layoutParams
        layoutParams.height = screenHeight / 2
        binding.appBar.layoutParams = layoutParams

        Glide.with(this)
            .load(movie?.poster)
            .transition(
                DrawableTransitionOptions.withCrossFade())
            .into(binding.moviePoster)

        binding.moviePoster.setOnClickListener {
            imageExpanded = !imageExpanded
            if (imageExpanded) {
                layoutParams.height = screenHeight
                binding.appBar.layoutParams = layoutParams
            } else {
                layoutParams.height = screenHeight / 2
                binding.appBar.layoutParams = layoutParams
            }
        }

        // Establece la información de la película en los elementos de la vista
        binding.movieTitle.text = movie?.title
        binding.txtAge.text = movie?.ageRating
        binding.txtRate.text = movie?.imdbRating
        binding.txtTime.text = movie?.runtime
        binding.txtYear.text = movie?.year.toString()

        // Une los géneros en una sola cadena para mostrarlos en la vista
        val genres = movie?.genre?.joinToString(", ") ?: "N/A"
        binding.movieGenres.text = genres

        binding.movieSynopsis.text = movie?.plot
        binding.txtDirector.text = getString(R.string.director_text, movie?.director)
        val cast = movie?.actors?.joinToString(", ") ?: "N/A"
        binding.txtCast.text = getString(R.string.cast_text, cast)
        // Establece el OnClickListener para el botón "Ver ahora"
        binding.btnPlay.setOnClickListener {
            // Código para iniciar la reproducción de la película
            startActivity(Intent(this, PlayActivity::class.java).apply {
                putExtra("url", movie?.url)
                putExtra("id", movie?.title)
            })
        }

        // Establece el OnClickListener para el icono de favoritos
        binding.btnFavorite.setOnClickListener {
            //viewModel.toggleFavoriteMovie(applicationContext,user?.id ?: "", movie!!)
            CoroutineScope(Dispatchers.IO).launch {
                if (isFavorite) {
                    userFavorites.removeAll { movieToDelete -> movieToDelete.title == movie?.title }
                    userPreferences.saveFavorites(userFavorites)
                    runOnUiThread {
                        binding.btnFavorite.setImageDrawable(
                            ContextCompat.getDrawable(
                                this@MovieDetailActivity,
                                R.drawable.ic_favorite_nav
                            )
                        )
                    }
                } else {
                    userFavorites.add(movie!!)
                    userPreferences.saveFavorites(userFavorites)
                    binding.btnFavorite.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MovieDetailActivity,
                            R.drawable.ic_favorite_added
                        )
                    )
                }
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}