package com.example.walkthrough.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS drafts")

        // Создаём таблицу заново с правильной структурой
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS drafts (
                id TEXT PRIMARY KEY NOT NULL,
                houseId INTEGER NOT NULL,
                apartmentNumber INTEGER NOT NULL,
                fullName TEXT NOT NULL DEFAULT '',
                appeals TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                attitude TEXT NOT NULL DEFAULT '',
                comment TEXT NOT NULL DEFAULT ''
            )
        """)

        // Создаём индекс
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_drafts_house_apartment 
            ON drafts(houseId, apartmentNumber)
        """)
    }
}