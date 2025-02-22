package com.wm.astroplay.view

import com.wm.astroplay.view.PlayActivity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.wm.astroplay.R
import com.wm.astroplay.databinding.ActivityMovieDetailBinding
import com.wm.astroplay.model.Movie
import com.wm.astroplay.model.User
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.viewmodel.MoviesViewModel
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
    private var movieAnnounce: InterstitialAd? = null
    var movie: Movie? = null
    var playedMovie = false
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        firebaseAnalytics = Firebase.analytics

        movie = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("movie")
        } else {
            intent.getParcelableExtra<Movie>("movie")
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUser().collect { currentUser ->
                user = currentUser
                if ((user?.role ?: 1) < 2) {
                    initAds()
                    withContext(Dispatchers.Main) {
                        initInterstitial()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.loadingMovie.isVisible = false
                        binding.btnPlay.isVisible = true
                    }
                }
                withContext(Dispatchers.Main) {
                    binding.btnPlay.setOnClickListener {
                        val parameters = Bundle().apply {
                            this.putString("action", "play_movie")
                        }
                        firebaseAnalytics.logEvent("movie_detail", parameters)
                        if ((user?.role ?: 1) < 2) {
                            showMovieAd(false)
                        } else {
                            startActivity(
                                Intent(
                                    this@MovieDetailActivity,
                                    PlayActivity::class.java
                                ).apply {
                                    putExtra("movie", movie as Parcelable)
                                })
                        }
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getFavorites().collect { favorites ->
                userFavorites = favorites.toMutableList()
                isFavorite = favorites.any { it.title == (movie?.title) }

                withContext(Dispatchers.Main) {
                    if (isFavorite) binding.btnFavorite.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MovieDetailActivity,
                            R.drawable.ic_favorite_added
                        )
                    )
                }
            }
        }

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val layoutParams = binding.appBar.layoutParams
        layoutParams.height = screenHeight / 3
        binding.appBar.layoutParams = layoutParams

        Glide.with(this)
            .load(movie?.poster)
            .transition(
                DrawableTransitionOptions.withCrossFade()
            )
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
        binding.txtQuality.text = movie?.quality ?: "HD"
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


        // Establece el OnClickListener para el icono de favoritos
        binding.btnFavorite.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                if (isFavorite) {
                    userFavorites.removeAll { movieToDelete -> movieToDelete.title == movie?.title }
                    userPreferences.saveFavorites(userFavorites)
                    val parameters = Bundle().apply {
                        this.putString("action", "removed")
                    }
                    firebaseAnalytics.logEvent("menu_favorites", parameters)
                    withContext(Dispatchers.Main) {
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
                    val parameters = Bundle().apply {
                        this.putString("action", "added")
                    }
                    firebaseAnalytics.logEvent("menu_favorites", parameters)
                    withContext(Dispatchers.Main) {
                        binding.btnFavorite.setImageDrawable(
                            ContextCompat.getDrawable(
                                this@MovieDetailActivity,
                                R.drawable.ic_favorite_added
                            )
                        )
                    }
                }
            }
        }

        binding.btnShare.setOnClickListener {
            startShareFlow()
        }

        binding.btnRate.setOnClickListener {
            showRatingDialog()
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showMovieAd(resume: Boolean) {
        movieAnnounce?.show(this)
        movieAnnounce?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                initInterstitial()
                if(!resume){
                    playedMovie = true
                    startActivity(
                        Intent(
                            this@MovieDetailActivity,
                            PlayActivity::class.java
                        ).apply {
                            putExtra("movie", movie as Parcelable)
                        })
                }
              //  Log.d("AstroDebug","Ad Dismissed!")
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
               // Log.d("AstroDebug","Ad Failed!")
                initInterstitial()
            }

            override fun onAdShowedFullScreenContent() {
               // Log.d("AstroDebug","Ad Showed!")
                movieAnnounce = null
                initInterstitial()
            }
        }
    }

    private fun initInterstitial() {
        val adRequest: AdRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-1892256007304751/7211468461", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    movieAnnounce = interstitialAd
                    binding.loadingMovie.isVisible = false
                    binding.btnPlay.isVisible = true
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    movieAnnounce = null
                }
            })
        //ca-app-pub-1892256007304751/7211468461
    }

    private fun initAds() {
        MobileAds.initialize(this) {}

        val adRequest = AdRequest.Builder().build()
        runOnUiThread {
            binding.adView.loadAd(adRequest)

            binding.adView.adListener = object : AdListener() {
                override fun onAdClicked() {

                    // Code to be executed when the user clicks on an ad.
                }

                override fun onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Code to be executed when an ad request fails.
                }

                override fun onAdImpression() {
                    // Code to be executed when an impression is recorded
                    // for an ad.
                }

                override fun onAdLoaded() {
                    binding.adView.isVisible = true
                    // Code to be executed when an ad finishes loading.
                }

                override fun onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                }
            }
        }
    }

    private fun startShareFlow(){
        val share = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "https://astroplay.dev/?id=${movie?.id ?: movie?.title.toString()}")
            putExtra(Intent.EXTRA_TITLE, "AstroPlay - Peliculas y Series Online")
            type="text/plain"
            flags=Intent.FLAG_GRANT_READ_URI_PERMISSION
        }, getString(R.string.share_title).toString())
        val bundle = Bundle()
        bundle.putString("share_movie","share")
        FirebaseAnalytics.getInstance(applicationContext).logEvent("share_movie",bundle)
        startActivity(share)
    }

    private fun showRatingDialog() {
        // Infla el layout del diálogo de calificación
        val inflater = LayoutInflater.from(this)
        val ratingDialogView = inflater.inflate(R.layout.rating_dialog, null)

        // Obtiene una referencia al RatingBar
        val ratingBar: RatingBar = ratingDialogView.findViewById(R.id.rating_bar)

        // Crea y muestra el MaterialAlertDialog
        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            .setTitle(getString(R.string.rate_movie_title))
            .setView(ratingDialogView)
            .setPositiveButton(getString(R.string.rate_movie)) { _, _ ->
                val userRating = ratingBar.rating

                lifecycleScope.launch {
                    try {
                        viewModel.saveRating(user?.id ?: "", movie?.id ?: movie?.title.toString(), userRating)
                        Toast.makeText(this@MovieDetailActivity, getString(R.string.rate_sent), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MovieDetailActivity, getString(R.string.rate_sent_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    override fun onResume() {
        super.onResume()
        if(playedMovie){
            showMovieAd(true)
            playedMovie = false
        }
    }
}