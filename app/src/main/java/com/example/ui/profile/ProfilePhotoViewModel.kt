package com.example.ui.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProfilePhotoRepository
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilePhotoUiState(
    val photoUrl: String? = null,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

class ProfilePhotoViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val photoRepository = ProfilePhotoRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProfilePhotoUiState())
    val uiState: StateFlow<ProfilePhotoUiState> = _uiState.asStateFlow()

    init {
        observeCurrentPhoto()
    }

    private fun observeCurrentPhoto() {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentAuthPhoto = auth.currentUser?.photoUrl?.toString()
        _uiState.update { it.copy(photoUrl = currentAuthPhoto) }

        viewModelScope.launch {
            userRepository.observeUserProfile(currentUserId).collect { profile ->
                if (profile != null) {
                    val url = profile.photoUrl ?: auth.currentUser?.photoUrl?.toString()
                    _uiState.update { it.copy(photoUrl = url) }
                }
            }
        }
    }

    fun uploadPhoto(uri: Uri) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Please sign in to upload a profile picture.") }
            return
        }

        if (_uiState.value.isUploading || _uiState.value.isDeleting) return

        _uiState.update { it.copy(isUploading = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val compressedBytes = photoRepository.compressAndDownscale(uri)
                if (compressedBytes == null) {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = "Could not process image. Please try another photo."
                        )
                    }
                    return@launch
                }

                val result = photoRepository.uploadProfilePhoto(userId, compressedBytes)
                result.onSuccess { downloadUrl ->
                    _uiState.update {
                        it.copy(
                            photoUrl = downloadUrl,
                            isUploading = false,
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    Log.e("ProfilePhotoVM", "Upload failed: ${error.message}", error)
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = error.localizedMessage ?: "Failed to upload photo. Please check your connection."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfilePhotoVM", "Upload exception: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = e.localizedMessage ?: "An unexpected error occurred during upload."
                    )
                }
            }
        }
    }

    fun deletePhoto() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Please sign in to manage profile picture.") }
            return
        }

        if (_uiState.value.isUploading || _uiState.value.isDeleting) return

        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = photoRepository.deleteProfilePhoto(userId)
                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            photoUrl = null,
                            isDeleting = false,
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    Log.e("ProfilePhotoVM", "Delete failed: ${error.message}", error)
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = error.localizedMessage ?: "Failed to remove photo. Please try again."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfilePhotoVM", "Delete exception: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = e.localizedMessage ?: "An unexpected error occurred."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
