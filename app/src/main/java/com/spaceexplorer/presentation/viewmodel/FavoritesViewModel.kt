package com.spaceexplorer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spaceexplorer.R
import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.usecase.GetFavoritesUseCase
import com.spaceexplorer.domain.usecase.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    getFavoritesUseCase: GetFavoritesUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val favorites: StateFlow<List<Apod>> = getFavoritesUseCase()
        .catch {
            _uiEvent.emit(UiEvent.ShowSnackbar(context.getString(R.string.error_favorites_load)))
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun removeFavorite(apod: Apod) {
        viewModelScope.launch {
            removeFavoriteUseCase(apod.date)
                .onFailure {
                    _uiEvent.emit(UiEvent.ShowSnackbar(context.getString(R.string.error_favorite_remove)))
                }
        }
    }
}
