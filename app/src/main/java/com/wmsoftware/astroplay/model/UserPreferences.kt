package com.wmsoftware.astroplay.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    /** SETTERS **/
    suspend fun saveTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("theme")] = isDark
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
}