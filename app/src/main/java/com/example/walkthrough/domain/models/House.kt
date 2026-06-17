package com.example.walkthrough.domain.models

data class House(
    val id: Long = 0,
    val address: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)