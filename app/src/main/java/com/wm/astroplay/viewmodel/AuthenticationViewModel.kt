package com.wm.astroplay.viewmodel

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.wm.astroplay.R
import com.wm.astroplay.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthenticationViewModel : ViewModel() {
    private val db = Firebase.firestore
    var newProfileImage = MutableLiveData<String>()
    var userUpdated = MutableLiveData<Boolean>()
    var loading = MutableLiveData<Boolean>()

    suspend fun updateProfile(user: User, imageUri: Uri?, newName:String) {
        withContext(Dispatchers.IO){
            loading.postValue(true)
            val imageId = UUID.randomUUID().toString()
            val storageReference = FirebaseStorage.getInstance().reference
            val profileImageRef = storageReference.child("profiles/${imageId}.jpg")

            if (imageUri != null){
                val uploadTask = profileImageRef.putFile(imageUri)
                uploadTask.addOnSuccessListener {
                    profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        viewModelScope.launch {
                            val updatedUser = User(
                                id = user.id,
                                name = newName,
                                email = user.email,
                                photo = downloadUri.toString(),
                                favorites = user.favorites,
                                role = user.role,
                                deviceId = user.deviceId,
                                fcmToken = user.fcmToken,
                                disabled = user.disabled,
                                createdAt = user.createdAt
                            )
                            db.collection("users").document(user.id.toString()).set(updatedUser).await()
                            loading.postValue(false)
                            userUpdated.postValue(true)
                        }
                    }
                }.addOnFailureListener {
                    // Manejar el error de carga aquí
                    loading.postValue(false)
                }
            } else {
                viewModelScope.launch {
                    val updatedUser = User(
                        id = user.id,
                        name = newName,
                        email = user.email,
                        photo = user.photo,
                        favorites = user.favorites,
                        role = user.role,
                        disabled = user.disabled,
                        createdAt = user.createdAt
                    )
                    db.collection("users").document(user.id.toString()).set(updatedUser).await()
                    loading.postValue(false)
                    userUpdated.postValue(true)
                }
            }
        }
    }

    suspend fun updateUserToken(email: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("users")

        usersRef.whereEqualTo("email", email).get()
            .addOnSuccessListener { querySnapshot ->
                    val userDoc = querySnapshot.documents.first()
                    val userId = userDoc.id

                    usersRef.document(userId).update("fcmToken", token)
                        .addOnSuccessListener {
                            // Log.d(TAG, "Rol del usuario actualizado correctamente")
                        }
                        .addOnFailureListener { e ->
                            // Log.w(TAG, "Error al actualizar el rol del usuario", e)
                        }
            }
            .addOnFailureListener { e ->
                //Log.w(TAG, "Error al buscar usuario por correo electrónico", e)
            }
    }
}