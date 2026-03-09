package com.spaceexplorer.presentation.viewmodel

import com.spaceexplorer.domain.model.Apod

sealed class ApodUiState {
    data object Loading : ApodUiState()
    data class Success(val apod: Apod) : ApodUiState()
    data class Error(val message: String) : ApodUiState()
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}
