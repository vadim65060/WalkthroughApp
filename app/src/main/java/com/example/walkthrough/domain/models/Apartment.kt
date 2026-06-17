package com.example.walkthrough.domain.models

data class Apartment(
    val id: Long = 0,
    val houseId: Long,
    val apartmentNumber: Int,
    val fullName: String = "",
    val appeals: String = "",
    val phone: String = "",
    val attitude: String = "",
    val comment: String = "",
    val lastVisitDate: Long = System.currentTimeMillis(),
    val isNotHome: Boolean = false
)