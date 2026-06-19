package com.example.walkthrough.di

import android.content.Context
import androidx.room.Room
import com.example.walkthrough.data.local.AppDatabase
import com.example.walkthrough.data.local.MIGRATION_2_3
import com.example.walkthrough.data.repository.ApartmentRepository
import com.example.walkthrough.data.repository.DraftRepository
import com.example.walkthrough.data.repository.HouseRepository

object RepositoryHolder {
    private var isInitialized = false
    private lateinit var houseRepository: HouseRepository
    private lateinit var apartmentRepository: ApartmentRepository
    private lateinit var draftRepository: DraftRepository

    fun init(context: Context) {
        synchronized(this) {
            if (isInitialized) return
            val database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "walkthrough_database"
            )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .build()

            // 1. Сначала создаём репозитории без зависимостей
            apartmentRepository = ApartmentRepository(database.apartmentDao())
            draftRepository = DraftRepository(database.draftDao())

            // 2. Затем создаём HouseRepository с передачей draftRepository
            houseRepository = HouseRepository(database.houseDao(), draftRepository)

            isInitialized = true
        }
    }

    fun getHouseRepository(): HouseRepository = houseRepository
    fun getApartmentRepository(): ApartmentRepository = apartmentRepository
    fun getDraftRepository(): DraftRepository = draftRepository
}