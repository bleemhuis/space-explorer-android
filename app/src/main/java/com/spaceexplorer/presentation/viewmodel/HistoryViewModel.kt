package com.spaceexplorer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spaceexplorer.R
import com.spaceexplorer.domain.usecase.GetApodRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getApodRangeUseCase: GetApodRangeUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Idle)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker: StateFlow<Boolean> = _showDatePicker.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun openDatePicker() {
        _showDatePicker.update { true }
    }

    fun dismissDatePicker() {
        _showDatePicker.update { false }
    }

    fun loadRange(startDate: String, endDate: String) {
        _showDatePicker.update { false }
        viewModelScope.launch {
            _uiState.update { HistoryUiState.Loading }
            getApodRangeUseCase(startDate, endDate)
                .onSuccess { items ->
                    _uiState.update { HistoryUiState.Success(items) }
                }
                .onFailure { error ->
                    _uiState.update {
                        HistoryUiState.Error(error.message ?: context.getString(R.string.error_unknown))
                    }
                    _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_loading)))
                }
        }
    }
}
