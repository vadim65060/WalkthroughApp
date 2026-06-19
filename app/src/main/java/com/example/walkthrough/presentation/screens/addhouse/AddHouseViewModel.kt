// walkthrough/presentation/screens/addhouse/AddHouseViewModel.kt
package com.example.walkthrough.presentation.screens.addhouse

import androidx.lifecycle.ViewModel
import com.example.walkthrough.data.repository.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AddHouseViewModel(
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun addHouse(address: String, cityCode: String = ""): Long? {
        return try {
            _isSaving.value = true
            _error.value = null
            val id = houseRepository.insertHouse(address, cityCode)
            _isSaving.value = false
            id
        } catch (e: Exception) {
            _error.value = "Ошибка сохранения дома: ${e.message}"
            _isSaving.value = false
            null
        }
    }

    fun clearError() {
        _error.value = null
    }
}