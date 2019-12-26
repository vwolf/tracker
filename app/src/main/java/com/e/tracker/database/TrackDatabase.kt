package com.e.tracker.database

import android.content.Context
import androidx.room.*

@Database(entities = [TrackModel::class, TrackCoordModel::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TrackDatabase : RoomDatabase() {

    abstract  val trackDatabaseDao: TrackDatabaseDao
    abstract  val trackCoordDatabaseDao: TrackCoordDatabaseDao

    // The companion object allows clients to access the methods
    // for creating or getting the database without instantiating the class.
    // Since the only purpose of this class is to provide a database,
    // there is no reason to ever instantiate it.

    companion object {

        // The value of a volatile variable will never be cached,
        // and all writes and reads will be done to and from the main memory.
        // This helps make sure the value of INSTANCE is always up-to-date and
        // the same to all execution threads.
        @Volatile
        private var INSTANCE: TrackDatabase? = null

        fun getInstance(context: Context): TrackDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TrackDatabase::class.java,
                        "track_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
