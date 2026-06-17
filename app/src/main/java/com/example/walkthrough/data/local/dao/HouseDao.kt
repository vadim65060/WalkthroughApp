package com.example.walkthrough.data.local.dao

import androidx.room.*
import com.example.walkthrough.data.local.entities.HouseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseDao {
    @Query("SELECT * FROM houses ORDER BY lastUpdated DESC")
    fun getAllHouses(): Flow<List<HouseEntity>>

    @Query("SELECT * FROM houses WHERE id = :houseId")
    suspend fun getHouseById(houseId: Long): HouseEntity?

    @Insert
    suspend fun insertHouse(house: HouseEntity): Long

    @Update
    suspend fun updateHouse(house: HouseEntity)

    @Delete
    suspend fun deleteHouse(house: HouseEntity)

    @Query("UPDATE houses SET lastUpdated = :timestamp WHERE id = :houseId")
    suspend fun updateHouseLastUpdated(houseId: Long, timestamp: Long)
}