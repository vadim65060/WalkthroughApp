// walkthrough/presentation/screens/walkthrough/WalkthroughViewModel.kt
package com.example.walkthrough.presentation.screens.walkthrough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkthrough.data.repository.ApartmentRepository
import com.example.walkthrough.data.repository.DraftRepository
import com.example.walkthrough.data.repository.HouseRepository
import com.example.walkthrough.domain.models.Apartment
import com.example.walkthrough.domain.utils.PhoneValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WalkthroughViewModel(
    private val apartmentRepository: ApartmentRepository,
    private val houseRepository: HouseRepository,
    private val draftRepository: DraftRepository,
    houseId: Long
) : ViewModel() {

    private val _currentApartmentNumber = MutableStateFlow(1)
    val currentApartmentNumber: StateFlow<Int> = _currentApartmentNumber.asStateFlow()

    private val _stepDirection = MutableStateFlow(1)
    val stepDirection: StateFlow<Int> = _stepDirection.asStateFlow()

    // Поля формы
    private val _currentFullName = MutableStateFlow("")
    val currentFullName: StateFlow<String> = _currentFullName.asStateFlow()

    private val _currentAppeals = MutableStateFlow("")
    val currentAppeals: StateFlow<String> = _currentAppeals.asStateFlow()

    private val _currentPhone = MutableStateFlow("")
    val currentPhone: StateFlow<String> = _currentPhone.asStateFlow()

    private val _currentAttitude = MutableStateFlow("")
    val currentAttitude: StateFlow<String> = _currentAttitude.asStateFlow()

    private val _currentComment = MutableStateFlow("")
    val currentComment: StateFlow<String> = _currentComment.asStateFlow()

    private val _phoneError = MutableStateFlow(false)
    val phoneError: StateFlow<Boolean> = _phoneError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _savedSuccess = MutableStateFlow(false)
    val savedSuccess: StateFlow<Boolean> = _savedSuccess.asStateFlow()

    private val _savedNumbers = MutableStateFlow<Set<Int>>(emptySet())
    val savedNumbers: StateFlow<Set<Int>> = _savedNumbers.asStateFlow()

    // Код города – загружается асинхронно, хранится как StateFlow
    private val _areaCode = MutableStateFlow<String?>(null)
    val areaCode: StateFlow<String?> = _areaCode.asStateFlow()

    private var currentHouseId = houseId
    private var isFirstLoad = true

    init {
        // Загружаем код города
        viewModelScope.launch {
            try {
                val house = houseRepository.getHouseById(houseId)
                _areaCode.value = house?.cityCode
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки кода города: ${e.message}"
            }
        }

        // Подписываемся на список сохранённых квартир
        apartmentRepository.getApartmentsByHouse(houseId)
            .onEach { apartments ->
                val numbers = apartments.map { it.apartmentNumber }.toSet()
                _savedNumbers.value = numbers
                if (isFirstLoad) {
                    isFirstLoad = false
                    val initialNumber = if (_stepDirection.value == 1) {
                        getMinUnsaved()
                    } else {
                        getMaxUnsaved()
                    }
                    _currentApartmentNumber.value = initialNumber
                    loadApartment(houseId, initialNumber)
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadApartment(houseId: Long, apartmentNumber: Int) {
        if (_isLoading.value) return
        _isLoading.value = true
        _error.value = null
        currentHouseId = houseId

        viewModelScope.launch {
            try {
                // Сначала черновик
                val draft = draftRepository.getDraft(houseId, apartmentNumber)
                if (draft != null) {
                    _currentFullName.value = draft.fullName
                    _currentAppeals.value = draft.appeals
                    _currentPhone.value = draft.phone
                    _currentAttitude.value = draft.attitude
                    _currentComment.value = draft.comment
                } else {
                    // Затем из БД
                    val apartment = apartmentRepository.getApartmentByNumber(houseId, apartmentNumber)
                    if (apartment != null && !apartment.isNotHome) {
                        _currentFullName.value = apartment.fullName
                        _currentAppeals.value = apartment.appeals
                        _currentPhone.value = apartment.phone
                        _currentAttitude.value = apartment.attitude
                        _currentComment.value = apartment.comment
                    } else {
                        clearFields()
                    }
                }
                // Валидация с учётом текущего areaCode
                validatePhone()
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun clearFields() {
        _currentFullName.value = ""
        _currentAppeals.value = ""
        _currentPhone.value = ""
        _currentAttitude.value = ""
        _currentComment.value = ""
    }

    fun updateField(field: String, value: String) {
        when (field) {
            "fullName" -> _currentFullName.value = value
            "appeals" -> _currentAppeals.value = value
            "phone" -> {
                _currentPhone.value = value
                validatePhone()
            }
            "attitude" -> _currentAttitude.value = value
            "comment" -> _currentComment.value = value
        }
        // Сохраняем черновик в БД (не блокируя UI)
        viewModelScope.launch {
            try {
                val currentNumber = _currentApartmentNumber.value
                val apartment = Apartment(
                    houseId = currentHouseId,
                    apartmentNumber = currentNumber,
                    fullName = _currentFullName.value,
                    appeals = _currentAppeals.value,
                    phone = _currentPhone.value,
                    attitude = _currentAttitude.value,
                    comment = _currentComment.value,
                    lastVisitDate = System.currentTimeMillis(),
                    isNotHome = false
                )
                draftRepository.saveDraft(currentHouseId, currentNumber, apartment)
            } catch (e: Exception) {
                _error.value = "Ошибка сохранения черновика: ${e.message}"
            }
        }
    }

    private fun validatePhone() {
        val currentAreaCode = _areaCode.value
        _phoneError.value = _currentPhone.value.isNotBlank() &&
                !PhoneValidator.isValid(_currentPhone.value, currentAreaCode)
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
        if (_isLoading.value) return
        _isLoading.value = true
        _error.value = null
        try {
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
            draftRepository.deleteDraft(houseId, apartmentNumber)
            _savedSuccess.value = true
            incrementApartment()
        } catch (e: Exception) {
            _error.value = "Ошибка сохранения: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun markNotHome(houseId: Long, apartmentNumber: Int) {
        if (_isLoading.value) return
        _isLoading.value = true
        _error.value = null
        try {
            apartmentRepository.markNotHome(houseId, apartmentNumber)
            houseRepository.updateHouseLastUpdated(houseId)
            draftRepository.deleteDraft(houseId, apartmentNumber)
            _savedSuccess.value = true
            incrementApartment()
        } catch (e: Exception) {
            _error.value = "Ошибка отметки 'нет дома': ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun resetSavedSuccess() {
        _savedSuccess.value = false
    }

    fun toggleStepDirection() {
        val newDirection = if (_stepDirection.value == 1) -1 else 1
        _stepDirection.value = newDirection
        val newNumber = if (newDirection == 1) {
            getMinUnsaved()
        } else {
            getMaxUnsaved()
        }
        setCurrentApartmentNumber(newNumber)
    }

    fun setCurrentApartmentNumber(number: Int) {
        if (number >= 1) {
            _currentApartmentNumber.value = number
            loadApartment(currentHouseId, number)
        }
    }

    private fun incrementApartment() {
        val current = _currentApartmentNumber.value
        val saved = _savedNumbers.value
        val direction = _stepDirection.value

        if (direction == 1) {
            var candidate = current + 1
            while (saved.contains(candidate)) {
                candidate++
            }
            if (candidate > 10000) return
            setCurrentApartmentNumber(candidate)
        } else {
            var candidate = current - 1
            while (candidate > 0 && saved.contains(candidate)) {
                candidate--
            }
            if (candidate > 0) {
                setCurrentApartmentNumber(candidate)
            } else {
                val maxUnsaved = getMaxUnsaved()
                setCurrentApartmentNumber(maxUnsaved)
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
        var candidate = maxSaved - 1
        while (candidate > 0 && saved.contains(candidate)) {
            candidate--
        }
        return if (candidate > 0) candidate else maxSaved + 1
    }

    fun clearError() {
        _error.value = null
    }
}