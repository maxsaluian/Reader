package com.maxsaluian.android.reader.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.maxsaluian.android.reader.data.local.database.dao.CombinedDao
import com.maxsaluian.android.reader.data.model.cross.FeedEntryCrossRef
import com.maxsaluian.android.reader.data.model.entry.Entry
import com.maxsaluian.android.reader.data.model.feed.Feed

@Database(
    entities = [
        Feed::class,
        Entry::class,
        FeedEntryCrossRef::class
    ],
    version = 1
)
@TypeConverters(com.maxsaluian.android.reader.data.local.database.TypeConverters::class)
abstract class FeedDatabase : RoomDatabase() {

    abstract fun combinedDao(): CombinedDao

    companion object {
        private const val DATABASE_NAME = "database"

        fun build(context: Context): FeedDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FeedDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}