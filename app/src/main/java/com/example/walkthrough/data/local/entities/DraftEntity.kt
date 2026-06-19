package com.example.walkthrough.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey
    val id: String, // составной ключ: "houseId_apartmentNumber"
    val houseId: Long,
    val apartmentNumber: Int,
    val fullName: String = "",
    val appeals: String = "",
    val phone: String = "",
    val attitude: String = "",
    val comment: String = ""
)