package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent

@Database(entities = [Individual::class, StrikeEvent::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun individualDao(): IndividualDao
    abstract fun strikeEventDao(): StrikeEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE individuals ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE individuals ADD COLUMN firstStrikeLat REAL")
                db.execSQL("ALTER TABLE individuals ADD COLUMN firstStrikeLng REAL")
                db.execSQL("ALTER TABLE individuals ADD COLUMN firstStrikePlaceName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE strike_events ADD COLUMN lat REAL")
                db.execSQL("ALTER TABLE strike_events ADD COLUMN lng REAL")
                db.execSQL("ALTER TABLE strike_events ADD COLUMN placeName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE strike_events ADD COLUMN isFirstStrike INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "strikepulse_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys=ON;")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
