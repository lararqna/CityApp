package com.example.cityapp

import android.content.Context
import com.example.cityapp.AppDatabase
import com.example.cityapp.repository.LocationRepository
import com.google.firebase.firestore.FirebaseFirestore

object ServiceLocator {

    fun provideLocationRepository(context: Context): LocationRepository {
        val db = AppDatabase.getInstance(context)
        val firestore = FirebaseFirestore.getInstance()
        return LocationRepository(
            cityDao = db.cityDao(),
            locationDao = db.locationDao(),
            db = firestore
        )
    }
}
