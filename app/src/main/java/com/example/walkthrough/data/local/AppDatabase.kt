package com.example.walkthrough.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.walkthrough.data.local.dao.ApartmentDao
import com.example.walkthrough.data.local.dao.HouseDao
import com.example.walkthrough.data.local.entities.ApartmentEntity
import com.example.walkthrough.data.local.entities.HouseEntity

@Database(
    entities = [HouseEntity::class, ApartmentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun houseDao(): HouseDao
    abstract fun apartmentDao(): ApartmentDao
}