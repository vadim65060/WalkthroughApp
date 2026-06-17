package com.example.walkthrough.presentation.screens.walkthrough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkthrough.data.repository.ApartmentRepository
import com.example.walkthrough.data.repository.HouseRepository
import com.example.walkthrough.domain.models.Apartment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WalkthroughViewModel(
    private val apartmentRepository: ApartmentRepository,
    private val houseRepository: HouseRepository,
    houseId: Long
) : ViewModel() {

    private val _currentApartmentNumber = MutableStateFlow(1)
    val currentApartmentNumber: StateFlow<Int> = _currentApartmentNumber.asStateFlow()

    private val _stepDirection = MutableStateFlow(1) // 1 = +1, -1 = -1
    val stepDirection: StateFlow<Int> = _stepDirection.asStateFlow()

    private val _savedSuccess = MutableStateFlow(false)
    val savedSuccess: StateFlow<Boolean> = _savedSuccess.asStateFlow()

    private val _savedNumbers = MutableStateFlow<Set<Int>>(emptySet())
    val savedNumbers: StateFlow<Set<Int>> = _savedNumbers.asStateFlow()

    private var isFirstLoad = true

    init {
        apartmentRepository.getApartmentsByHouse(houseId)
            .onEach { apartments ->
                val numbers = apartments.map { it.apartmentNumber }.toSet()
                _savedNumbers.value = numbers
                if (isFirstLoad) {
                    isFirstLoad = false
                    // Устанавливаем начальный номер в зависимости от направления
                    val initialNumber = if (_stepDirection.value == 1) {
                        getMinUnsaved()
                    } else {
                        getMaxUnsaved()
                    }
                    _currentApartmentNumber.value = initialNumber
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleStepDirection() {
        val newDirection = if (_stepDirection.value == 1) -1 else 1
        _stepDirection.value = newDirection
        // При переключении направления устанавливаем соответствующий крайний несохранённый номер
        val newNumber = if (newDirection == 1) {
            getMinUnsaved()
        } else {
            getMaxUnsaved()
        }
        _currentApartmentNumber.value = newNumber
    }

    fun setCurrentApartmentNumber(number: Int) {
        if (number >= 1) {
            _currentApartmentNumber.value = number
        }
    }

    /**
     * Переключение на следующий/предыдущий несохранённый номер в текущем направлении
     */
    fun incrementApartment() {
        val current = _currentApartmentNumber.value
        val saved = _savedNumbers.value
        val direction = _stepDirection.value

        if (direction == 1) {
            // Ищем следующий несохранённый номер > current
            var candidate = current + 1
            while (saved.contains(candidate)) {
                candidate++
            }
            // Ограничим, чтобы не уйти в бесконечность
            if (candidate > 10000) return
            _currentApartmentNumber.value = candidate
        } else {
            // direction == -1, ищем предыдущий несохранённый номер < current
            var candidate = current - 1
            while (candidate > 0 && saved.contains(candidate)) {
                candidate--
            }
            if (candidate > 0) {
                _currentApartmentNumber.value = candidate
            } else {
                // Если несохранённых меньше current нет, переходим на максимальный несохранённый
                val maxUnsaved = getMaxUnsaved()
                _currentApartmentNumber.value = maxUnsaved
            }
        }
    }

    private fun getMinUnsaved(): Int {
        val saved = _savedNumbers.value
        var candidate = 1
        while (saved.contains(candidate)) {
            candidate++
        }
        return candidate
    }

    private fun getMaxUnsaved(): Int {
        val saved = _savedNumbers.value
        if (saved.isEmpty()) return 1
        val maxSaved = saved.maxOrNull() ?: 0
        // Идём от maxSaved вниз, пока номер сохранён
        var candidate = maxSaved - 1
        while (candidate > 0 && saved.contains(candidate)) {
            candidate--
        }
        if (candidate > 0) {
            return candidate
        } else {
            // Все номера от 1 до maxSaved сохранены, берём maxSaved + 1
            return maxSaved + 1
        }
    }

    suspend fun saveApartment(
        houseId: Long,
        apartmentNumber: Int,
        fullName: String,
        appeals: String,
        phone: String,
        attitude: String,
        comment: String
    ) {
        val apartment = Apartment(
            houseId = houseId,
            apartmentNumber = apartmentNumber,
            fullName = fullName,
            appeals = appeals,
            phone = phone,
            attitude = attitude,
            comment = comment,
            lastVisitDate = System.currentTimeMillis(),
            isNotHome = false
        )
        apartmentRepository.saveApartment(apartment)
        houseRepository.updateHouseLastUpdated(houseId)
        _savedSuccess.value = true
        // После сохранения переключаемся на следующий несохранённый
        incrementApartment()
    }

    suspend fun markNotHome(houseId: Long, apartmentNumber: Int) {
        apartmentRepository.markNotHome(houseId, apartmentNumber)
        houseRepository.updateHouseLastUpdated(houseId)
        _savedSuccess.value = true
        incrementApartment()
    }

    suspend fun loadApartment(houseId: Long, apartmentNumber: Int): Apartment? {
        return apartmentRepository.getApartmentByNumber(houseId, apartmentNumber)
    }

    fun resetSavedSuccess() {
        _savedSuccess.value = false
    }
}