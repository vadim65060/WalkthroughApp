package com.example.walkthrough.data.repository

import com.example.walkthrough.data.local.dao.DraftDao
import com.example.walkthrough.data.local.entities.DraftEntity
import com.example.walkthrough.domain.models.Apartment

class DraftRepository(
    private val draftDao: DraftDao
) {
    suspend fun saveDraft(houseId: Long, apartmentNumber: Int, apartment: Apartment) {
        val draft = DraftEntity(
            id = "${houseId}_${apartmentNumber}",
            houseId = houseId,
            apartmentNumber = apartmentNumber,
            fullName = apartment.fullName,
            appeals = apartment.appeals,
            phone = apartment.phone,
            attitude = apartment.attitude,
            comment = apartment.comment
        )
        draftDao.saveDraft(draft)
    }

    suspend fun getDraft(houseId: Long, apartmentNumber: Int): Apartment? {
        val entity = draftDao.getDraft(houseId, apartmentNumber)
        return entity?.let {
            Apartment(
                houseId = it.houseId,
                apartmentNumber = it.apartmentNumber,
                fullName = it.fullName,
                appeals = it.appeals,
                phone = it.phone,
                attitude = it.attitude,
                comment = it.comment
            )
        }
    }

    suspend fun deleteDraft(houseId: Long, apartmentNumber: Int) {
        draftDao.deleteDraftByKey(houseId, apartmentNumber)
    }

    suspend fun deleteDraftsByHouseId(houseId: Long) {
        draftDao.deleteDraftsByHouseId(houseId)
    }
}