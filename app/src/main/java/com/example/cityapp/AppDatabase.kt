package com.example.cityapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cityapp.entity.CityEntity
import com.example.cityapp.entity.LocationEntity
import com.example.cityapp.dao.CityDao
import com.example.cityapp.dao.LocationDao
@Database(
    entities = [CityEntity::class, LocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cityDao(): CityDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cityapp.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
