package com.spaceexplorer.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `apod_cache` (
                `date` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `explanation` TEXT NOT NULL,
                `url` TEXT NOT NULL,
                `hdUrl` TEXT,
                `mediaType` TEXT NOT NULL,
                `copyright` TEXT,
                `thumbnailUrl` TEXT,
                `cachedAt` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
            )
            """.trimIndent()
        )
    }
}
