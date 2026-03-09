package com.spaceexplorer.presentation.viewmodel

import com.spaceexplorer.domain.model.Apod

sealed class HistoryUiState {
    data object Idle : HistoryUiState()
    data object Loading : HistoryUiState()
    data class Success(val items: List<Apod>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
