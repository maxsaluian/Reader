package com.maxsaluian.android.reader.ui.viewmodel

import androidx.lifecycle.*
import com.maxsaluian.android.reader.data.Repository
import com.maxsaluian.android.reader.data.local.Preferences
import com.maxsaluian.android.reader.data.model.UpdateValues
import com.maxsaluian.android.reader.data.model.cross.FeedWithEntries
import com.maxsaluian.android.reader.data.model.entry.Entry
import com.maxsaluian.android.reader.data.model.entry.EntryLight
import com.maxsaluian.android.reader.data.model.feed.Feed
import com.maxsaluian.android.reader.data.remote.FeedParser
import com.maxsaluian.android.reader.ui.dialog.FilterEntriesFragment
import com.maxsaluian.android.reader.ui.fragment.EntryListFragment
import com.maxsaluian.android.reader.ui.fragment.EntryListFragment.Companion.FOLDER
import com.maxsaluian.android.reader.util.UpdateManager
import com.maxsaluian.android.reader.util.extensions.shortened
import com.maxsaluian.android.reader.util.extensions.sortedByDate
import com.maxsaluian.android.reader.util.extensions.sortedUnreadOnTop
import kotlinx.coroutines.launch
import java.util.*

class EntryListViewModel : ViewModel(), UpdateManager.UpdateReceiver {

    private val repo = Repository.get()
    private val parser = FeedParser(repo.networkMonitor)
    private val updateManager = UpdateManager(this)

    private val feedIdLiveData = MutableLiveData<String>()
    val feedLiveData = Transformations.switchMap(feedIdLiveData) { feedId ->
        repo.getFeed(feedId)
    }
    private val sourceEntriesLiveData = Transformations.switchMap(feedIdLiveData) { feedId ->
        when (feedId) {
            EntryListFragment.FOLDER_NEW -> repo.getNewEntries(MAX_NEW_ENTRIES)
            EntryListFragment.FOLDER_STARRED -> repo.getStarredEntries()
            else -> repo.getEntriesByFeed(feedId)
        }
    }

    private val entriesLiveData = MediatorLiveData<List<Entry>>()
    val entriesLightLiveData = MediatorLiveData<List<EntryLight>>()
    val updateResultLiveData = parser.feedRequestLiveData

    var query = ""
        private set
    private var order = 0
    var filter = 0
        private set
    val updateValues = UpdateValues()

    private var updateWasRequested = false
    var isAutoUpdating = true

    init {
        entriesLiveData.addSource(sourceEntriesLiveData) { source ->
            val filteredEntries = filterEntries(source, filter)
            entriesLiveData.value = queryEntries(filteredEntries, query)
            updateManager.setInitialEntries(source)
        }

        entriesLightLiveData.addSource(entriesLiveData) { entries ->
            val list = entries.map { entry ->
                EntryLight(
                    url = entry.url,
                    title = entry.title,
                    website = entry.website,
                    date = entry.date,
                    image = entry.image,
                    isRead = entry.isRead,
                    isStarred = entry.isStarred
                )
            }
            entriesLightLiveData.value = sortEntries(list, order)
        }
    }

    fun getFeedWithEntries(feedId: String) {
        if (feedId.startsWith(FOLDER)) isAutoUpdating = false
        feedIdLiveData.value = feedId
    }

    fun requestUpdate(url: String) {
        isAutoUpdating = false
        updateWasRequested = true
        viewModelScope.launch { parser.requestFeed(url) }
    }

    fun onFeedRetrieved(feed: Feed?) {
        feed?.let { updateManager.setInitialFeed(feed) }
    }

    fun onUpdatesDownloaded(feedWithEntries: FeedWithEntries) {
        if (updateWasRequested) {
            updateManager.submitUpdates(feedWithEntries)
            updateWasRequested = false
        }
    }

    fun setFilter(filter: Int) {
        this.filter = filter
        sourceEntriesLiveData.value?.let { entries ->
            val filteredEntries = filterEntries(entries, filter)
            entriesLiveData.value = queryEntries(filteredEntries, query)
        }
    }

    fun setOrder(order: Int) {
        if (this.order != order) {
            this.order = order
            entriesLightLiveData.value?.let { entries ->
                entriesLightLiveData.value = sortEntries(entries, order)
            }
        }
    }

