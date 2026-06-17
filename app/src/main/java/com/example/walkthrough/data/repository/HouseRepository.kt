package com.example.walkthrough.data.repository

import com.example.walkthrough.data.local.dao.HouseDao
import com.example.walkthrough.data.local.entities.HouseEntity
import com.example.walkthrough.domain.models.House
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class HouseRepository constructor(
    private val houseDao: HouseDao
) {
    fun getAllHouses(): Flow<List<House>> {
        return houseDao.getAllHouses().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getHouseById(houseId: Long): House? {
        return houseDao.getHouseById(houseId)?.toDomainModel()
    }

    suspend fun insertHouse(address: String): Long {
        val house = HouseEntity(address = address)
        return houseDao.insertHouse(house)
    }

    suspend fun updateHouse(house: House) {
        houseDao.updateHouse(house.toEntity())
    }

    suspend fun deleteHouse(house: House) {
        houseDao.deleteHouse(house.toEntity())
    }

    suspend fun updateHouseLastUpdated(houseId: Long) {
        houseDao.updateHouseLastUpdated(houseId, System.currentTimeMillis())
    }

    private fun HouseEntity.toDomainModel(): House {
        return House(
            id = id,
            address = address,
            createdAt = createdAt,
            lastUpdated = lastUpdated
        )
    }

    private fun House.toEntity(): HouseEntity {
        return HouseEntity(
            id = id,
            address = address,
            createdAt = createdAt,
            lastUpdated = lastUpdated
        )
    }
}