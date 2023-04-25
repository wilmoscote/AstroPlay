package com.wm.astroplay.model

import android.content.Context
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.wm.astroplay.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "userPreferences")
class UserPreferences(private val context: Context) {

    private val dataStore: DataStore<Preferences> by lazy {
        context.dataStore
    }

    suspend fun clearDataStore() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /** GETTERS **/
    fun getMoviePlaybackPosition(movieId: String) = dataStore.data.map { preferences ->
        preferences[longPreferencesKey(movieId)]
    }

    fun getLastRequestTime() = dataStore.data.map { preferences ->
        preferences[longPreferencesKey("last_request_time")]
    }

    fun getUserTheme() = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("theme")]
    }

    fun getUser(): Flow<User?> {
        return dataStore.data.map { preferences ->
            val jsonString = preferences[stringPreferencesKey("user")]
            jsonString?.let { Json.decodeFromString(it) }
        }
    }

    fun getFavorites(): Flow<List<Movie>> {
        return dataStore.data.map { preferences ->
            val jsonString = preferences[stringPreferencesKey("favorites")] ?: "[]"
            Json.decodeFromString(jsonString)
        }
    }

    fun getFcmToken() = dataStore.data.map { preferences ->
        preferences[stringPreferencesKey("fcm_token")]
    }

    /** SETTERS **/
    suspend fun saveTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("theme")] = isDark
        }
    }

    suspend fun saveMoviePlaybackPosition(movieId: String, position: Long) {
        dataStore.edit { preferences ->
            preferences[longPreferencesKey(movieId)] = position
        }
    }

    suspend fun saveLastRequestTime(time: Long) {
        dataStore.edit { preferences ->
            preferences[longPreferencesKey("last_request_time")] = time
        }
    }


    suspend fun saveUser(user: User) {
        val jsonString = Json.encodeToString(User.serializer(), user)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("user")] = jsonString
        }
    }

    suspend fun saveFavorites(favorites: List<Movie>) {
        val jsonString = Json.encodeToString(favorites)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("favorites")] = jsonString
        }
    }

    suspend fun saveFcmToken(token: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("fcm_token")] = token
        }
    }
}