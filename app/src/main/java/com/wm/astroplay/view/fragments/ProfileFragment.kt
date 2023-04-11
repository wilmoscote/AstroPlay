package com.wm.astroplay.view.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.wm.astroplay.BuildConfig
import com.wm.astroplay.R
import com.wm.astroplay.databinding.FragmentProfileBinding
import com.wm.astroplay.model.UserPreferences
import com.wm.astroplay.view.AuthenticationActivity
import com.wm.astroplay.view.MainActivity.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        userPreferences = UserPreferences(this.requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUser().collect { user ->
                withContext(Dispatchers.Main){
                    Glide.with(this@ProfileFragment).load(user?.photo).circleCrop().transition(
                        DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.default_user).into(binding.profileImage)
                    binding.userName.text = user?.name ?: "Usuario"
                    binding.userEmail.text = user?.email ?: "email@gmail.com"
                    when(user?.role){
                        1 -> {
                            binding.btnAdmin.isVisible = false
                            binding.userRole.setBackgroundResource(R.drawable.role_badge)
                            binding.userRole.text = getString(R.string.role_user_1)
                        }
                        2 -> {
                            binding.btnAdmin.isVisible = false
                            binding.userRole.setBackgroundResource(R.drawable.role_badge_2)
                            binding.userRole.text = getString(R.string.role_user_2)
                        }
                        3 -> {
                            binding.userRole.setBackgroundResource(R.drawable.role_badge_3)
                            binding.userRole.text = getString(R.string.role_user_3)
                            binding.btnAdmin.isVisible = true
                            binding.btnAdmin.setOnClickListener {
                                showRoleChangeDialog()
                            }
                        }
                        else -> {
                            binding.btnAdmin.isVisible = false
                            binding.userRole.setBackgroundResource(R.drawable.role_badge)
                            binding.userRole.text = getString(R.string.role_user_1)
                        }
                    }
                }
            }
        }


        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this.requireContext(),R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.logout)) { dialog, which ->
                    Firebase.auth.signOut()
                    CoroutineScope(Dispatchers.IO).launch {
                        userPreferences.clearDataStore()
                        startActivity(Intent(this@ProfileFragment.requireContext(), AuthenticationActivity::class.java))
                        requireActivity().finish()
                    }
                }
                .show()
        }

        binding.versionInfo.setOnLongClickListener {
            Toast.makeText(this.requireContext(), "\uD83D\uDC9B\uD83D\uDC99❤️", Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }

        binding.versionInfo.text = getString(R.string.version_info, BuildConfig.VERSION_NAME)
        return binding.root
    }

    private fun showRoleChangeDialog() {
        val view = LayoutInflater.from(this.requireContext()).inflate(R.layout.role_change_dialog, null)
        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val roleSpinner = view.findViewById<Spinner>(R.id.roleSpinner)
        val userDisable = view.findViewById<SwitchMaterial>(R.id.disableUser)
        val roles = arrayOf("Usuario", "Premium", "Administrador")
        val roleValues = arrayOf(1, 2, 3)
        roleSpinner.adapter = ArrayAdapter(this.requireContext(), R.layout.custom_spinner_item, roles)

        val dialog = MaterialAlertDialogBuilder(this.requireContext(), R.style.MaterialAlertDialog_rounded)
            .setTitle(getString(R.string.manage_user))
            .setView(view)
            .setPositiveButton(getString(R.string.update)) { _, _ ->
                val email = emailEditText.text.toString()
                val selectedRoleIndex = roleSpinner.selectedItemPosition
                val newRole = roleValues[selectedRoleIndex]

                if (isValidEmail(email)) {
                    updateUserRoleByEmail(email, newRole,userDisable.isChecked)
                } else {
                    Toast.makeText(context, getString(R.string.email_error), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.show()
    }

    private fun updateUserRoleByEmail(email: String, newRole: Int, disabled:Boolean) {
        val db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("users")

        usersRef.whereEqualTo("email", email).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this@ProfileFragment.requireContext(),getString(R.string.user_email_not_found),Toast.LENGTH_LONG).show()
                } else {
                    val userDoc = querySnapshot.documents.first()
                    val userId = userDoc.id

                    usersRef.document(userId).update("role", newRole)
                        .addOnSuccessListener {
                            Log.d(TAG, "Rol del usuario actualizado correctamente")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error al actualizar el rol del usuario", e)
                        }

                    usersRef.document(userId).update("disabled", disabled)
                        .addOnSuccessListener {
                            Log.d(TAG, "Estado del usuario actualizado correctamente")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error al actualizar el Estado del usuario", e)
                        }
                    Toast.makeText(this@ProfileFragment.requireContext(),getString(R.string.user_updated),Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@ProfileFragment.requireContext(),getString(R.string.user_update_error),Toast.LENGTH_LONG).show()
                Log.w(TAG, "Error al buscar usuario por correo electrónico", e)
            }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z\\d._%+-]+@[A-Za-z\\d.-]+\\.[A-Z]{2,6}$"
        return email.matches(Regex(emailPattern, RegexOption.IGNORE_CASE))
    }
}