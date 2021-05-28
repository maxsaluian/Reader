package com.maxsaluian.android.reader.data.local.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.maxsaluian.android.reader.data.model.entry.Entry
import com.maxsaluian.android.reader.data.model.entry.EntryToggleable

interface EntriesDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addEntries(entries: List<Entry>)

    @Query("SELECT * FROM Entry WHERE url = :entryId")
    fun getEntry(entryId: String): LiveData<Entry?>

    @Query(
        "SELECT url, title, website, date, image, isStarred, isRead " +
                "FROM Entry WHERE isRead = 0 ORDER BY date DESC LIMIT :max"
    )
    // Warning is for unspecified fields, which we want null
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    fun getNewEntries(max: Int): LiveData<List<Entry>>

    @Query(
        "SELECT url, title, website, date, image, isStarred, isRead " +
                "FROM Entry WHERE isStarred = 1"
    )
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    fun getStarredEntries(): LiveData<List<Entry>>

    @Query(
        "SELECT Entry.url, title, website, author, date, content, image, isStarred, isRead " +
                "FROM FeedEntryCrossRef AS _junction " +
                "INNER JOIN Entry ON (_junction.entryUrl = Entry.url) " +
                "WHERE _junction.feedUrl = :feedId"
    )
    fun getEntriesByFeed(feedId: String): LiveData<List<Entry>>

    @Query(
        "SELECT Entry.url, isStarred, isRead " +
                "FROM FeedEntryCrossRef AS _junction " +
                "INNER JOIN Entry ON (_junction.entryUrl = Entry.url) " +
                "WHERE _junction.feedUrl = :feedId"
    )
    fun getEntriesToggleableByFeedSynchronously(feedId: String): List<EntryToggleable>

    @Update
    fun updateEntries(entries: List<Entry>)

    @Query("UPDATE Entry SET isStarred = :isStarred WHERE url IN (:entryId)")
    fun updateEntryIsStarred(vararg entryId: String, isStarred: Boolean)

    @Query("UPDATE Entry SET isRead = :isRead WHERE url IN (:entryId)")
    fun updateEntryIsRead(vararg entryId: String, isRead: Boolean)

    @Delete
    fun deleteEntries(entries: List<Entry>)

    @Query(
        "DELETE FROM Entry WHERE url IN " +
                "(SELECT url FROM FeedEntryCrossRef AS _junction " +
                "INNER JOIN Entry ON (_junction.entryUrl = Entry.url) " +
                "WHERE _junction.feedUrl IN (:feedId))"
    )
    fun deleteEntriesByFeed(vararg feedId: String)

    @Query("DELETE FROM Entry WHERE url IN (:entryIds)")
    fun deleteEntriesById(entryIds: List<String>)

    @Query("DELETE FROM Entry WHERE url NOT IN (SELECT entryUrl FROM FeedEntryCrossRef)")
    fun deleteLeftoverEntries()
}