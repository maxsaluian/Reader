package com.maxsaluian.android.reader.ui.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.maxsaluian.android.reader.data.Repository
import com.maxsaluian.android.reader.data.local.Preferences
import com.maxsaluian.android.reader.data.model.CategoryHeader
import com.maxsaluian.android.reader.data.model.FeedMenuItem
import com.maxsaluian.android.reader.data.model.feed.FeedLight
import com.maxsaluian.android.reader.util.extensions.sortedByTitle
import com.maxsaluian.android.reader.util.extensions.sortedByUnreadCount

class FeedListViewModel : ViewModel() {

    private val repo = Repository.get()
    private val sourceFeedsLiveData = repo.getFeedsLight()

    var activeFeedId: String? = null
    var categories = arrayOf<String>()
        private set
    val minimizedCategories = mutableSetOf<String>()
    private var feedOrder = 0
    val feedListLiveData = MediatorLiveData<List<FeedMenuItem>>()

    init {
        feedListLiveData.addSource(sourceFeedsLiveData) { feeds ->
            feedListLiveData.value = organizeFeedsAndCategories(feeds, minimizedCategories)
        }
    }

    fun setMinimizedCategories(categories: Set<String>?) {
        categories?.forEach { category ->
            minimizedCategories.add(category)
        }
    }

    fun toggleCategoryDropDown(category: String) {
        if (minimizedCategories.contains(category)) {
            minimizedCategories.remove(category)
        } else minimizedCategories.add(category)
        arrangeMenu()
    }

    fun setFeedOrder(order: Int) {
        if (order != feedOrder) {
            feedOrder = order
            arrangeMenu()
        }
    }

    private fun arrangeMenu() {
        sourceFeedsLiveData.value?.let { feeds ->
            feedListLiveData.value = organizeFeedsAndCategories(feeds, minimizedCategories)
        }
    }

    private fun sortFeeds(feeds: List<FeedLight>, order: Int): List<FeedLight> {
        return if (order == Preferences.FEED_ORDER_UNREAD) {
            feeds.sortedByUnreadCount()
        } else {
            feeds.sortedByTitle()
        }
    }

    private fun organizeFeedsAndCategories(
        feeds: List<FeedLight>,
        minimizedCategories: Set<String>
    ): List<FeedMenuItem> {
        val categories = getOrderedCategories(feeds)
        val arrangedMenu = mutableListOf<FeedMenuItem>()

        for (category in categories) {
            val isMinimized = minimizedCategories.contains(category)
            val categoryHeader = CategoryHeader(category, isMinimized)
            arrangedMenu.add(FeedMenuItem(categoryHeader))

            sortFeeds(feeds, feedOrder).forEach { feed ->
                if (feed.category == category) {
                    categoryHeader.unreadCount += feed.unreadCount
                    if (!isMinimized) {
                        arrangedMenu.add(FeedMenuItem(feed))
                    }
                }
            }
        }

        this.categories = categories.toTypedArray()
        return arrangedMenu
    }

    private fun getOrderedCategories(feeds: List<FeedLight>): List<String> {
        val categories = mutableSetOf<String>()
        for (feed in feeds) {
            categories.add(feed.category)
        }
        // Sort alphabetically:
        return categories.toList().sorted()
    }
}