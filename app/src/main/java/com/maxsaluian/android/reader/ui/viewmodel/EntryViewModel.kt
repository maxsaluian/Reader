package com.maxsaluian.android.reader.ui.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.maxsaluian.android.reader.data.Repository
import com.maxsaluian.android.reader.data.model.entry.Entry
import com.maxsaluian.android.reader.data.model.entry.EntryMinimal
import com.maxsaluian.android.reader.data.remote.FeedParser
import com.maxsaluian.android.reader.util.EntryToHtmlFormatter

class EntryViewModel : ViewModel() {

    private val repo = Repository.get()

    private val entryIdLiveData = MutableLiveData<String>()
    private val entryLiveData = Transformations.switchMap(entryIdLiveData) { entryId ->
        repo.getEntry(entryId)
    }
    val htmlLiveData = MediatorLiveData<String?>()

    var lastPosition: Pair<Int, Int> = Pair(0, 0)
    var textSize = 0
        private set
    var font = 0
    var bannerIsEnabled = true
    var isInitialLoading = true

    var entry: Entry? = null
        private set
    private var isExcerpt = false // As of now, unused

    init {
        htmlLiveData.addSource(entryLiveData) { source ->
            if (source != null) {
                entry = source
                isExcerpt = source.content?.startsWith(FeedParser.FLAG_EXCERPT) ?: false
                drawHtml(source)
            } else htmlLiveData.value = null
        }
    }

    fun getEntryById(entryId: String) {
        entryIdLiveData.value = entryId
    }

    fun setTextSize(textSize: Int) {
        this.textSize = textSize
        entryLiveData.value?.let { entry -> drawHtml(entry) }
    }

    private fun drawHtml(entry: Entry) {
        EntryMinimal(
            title = entry.title, date = entry.date, author = entry.author,
            content = entry.content?.removePrefix(FeedParser.FLAG_EXCERPT) ?: ""
        ).let {
            htmlLiveData.value = EntryToHtmlFormatter(textSize, font, !bannerIsEnabled).getHtml(it)
        }
    }

    fun saveChanges() {
        entry?.let { repo.updateEntryAndFeedUnreadCount(it.url, true, it.isStarred) }
    }
}