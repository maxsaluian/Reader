package com.maxsaluian.android.reader.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.maxsaluian.android.reader.R
import com.maxsaluian.android.reader.data.model.TopicBlock
import com.maxsaluian.android.reader.data.model.feed.FeedIdWithCategory

class AddFeedsViewModel : FeedAddingViewModel() {

    val feedIdsWithCategoriesLiveData = repo.getFeedIdsWithCategories()
    private val _topicBlocksLiveData = MutableLiveData<List<TopicBlock>>()
    val topicBlocksLiveData: LiveData<List<TopicBlock>>
        get() = _topicBlocksLiveData

    var categories = listOf<String>()
    private var isFirstTimeLoading = true

    private val defaultTopics: MutableList<String> = mutableListOf()
    val defaultTopicsResId: List<Int> = listOf(
        R.string.news, R.string.politics, R.string.world, R.string.business, R.string.science,
        R.string.tech, R.string.art, R.string.culture, R.string.books, R.string.entertainment
    )
    private val colorsResId: List<Int> = listOf(
        R.color.topic1, R.color.topic2, R.color.topic3, R.color.topic4, R.color.topic5,
        R.color.topic6, R.color.topic7, R.color.topic8, R.color.topic9, R.color.topic10
    )

    fun initDefaultTopics(topics: List<String>) {
        topics.forEach { defaultTopics.add(it) }
    }

    fun onFeedDataRetrieved(data: List<FeedIdWithCategory>) {
        currentFeedIds = data.map { it.url }
        val categories = data.map { it.category }.distinct().filterNot { it == "Uncategorized" }
        if (categories.sorted() != this.categories.sorted() || categories.isEmpty()) {
            if (isFirstTimeLoading) _topicBlocksLiveData.value = getTopicBlocks(categories)
            this.categories = categories
            isFirstTimeLoading = false
        }
    }

    private fun getTopicBlocks(categories: List<String>): List<TopicBlock> {
        val topics = (categories + defaultTopics).distinct().shuffled()
        val topicBlocks: MutableList<TopicBlock> = mutableListOf()
        var index = 0
        while (topicBlocks.size < MAX_TOPICS) {
            topicBlocks.add(TopicBlock(topics[index], colorsResId[index]))
            index += 1
        }
        return topicBlocks.shuffled()
    }

    companion object {

        private const val MAX_TOPICS = 10
    }
}