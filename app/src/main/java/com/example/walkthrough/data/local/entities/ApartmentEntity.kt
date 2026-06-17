package com.example.walkthrough.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "apartments",
    foreignKeys = [
        ForeignKey(
            entity = HouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["houseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("houseId")]
)
data class ApartmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseId: Long,
    val apartmentNumber: Int,
    val fullName: String = "",
    val appeals: String = "", // текстовое поле
    val phone: String = "",
    val attitude: String = "", // "позитивно", "нейтрально", "негативно"
    val comment: String = "",
    val lastVisitDate: Long = System.currentTimeMillis(), // автоматически
    val isNotHome: Boolean = false
)