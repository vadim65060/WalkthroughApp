package com.example.walkthrough.data.local.dao

import androidx.room.*
import com.example.walkthrough.data.local.entities.ApartmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApartmentDao {
    @Query("SELECT * FROM apartments WHERE houseId = :houseId ORDER BY apartmentNumber ASC")
    fun getApartmentsByHouse(houseId: Long): Flow<List<ApartmentEntity>>

    @Query("SELECT * FROM apartments WHERE houseId = :houseId AND apartmentNumber = :apartmentNumber")
    suspend fun getApartmentByNumber(houseId: Long, apartmentNumber: Int): ApartmentEntity?

    @Insert
    suspend fun insertApartment(apartment: ApartmentEntity): Long

    @Update
    suspend fun updateApartment(apartment: ApartmentEntity)

    @Delete
    suspend fun deleteApartment(apartment: ApartmentEntity)

    @Query("SELECT COUNT(*) FROM apartments WHERE houseId = :houseId")
    suspend fun getApartmentsCount(houseId: Long): Int

    @Query("SELECT MAX(apartmentNumber) FROM apartments WHERE houseId = :houseId")
    suspend fun getMaxApartmentNumber(houseId: Long): Int?
}