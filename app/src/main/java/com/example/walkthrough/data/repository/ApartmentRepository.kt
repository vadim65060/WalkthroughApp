package com.example.walkthrough.data.repository

import com.example.walkthrough.data.local.dao.ApartmentDao
import com.example.walkthrough.data.local.entities.ApartmentEntity
import com.example.walkthrough.domain.models.Apartment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ApartmentRepository constructor(
    private val apartmentDao: ApartmentDao
) {
    fun getApartmentsByHouse(houseId: Long): Flow<List<Apartment>> {
        return apartmentDao.getApartmentsByHouse(houseId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getApartmentByNumber(houseId: Long, apartmentNumber: Int): Apartment? {
        return apartmentDao.getApartmentByNumber(houseId, apartmentNumber)?.toDomainModel()
    }

    suspend fun getApartmentsCounts(): Map<Long, Int> {
        return apartmentDao.getApartmentsCounts().associate { it.houseId to it.count }
    }

    suspend fun saveApartment(apartment: Apartment) {
        val existing = apartmentDao.getApartmentByNumber(apartment.houseId, apartment.apartmentNumber)
        if (existing != null) {
            // Обновляем существующий
            val updatedEntity = apartment.toEntity().copy(id = existing.id)
            apartmentDao.updateApartment(updatedEntity)
        } else {
            // Вставляем новый
            apartmentDao.insertApartment(apartment.toEntity())
        }
    }

    suspend fun markNotHome(houseId: Long, apartmentNumber: Int) {
        val existing = apartmentDao.getApartmentByNumber(houseId, apartmentNumber)
        if (existing != null) {
            val updated = existing.copy(
                isNotHome = true,
                lastVisitDate = System.currentTimeMillis()
            )
            apartmentDao.updateApartment(updated)
        } else {
            val newApartment = ApartmentEntity(
                houseId = houseId,
                apartmentNumber = apartmentNumber,
                isNotHome = true,
                lastVisitDate = System.currentTimeMillis()
            )
            apartmentDao.insertApartment(newApartment)
        }
    }

    suspend fun deleteApartment(apartment: Apartment) {
        apartmentDao.deleteApartment(apartment.toEntity())
    }

    suspend fun getApartmentsCount(houseId: Long): Int {
        return apartmentDao.getApartmentsCount(houseId)
    }

    suspend fun getMaxApartmentNumber(houseId: Long): Int {
        return apartmentDao.getMaxApartmentNumber(houseId) ?: 0
    }

    private fun ApartmentEntity.toDomainModel(): Apartment {
        return Apartment(
            id = id,
            houseId = houseId,
            apartmentNumber = apartmentNumber,
            fullName = fullName,
            appeals = appeals,
            phone = phone,
            attitude = attitude,
            comment = comment,
            lastVisitDate = lastVisitDate,
            isNotHome = isNotHome
        )
    }

    private fun Apartment.toEntity(): ApartmentEntity {
        return ApartmentEntity(
            id = id,
            houseId = houseId,
            apartmentNumber = apartmentNumber,
            fullName = fullName,
            appeals = appeals,
            phone = phone,
            attitude = attitude,
            comment = comment,
            lastVisitDate = lastVisitDate,
            isNotHome = isNotHome
        )
    }
}