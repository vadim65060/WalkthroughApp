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

    fun loadApartments(houseId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            apartmentRepository.getApartmentsByHouse(houseId).collect { apartmentList ->
                _apartments.value = apartmentList
                _isLoading.value = false
            }
        }
    }

    fun deleteApartment(apartment: Apartment) {
        viewModelScope.launch {
            apartmentRepository.deleteApartment(apartment)
        }
    }

    suspend fun updateApartment(apartment: Apartment) {
        apartmentRepository.saveApartment(apartment)
    }
}