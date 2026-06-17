package com.example.walkthrough.di

import android.content.Context
import androidx.room.Room
import com.example.walkthrough.data.local.AppDatabase
import com.example.walkthrough.data.repository.ApartmentRepository
import com.example.walkthrough.data.repository.HouseRepository

object RepositoryHolder {
    private lateinit var houseRepository: HouseRepository
    private lateinit var apartmentRepository: ApartmentRepository

    fun init(context: Context) {
        val database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "walkthrough_database"
        ).build()

        houseRepository = HouseRepository(database.houseDao())
        apartmentRepository = ApartmentRepository(database.apartmentDao())
    }

    fun getHouseRepository(): HouseRepository = houseRepository
    fun getApartmentRepository(): ApartmentRepository = apartmentRepository
}