package com.example.walkthrough.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.walkthrough.data.local.entities.DraftEntity

@Dao
interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE houseId = :houseId AND apartmentNumber = :apartmentNumber")
    suspend fun getDraft(houseId: Long, apartmentNumber: Int): DraftEntity?

    @Delete
    suspend fun deleteDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE houseId = :houseId AND apartmentNumber = :apartmentNumber")
    suspend fun deleteDraftByKey(houseId: Long, apartmentNumber: Int)

    @Query("DELETE FROM drafts WHERE houseId = :houseId")
    suspend fun deleteDraftsByHouseId(houseId: Long)
}