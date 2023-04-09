package com.wmsoftware.astroplay.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wmsoftware.astroplay.R
import com.wmsoftware.astroplay.model.Movie
import com.wmsoftware.astroplay.model.MovieProvider
import com.wmsoftware.astroplay.model.UserPreferences
import com.wmsoftware.astroplay.view.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MoviesViewModel : ViewModel() {
    private val db = Firebase.firestore
    val TAG = "AstroDebug"
    var actualFavorite = MutableLiveData<Boolean>()
    var movieList = MutableLiveData<List<Movie>>()
    var popularMovieList = MutableLiveData<List<Movie>>()
    var recentMovieList = MutableLiveData<List<Movie>>()
    var randomMovieList = MutableLiveData<List<Movie>>()
    var searchResult = MutableLiveData<List<Movie>>()
    var searching = MutableLiveData<Boolean>()
    var userFavorites = mutableListOf<Movie>()
    fun init(){
        viewModelScope.launch {
            fetchPopularMovies()
            fetchRecentsMovies()
            fetchRandomMovies()
        }
    }
    private suspend fun getMovies(){
        withContext(Dispatchers.IO){
            db.collection("movies")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val movies = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Movie::class.java)
                    }
                    Log.d(TAG, "Movies fetched. ${querySnapshot.documents.toString()}")
                    movieList.postValue(movies)
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error al obtener las películas.", exception)
                }
        }
    }

    suspend fun fetchPopularMovies() {
        return withContext(Dispatchers.IO) {
            db.collection("movies")
                .orderBy("views", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val movies = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Movie::class.java)
                    }
                    Log.d(TAG, "Movies fetched. ${querySnapshot.documents.toString()}")
                    popularMovieList.postValue(movies)
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error al obtener las películas.", exception)
                }
        }
    }

    suspend fun fetchRecentsMovies() {
        return withContext(Dispatchers.IO) {
            db.collection("movies")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val movies = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Movie::class.java)
                    }
                    Log.d(TAG, "Movies fetched. ${querySnapshot.documents.toString()}")
                    recentMovieList.postValue(movies)
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error al obtener las películas.", exception)
                }
        }
    }

    suspend fun fetchRandomMovies(numberOfMovies: Int = 10) {
        val movieCollection = db.collection("movies")

        // Obtener todos los documentos en la colección
        val snapshot = movieCollection.get().await()

        // Convertir los documentos a objetos 'Movie'
        val randomMovies = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Movie::class.java)
        }
        randomMovieList.postValue(randomMovies.shuffled().take(numberOfMovies))
    }


    suspend fun searchMovies(searchTerm: String) {
        searching.postValue(true)

        val titleQuery = db.collection("movies")
            .whereGreaterThanOrEqualTo("title",searchTerm)
            .get()
            .await()

        // Realizar consulta para buscar por 'originalTitle'
        val originalTitleQuery = db.collection("movies")
            .whereGreaterThanOrEqualTo("originalTitle",searchTerm)
            .get()
            .await()

        // Convertir los documentos a objetos 'Movie'
        val titleResults = titleQuery.documents.mapNotNull { doc ->
            doc.toObject(Movie::class.java)
        }
        Log.d(TAG,"Title result: ${titleResults.toString()}")

        val originalTitleResults = originalTitleQuery.documents.mapNotNull { doc ->
            doc.toObject(Movie::class.java)
        }
        Log.d(TAG,"OriginalTitle result: ${originalTitleResults.toString()}")

        // Combinar los resultados y eliminar duplicados
        val combinedResults = (titleResults + originalTitleResults).distinctBy { it.title }

        // Filtrar los resultados para asegurarse de que 'title' o 'originalTitle' contengan el término de búsqueda
        val filteredResults = combinedResults.filter { movie ->
            movie.title.contains(searchTerm) ||
                    (movie.originalTitle?.contains(searchTerm) ?: false)
        }

        searchResult.postValue(filteredResults)
        searching.postValue(false)
    }

    suspend fun incrementMovieViews(movieId: String) {
        val movieRef = db.collection("movies").document(movieId)

        db.runTransaction { transaction ->
            val movieSnapshot = transaction.get(movieRef)

            if (movieSnapshot.exists()) {
                val currentViews = movieSnapshot.getLong("views") ?: 0
                transaction.update(movieRef, "views", currentViews + 1)
            }
        }.await()
    }

    suspend fun isMovieInFavorites(userId: String, movieId: String): Boolean {
        val movieRef = db.collection("users").document(userId).collection("favorites").document(movieId)
        val movieSnapshot = movieRef.get().await()
        Log.e("AstroDebug","isFavorite ${movieSnapshot.exists().toString()}")

        return movieSnapshot.exists()
    }

    suspend fun toggleFavoriteMovie(context: Context, userId: String, movie: Movie) {
        val favoritesRef = db.collection("users").document(userId).collection("favorites")
        val movieRef = favoritesRef.document(movie.title) // O usa el ID de la película si lo prefieres

        db.runTransaction { transaction ->
            val movieSnapshot = transaction.get(movieRef)

            if (movieSnapshot.exists()) {
                // Si la película ya está en favoritos, elimínala
                //removeMovieToLocalFavorite(context, movie)
                transaction.delete(movieRef)

            } else {
                // Si la película no está en favoritos, agrégala
               // addMovieToLocalFavorite(context, movie)
                transaction.set(movieRef, movie)
            }
        }.await()
    }
}

