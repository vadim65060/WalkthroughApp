package com.example.walkthrough.presentation.screens.homes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkthrough.data.repository.ApartmentRepository
import com.example.walkthrough.data.repository.HouseRepository
import com.example.walkthrough.domain.models.House
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomesViewModel(
    private val houseRepository: HouseRepository,
    private val apartmentRepository: ApartmentRepository
) : ViewModel() {

    private val _houses = MutableStateFlow<List<House>>(emptyList())
    val houses: StateFlow<List<House>> = _houses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHouses()
    }

    private fun loadHouses() {
        viewModelScope.launch {
            _isLoading.value = true
            houseRepository.getAllHouses().collect { houseList ->
                _houses.value = houseList
                _isLoading.value = false
            }
        }
    }

    fun deleteHouse(house: House) {
        viewModelScope.launch {
            houseRepository.deleteHouse(house)
        }
    }

    suspend fun getApartmentsCount(houseId: Long): Int {
        return apartmentRepository.getApartmentsCount(houseId)
    }
}