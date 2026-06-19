package com.example.walkthrough.presentation.screens.apartments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkthrough.data.repository.ApartmentRepository
import com.example.walkthrough.domain.models.Apartment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ApartmentsListViewModel(
    private val apartmentRepository: ApartmentRepository
) : ViewModel() {

    private val _apartments = MutableStateFlow<List<Apartment>>(emptyList())
    val apartments: StateFlow<List<Apartment>> = _apartments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadApartments(houseId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                apartmentRepository.getApartmentsByHouse(houseId).collect { apartmentList ->
                    _apartments.value = apartmentList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки квартир: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun deleteApartment(apartment: Apartment) {
        viewModelScope.launch {
            try {
                apartmentRepository.deleteApartment(apartment)
            } catch (e: Exception) {
                _error.value = "Ошибка удаления квартиры: ${e.message}"
            }
        }
    }

    suspend fun updateApartment(apartment: Apartment) {
        try {
            apartmentRepository.saveApartment(apartment)
        } catch (e: Exception) {
            _error.value = "Ошибка обновления квартиры: ${e.message}"
        }
    }

    fun clearError() {
        _error.value = null
    }
}