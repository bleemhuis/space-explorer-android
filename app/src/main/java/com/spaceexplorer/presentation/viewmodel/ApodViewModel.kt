package com.spaceexplorer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spaceexplorer.R
import com.spaceexplorer.domain.usecase.GetApodUseCase
import com.spaceexplorer.domain.usecase.ObserveIsFavoriteUseCase
import com.spaceexplorer.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApodViewModel @Inject constructor(
    private val getApodUseCase: GetApodUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val observeIsFavoriteUseCase: ObserveIsFavoriteUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ApodUiState>(ApodUiState.Loading)
    val uiState: StateFlow<ApodUiState> = _uiState.asStateFlow()

    val isFavorite: StateFlow<Boolean> = _uiState
        .flatMapLatest { state ->
            if (state is ApodUiState.Success) {
                observeIsFavoriteUseCase(state.apod.date)
            } else {
                flowOf(false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun loadApod(date: String? = null) {
        viewModelScope.launch {
            _uiState.update { ApodUiState.Loading }
            getApodUseCase(date)
                .onSuccess { apod -> _uiState.update { ApodUiState.Success(apod) } }
                .onFailure { error ->
                    _uiState.update {
                        ApodUiState.Error(error.message ?: context.getString(R.string.error_unknown))
                    }
                    _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_loading)))
                }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value as? ApodUiState.Success ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(state.apod)
                .onFailure {
                    _uiEvent.emit(UiEvent.ShowSnackbar(context.getString(R.string.error_favorite_save)))
                }
        }
    }
}
