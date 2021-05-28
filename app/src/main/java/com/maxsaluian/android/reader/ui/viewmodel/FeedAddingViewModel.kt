package com.maxsaluian.android.reader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxsaluian.android.reader.data.Repository
import com.maxsaluian.android.reader.data.model.cross.FeedWithEntries
import com.maxsaluian.android.reader.data.remote.FeedParser
import kotlinx.coroutines.launch

abstract class FeedAddingViewModel : ViewModel() {

    val repo = Repository.get()
    private val parser = FeedParser(repo.networkMonitor)

    val feedRequestLiveData = parser.feedRequestLiveData
    var currentFeedIds = listOf<String>()

    var isActiveRequest = false
    var requestFailedNoticeEnabled = false
    var alreadyAddedNoticeEnabled = false
    var subscriptionLimitNoticeEnabled = false
    var lastInputUrl = ""

    fun requestFeed(url: String, backup: String? = null) {
        onFeedRequested()
        viewModelScope.launch {
            parser.requestFeed(url, backup)
        }
    }

    private fun onFeedRequested() {
        isActiveRequest = true
        requestFailedNoticeEnabled = true
        alreadyAddedNoticeEnabled = true
        subscriptionLimitNoticeEnabled = true
    }

    fun addFeedWithEntries(feedWithEntries: FeedWithEntries) {
        repo.addFeedWithEntries(feedWithEntries)
    }

    fun cancelRequest() {
        parser.cancelRequest()
    }
}