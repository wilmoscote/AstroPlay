package com.wm.astroplay.view

import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wm.astroplay.R
import com.wm.astroplay.databinding.ActivityPlayBinding
import com.wm.astroplay.model.Movie
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.view.MainActivity.Companion.TAG
import com.wm.astroplay.viewmodel.MoviesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PlayActivity : AppCompatActivity(), Player.Listener {
    private lateinit var binding: ActivityPlayBinding
    private lateinit var userPreferences: UserPreferences
    private var exoPlayer: ExoPlayer? = null
    private var movie: Movie? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private val viewModel: MoviesViewModel by viewModels()
    private var brightness: Int = 0
    private var startX = 0f
    private var startY = 0f
    var isResumingMovie = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(applicationContext)
        movie = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("movie", Movie::class.java)
        } else {
            intent.getParcelableExtra<Movie>("movie")
        }
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.incrementMovieViews(movie?.id ?: movie?.title.toString())
            runOnUiThread {
                binding.movieTitleText.text = movie?.title.toString()
            }
        }
        binding.btnClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        try {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

            } else {
                window.decorView.systemUiVisibility = getSystemUiVisibility()
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        } catch (e: Exception){
            //
        }

        binding.brilloSelector.setOnTouchListener { view, event ->
            try {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val rawX = event.rawX
                        val rawY = event.rawY

                        val dx = rawX - startX
                        val dy = startY - rawY

                        val increase = dy > 0
                        val newValue = if(increase) brightness + 1 else brightness - 1
                        if (newValue in 0..30) brightness = newValue

                        setScreenBrightness(brightness)
                        Log.d(TAG,"Adjuts brigh: $brightness")
                    }
                    MotionEvent.ACTION_UP -> {
                        //view.performClick()
                    }
                }
            } catch (e:Exception){
                Log.e(TAG,"Error: ${e.message.toString()}")
            }
            true
        }
        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.getMoviePlaybackPosition(movie?.title.toString()).collect { time ->
                isResumingMovie = time != null
            }
        }
        preparePlayer()
    }

    private fun preparePlayer() {
        // Crear y configurar el reproductor ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
            seekTo(playbackPosition)
            addListener(playerListener) // Agrega un listener para manejar eventos del reproductor
        }

        // Establecer el reproductor en la vista
        binding.playerView.apply {
            player = exoPlayer
            setControllerVisibilityListener(playerVisibilityListener)
        }

        // Crear y configurar la fuente de datos HTTP y la fuente de medios
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        val mediaItem = MediaItem.fromUri(movie?.url.toString())
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

        // Preparar el reproductor con la fuente de medios
        exoPlayer?.setMediaSource(mediaSource)
        exoPlayer?.prepare()
    }

    private val playerVisibilityListener =
        PlayerControlView.VisibilityListener { visibility ->
            // Controla la visibilidad de los controles del reproductor
            binding.brightControls.isVisible = visibility == View.VISIBLE
            binding.movieTitleText.isVisible = visibility == View.VISIBLE
            binding.btnClose.isVisible = visibility == View.VISIBLE
        }

    private fun setScreenBrightness(value: Int){
        try {
            val resolver = contentResolver
            val window = window
            val brightnessMode = Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
            if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                // Desactiva el brillo automático si está habilitado
                Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
            }
            binding.billoInfoLayout.isVisible = true
            val d = 1.0f/30
            val lp = this.window.attributes
            lp.screenBrightness = d * value
            this.window.attributes = lp
            binding.txtBrighnessValue.text = "${ ((d * value) * 100).roundToInt()}%"
            val handler = Handler(Looper.getMainLooper())
            handler.removeCallbacks(hideBrightnessIconRunnable)
            handler.postDelayed(hideBrightnessIconRunnable, 5000)
        } catch (e:Exception){
            //
        }
    }

    private val hideBrightnessIconRunnable = Runnable {
        binding.billoInfoLayout.isVisible = false
    }


    private fun relasePlayer(){
        exoPlayer?.let { player ->
            playbackPosition = player.currentPosition
            playWhenReady = player.playWhenReady
            player.release()
            exoPlayer = null
        }
    }

    override fun onStop() {
        super.onStop()
        relasePlayer()
    }

    override fun onPause() {
        super.onPause()
        playbackPosition = exoPlayer?.currentPosition ?: 0
        // Libera el reproductor si es necesario

        CoroutineScope(Dispatchers.IO).launch {
            userPreferences.saveMoviePlaybackPosition(movie?.title.toString(), playbackPosition)
        }
    }

    override fun onResume() {
        super.onResume()
        // Restaura la posición de reproducción
        if (exoPlayer != null) {
            exoPlayer?.seekTo(playbackPosition)
            exoPlayer?.playWhenReady = true
        } else {
            // Inicializa el reproductor si es necesario
            preparePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        relasePlayer()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            when (playbackState) {
                Player.STATE_BUFFERING -> binding.loading.isVisible = true
                Player.STATE_READY -> {
                    binding.loading.isVisible = false
                    if (isResumingMovie){
                        resumeMovie()
                    }
                }
                // También puedes manejar otros estados aquí, si lo deseas
                Player.STATE_ENDED -> {
                    //
                }
                Player.STATE_IDLE -> {
                    //
                }
            }
        }

    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log.e("AstroDebug", "Player error: ${error.message}", error)
    }

    private fun getSystemUiVisibility(): Int {
        return View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun resumeMovie() {
        lifecycleScope.launch {
            userPreferences.getMoviePlaybackPosition(movie?.title.toString()).collect() { time ->
                runOnUiThread {
                    MaterialAlertDialogBuilder(this@PlayActivity, R.style.MaterialAlertDialog_rounded)
                        .setTitle(getString(R.string.continue_playing))
                        .setMessage(getString(R.string.continue_playing_message))
                        .setPositiveButton(getString(R.string.continuee)) { _, _ ->
                            // Inicia PlayActivity y pasa la posición de reproducción
                            if (exoPlayer != null) {
                                exoPlayer?.seekTo(time ?: 0)
                                exoPlayer?.playWhenReady = true
                            }
                            isResumingMovie = false
                        }
                        .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                            //
                            isResumingMovie = false
                        }
                        .show()
                }
            }
        }
    }

}