//package com.e.tracker.database
//
//import android.content.Context
//import androidx.room.*
//
//@Database(entities = [TrackCoordModel::class], version = 1, exportSchema = false)
//@TypeConverters(Converters::class)
//abstract class TrackCoordDatabase : RoomDatabase() {
//
//    abstract val trackCoordDatabaseDao: TrackCoordDatabaseDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: TrackCoordDatabase? = null
//
//        fun getInstance(context: Context): TrackCoordDatabase {
//            synchronized(this) {
//                var instance = INSTANCE
//
//                if (instance == null) {
//                    instance = Room.databaseBuilder(
//                        context.applicationContext,
//                        TrackCoordDatabase::class.java,
//                        "track_coords"
//                    )
//                        .fallbackToDestructiveMigration()
//                        .build()
//
//                    INSTANCE = instance
//                }
//                return instance
//            }
//        }
//    }
//}