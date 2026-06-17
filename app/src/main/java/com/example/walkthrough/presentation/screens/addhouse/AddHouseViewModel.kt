package com.example.walkthrough.presentation.screens.addhouse

import androidx.lifecycle.ViewModel
import com.example.walkthrough.data.repository.HouseRepository

class AddHouseViewModel(
    private val houseRepository: HouseRepository
) : ViewModel() {

    suspend fun addHouse(address: String, cityCode: String = ""): Long {
        return houseRepository.insertHouse(address, cityCode)
    }
}