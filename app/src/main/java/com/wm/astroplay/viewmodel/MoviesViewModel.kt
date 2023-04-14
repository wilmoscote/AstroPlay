package com.wm.astroplay.viewmodel

import android.content.ClipDescription
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wm.astroplay.R
import com.wm.astroplay.model.*
import com.wm.astroplay.view.MainActivity
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
    var searchGenreResult = MutableLiveData<List<Movie>>()
    var searching = MutableLiveData<Boolean>()
    var searchingGenre = MutableLiveData<Boolean>()
    var userNotifications = MutableLiveData<List<Notification>>()
    var userFavorites = mutableListOf<Movie>()
    var requestSent = MutableLiveData<Boolean>()
    var loading = MutableLiveData<Boolean>()

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
                    //Log.d(TAG, "Movies fetched. ${querySnapshot.documents.toString()}")
                    movieList.postValue(movies)
                }
                .addOnFailureListener { exception ->
                    //Log.w(TAG, "Error al obtener las películas.", exception)
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
                    //Log.d(TAG, "Movies fetched. ${querySnapshot.documents.toString()}")
                    popularMovieList.postValue(movies)
                }
                .addOnFailureListener { exception ->
                    //Log.w(TAG, "Error al obtener las películas.", exception)
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
                    //Log.d(TAG, "Movies fetched. ${querySnapshot.documents.toString()}")
                    recentMovieList.postValue(movies)
                }
                .addOnFailureListener { exception ->
                    //Log.w(TAG, "Error al obtener las películas.", exception)
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

        val originalTitleResults = originalTitleQuery.documents.mapNotNull { doc ->
            doc.toObject(Movie::class.java)
        }

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

    suspend fun searchMoviesByGenre(genre: String) {
        searchingGenre.postValue(true)
        val genreQuery = db.collection("movies")
            .whereArrayContains("genre", genre)
            .get()
            .await()

        val genreResult = genreQuery.documents.mapNotNull { doc ->
            doc.toObject(Movie::class.java)
        }
        searchGenreResult.postValue(genreResult)
        searchingGenre.postValue(false)
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

    suspend fun getUserNotifications(userId: String) {
        withContext(Dispatchers.IO){
            db.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val notifications = querySnapshot.documents
                        .filter { doc ->
                            val targetUsers = doc["targetUsers"] as? List<*>
                            targetUsers == null || userId in targetUsers
                        }.mapNotNull { doc -> doc.toObject(Notification::class.java) }
                    Log.d(TAG,"Hay notificaciones para ti: ${notifications.toString()}")
                    userNotifications.postValue(notifications)
                }
        }
    }

    suspend fun removeUserFromNotificationTarget(userId: String, notificationId: String) {
        withContext(Dispatchers.IO){
            val notificationRef = db.collection("notifications").document(notificationId)
            db.runTransaction { transaction ->
                val notificationSnapshot = transaction.get(notificationRef)
                val targetUsers = notificationSnapshot["targetUsers"] as? MutableList<String> ?: mutableListOf()
                targetUsers.remove(userId)
                transaction.update(notificationRef, "targetUsers", targetUsers)
            }.addOnSuccessListener {
                // El ID del usuario se ha eliminado correctamente de la lista targetUsers
                Log.d("AstroDebug","User removed from notification")
            }.addOnFailureListener { exception ->
                Log.d("AstroDebug","User NOT removed from notification")
                // Error al eliminar el ID del usuario de la lista targetUsers
            }
        }
    }

    suspend fun sendRequest(title: String, description: String?, currentUser: User) {
        withContext(Dispatchers.IO){
            loading.postValue(true)
            val request = Request(
                userId = currentUser.id.toString(),
                userName = currentUser.name.toString(),
                userEmail = currentUser.email.toString(),
                title = title,
                description = if (description?.isNotEmpty() == true) description else null
            )

            db.collection("requests").add(request)
                .addOnSuccessListener {
                    loading.postValue(false)
                    requestSent.postValue(true)
                }
                .addOnFailureListener { _ ->
                    loading.postValue(false)
                    requestSent.postValue(false)
                }
        }
    }
}

