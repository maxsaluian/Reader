package com.maxsaluian.android.reader.data

import androidx.lifecycle.LiveData
import com.maxsaluian.android.reader.data.local.database.FeedDatabase
import com.maxsaluian.android.reader.data.model.cross.FeedTitleWithEntriesToggleable
import com.maxsaluian.android.reader.data.model.cross.FeedWithEntries
import com.maxsaluian.android.reader.data.model.entry.Entry
import com.maxsaluian.android.reader.data.model.entry.EntryToggleable
import com.maxsaluian.android.reader.data.model.feed.Feed
import com.maxsaluian.android.reader.data.model.feed.FeedIdWithCategory
import com.maxsaluian.android.reader.data.model.feed.FeedLight
import com.maxsaluian.android.reader.data.model.feed.FeedManageable
import com.maxsaluian.android.reader.util.NetworkMonitor
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class Repository private constructor(
    database: FeedDatabase,
    val networkMonitor: NetworkMonitor
) {

    private val dao = database.combinedDao()
    private val executor: Executor = Executors.newSingleThreadExecutor()

    fun getFeed(feedId: String): LiveData<Feed?> = dao.getFeed(feedId)

    fun getFeedsLight(): LiveData<List<FeedLight>> = dao.getFeedsLight()

    fun getFeedIds(): LiveData<List<String>> = dao.getFeedIds()

    fun getFeedIdsWithCategories(): LiveData<List<FeedIdWithCategory>> =
        dao.getFeedIdsWithCategories()

    fun getFeedUrlsSynchronously(): List<String> = dao.getFeedUrlsSynchronously()

    fun getFeedTitleWithEntriesToggleableSynchronously(feedId: String): FeedTitleWithEntriesToggleable {
        return dao.getFeedTitleAndEntriesToggleableSynchronously(feedId)
    }

    fun getFeedsManageable(): LiveData<List<FeedManageable>> = dao.getFeedsManageable()

    fun getEntry(entryId: String): LiveData<Entry?> = dao.getEntry(entryId)

    fun getEntriesByFeed(feedId: String): LiveData<List<Entry>> = dao.getEntriesByFeed(feedId)

    fun getNewEntries(max: Int): LiveData<List<Entry>> = dao.getNewEntries(max)

    fun getStarredEntries(): LiveData<List<Entry>> = dao.getStarredEntries()

    fun getEntriesToggleableByFeedSynchronously(feedId: String): List<EntryToggleable> {
        return dao.getEntriesToggleableByFeedSynchronously(feedId)
    }

    fun addFeedWithEntries(feedWithEntries: FeedWithEntries) {
        executor.execute {
            dao.addFeedAndEntries(feedWithEntries.feed, feedWithEntries.entries)
        }
    }

    fun updateFeed(feed: Feed) {
        executor.execute { dao.updateFeed(feed) }
    }

    fun updateFeedTitleAndCategory(feedId: String, title: String, category: String) {
        executor.execute { dao.updateFeedTitleAndCategory(feedId, title, category) }
    }

    fun updateFeedCategory(vararg feedId: String, category: String) {
        executor.execute { dao.updateFeedCategory(*feedId, category = category) }
    }

    fun updateFeedUnreadCount(feedId: String, count: Int) {
        executor.execute { dao.updateFeedUnreadCount(feedId, count) }
    }

    fun updateEntryAndFeedUnreadCount(entryId: String, isRead: Boolean, isStarred: Boolean) {
        executor.execute { dao.updateEntryAndFeedUnreadCount(entryId, isRead, isStarred) }
    }

    fun updateEntryIsStarred(vararg entryId: String, isStarred: Boolean) {
        executor.execute { dao.updateEntryIsStarred(*entryId, isStarred = isStarred) }
    }

    fun updateEntryIsRead(vararg entryId: String, isRead: Boolean) {
        executor.execute { dao.updateEntryIsReadAndFeedUnreadCount(*entryId, isRead = isRead) }
    }

    fun handleEntryUpdates(
        feedId: String,
        entriesToAdd: List<Entry>,
        entriesToUpdate: List<Entry>,
        entriesToDelete: List<Entry>,
    ) {
        executor.execute {
            dao.handleEntryUpdates(feedId, entriesToAdd, entriesToUpdate, entriesToDelete)
        }
    }

    fun handleBackgroundUpdate(
        feedId: String,
        newEntries: List<Entry>,
        oldEntries: List<EntryToggleable>,
        feedImage: String?,
    ) {
        executor.execute {
            dao.handleBackgroundUpdate(feedId, newEntries, oldEntries, feedImage)
        }
    }

    fun deleteFeedAndEntriesById(vararg feedId: String) {
        executor.execute { dao.deleteFeedAndEntriesById(*feedId) }
    }

    fun deleteLeftoverItems() {
        executor.execute { dao.deleteLeftoverItems() }
    }

    companion object {

        private var INSTANCE: Repository? = null

        fun initialize(database: FeedDatabase, networkMonitor: NetworkMonitor) {
            if (INSTANCE == null) INSTANCE = Repository(database, networkMonitor)
        }

        fun get(): Repository {
            return INSTANCE ?: throw IllegalStateException("Repository must be initialized!")
        }
    }
}