package com.example.walkthrough.presentation.screens.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkthrough.data.repository.ApartmentRepository
import com.example.walkthrough.data.repository.HouseRepository
import com.example.walkthrough.domain.models.Apartment
import com.example.walkthrough.domain.models.House
import com.example.walkthrough.domain.utils.ApartmentDataExport
import com.example.walkthrough.domain.utils.DateFormatter
import com.example.walkthrough.domain.utils.ExportUtils
import com.example.walkthrough.domain.utils.HouseDataExport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExportViewModel(
    private val houseRepository: HouseRepository,
    private val apartmentRepository: ApartmentRepository
) : ViewModel() {

    private val _houses = MutableStateFlow<List<House>>(emptyList())
    val houses: StateFlow<List<House>> = _houses.asStateFlow()

    private val _selectedHouses = MutableStateFlow<Set<Long>>(emptySet())
    val selectedHouses: StateFlow<Set<Long>> = _selectedHouses.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    init {
        loadHouses()
    }

    private fun loadHouses() {
        viewModelScope.launch {
            houseRepository.getAllHouses().collect { houseList ->
                _houses.value = houseList
            }
        }
    }

    fun toggleHouseSelection(houseId: Long) {
        val current = _selectedHouses.value.toMutableSet()
        if (current.contains(houseId)) {
            current.remove(houseId)
        } else {
            current.add(houseId)
        }
        _selectedHouses.value = current
    }

    fun selectAllHouses() {
        _selectedHouses.value = _houses.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedHouses.value = emptySet()
    }

    suspend fun generateCsv(): String? {
        val selected = getSelectedHousesData() ?: return null
        val sb = StringBuilder()
        selected.forEach { (house, apartments) ->
            sb.append("# ${house.address}\n")
            sb.append(ExportUtils.exportToCsv(house, apartments))
            sb.append("\n")
        }
        return sb.toString()
    }

    suspend fun generateJson(): String {
        val selected = getSelectedHousesData() ?: return "[]"
        val housesData = selected.map { (house, apartments) ->
            HouseDataExport(
                address = house.address,
                lastUpdated = DateFormatter.formatDate(house.lastUpdated),
                apartments = apartments.map { apt ->
                    ApartmentDataExport(
                        number = apt.apartmentNumber,
                        fullName = apt.fullName,
                        appeals = apt.appeals,
                        phone = apt.phone,
                        attitude = apt.attitude,
                        comment = apt.comment,
                        lastVisitDate = DateFormatter.formatDate(apt.lastVisitDate),
                        isNotHome = apt.isNotHome
                    )
                }
            )
        }
        return ExportUtils.exportToJson(housesData)
    }

    suspend fun getSelectedHousesData(): List<Pair<House, List<Apartment>>>? {
        if (_selectedHouses.value.isEmpty()) return null

        val result = mutableListOf<Pair<House, List<Apartment>>>()
        for (houseId in _selectedHouses.value) {
            val house = houseRepository.getHouseById(houseId) ?: continue
            val apartments = apartmentRepository.getApartmentsByHouse(houseId).first()
            result.add(house to apartments)
        }
        return result
    }
}