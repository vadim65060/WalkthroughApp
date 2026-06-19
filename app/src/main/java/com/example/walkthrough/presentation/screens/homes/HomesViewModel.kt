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

    // Новый StateFlow для количества квартир по домам
    private val _apartmentsCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val apartmentsCounts: StateFlow<Map<Long, Int>> = _apartmentsCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadHouses()
    }

    private fun loadHouses() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                houseRepository.getAllHouses().collect { houseList ->
                    _houses.value = houseList
                    // Получаем количество квартир для всех домов одним запросом
                    val counts = apartmentRepository.getApartmentsCounts()
                    _apartmentsCounts.value = counts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки данных: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun deleteHouse(house: House) {
        viewModelScope.launch {
            try {
                houseRepository.deleteHouse(house)
            } catch (e: Exception) {
                _error.value = "Ошибка удаления дома: ${e.message}"
            }
        }
    }
}