    fun submitQuery(query: String) {
        this.query = query.trim()
        sourceEntriesLiveData.value?.let { source ->
            val filteredEntries = filterEntries(source, filter)
            entriesLiveData.value = if (this.query.isNotEmpty()) {
                queryEntries(filteredEntries, this.query)
            } else filteredEntries
        }
    }

    fun clearQuery() {
        submitQuery("")
    }

    fun starAllCurrentEntries() {
        val entries = entriesLightLiveData.value ?: emptyList()
        val isStarred = !allIsStarred(entries)
        val entryIds = entries.map { entry -> entry.url }.toTypedArray()
        repo.updateEntryIsStarred(*entryIds, isStarred = isStarred)
    }

    fun markAllCurrentEntriesAsRead() {
        val entries = entriesLightLiveData.value ?: emptyList()
        val isRead = !allIsRead(entries)
        val entryIds = entries.map { entry -> entry.url }.toTypedArray()
        repo.updateEntryIsRead(*entryIds, isRead = isRead)
    }

    fun keepOldUnreadEntries(isKeeping: Boolean) {
        updateManager.keepOldUnreadEntries = isKeeping
    }

    fun allIsStarred(
        entries: List<EntryLight> = entriesLightLiveData.value ?: emptyList()
    ): Boolean {
        var count = 0
        for (entry in entries) {
            if (entry.isStarred) count += 1 else break
        }
        return count == entries.size
    }

    fun allIsRead(
        entries: List<EntryLight> = entriesLightLiveData.value ?: emptyList()
    ): Boolean {
        var count = 0
        for (entry in entries) {
            if (entry.isRead) count += 1 else break
        }
        return count == entries.size
    }

    private fun queryEntries(entries: List<Entry>, query: String): List<Entry> {
        val results = mutableListOf<Entry>()
        for (entry in entries) {
            if (entry.title.toLowerCase(Locale.ROOT).contains(query) ||
                entry.website.shortened().toLowerCase(Locale.ROOT).contains(query)
            ) {
                results.add(entry)
            }
        }
        return results
    }

    private fun filterEntries(entries: List<Entry>, filter: Int): List<Entry> {
        return when (filter) {
            FilterEntriesFragment.FILTER_UNREAD -> entries.filter { !it.isRead }
            FilterEntriesFragment.FILTER_STARRED -> entries.filter { it.isStarred }
            else -> entries
        }
    }

    private fun sortEntries(entries: List<EntryLight>, order: Int): List<EntryLight> {
        return if (order == Preferences.ENTRY_ORDER_UNREAD) {
            entries.sortedUnreadOnTop()
        } else {
            entries.sortedByDate()
        }
    }

    override fun onUnreadEntriesCounted(feedId: String, unreadCount: Int) {
        repo.updateFeedUnreadCount(feedId, unreadCount)
    }

    fun updateFeed(feed: Feed) {
        updateManager.forceUpdateFeed(feed)
    }

    override fun onFeedNeedsUpdate(feed: Feed) {
        repo.updateFeed(feed)
    }

    override fun onOldAndNewEntriesCompared(
        feedId: String,
        entriesToAdd: List<Entry>,
        entriesToUpdate: List<Entry>,
        entriesToDelete: List<Entry>,
    ) {
        repo.handleEntryUpdates(feedId, entriesToAdd, entriesToUpdate, entriesToDelete)
        if (entriesToAdd.size + entriesToUpdate.size > 0) {
            updateValues.added = entriesToAdd.size
            updateValues.updated = entriesToUpdate.size
        } else {
            updateValues.clear()
        }
    }

    fun getCurrentFeed() = updateManager.currentFeed

    fun updateEntryIsStarred(entryId: String, isStarred: Boolean) {
        repo.updateEntryIsStarred(entryId, isStarred = isStarred)
    }

    fun updateEntryIsRead(entryId: String, isRead: Boolean) {
        repo.updateEntryIsRead(entryId, isRead = isRead)
    }

    fun deleteFeedAndEntries() {
        getCurrentFeed()?.url?.let { feedId -> repo.deleteFeedAndEntriesById(feedId) }
    }

    companion object {

        private const val MAX_NEW_ENTRIES = 50
    }
}