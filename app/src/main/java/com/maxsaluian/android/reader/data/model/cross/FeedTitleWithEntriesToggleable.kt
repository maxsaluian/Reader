package com.maxsaluian.android.reader.data.model.cross

import com.maxsaluian.android.reader.data.model.entry.EntryToggleable

data class FeedTitleWithEntriesToggleable(
    val feedTitle: String,
    val entriesToggleable: List<EntryToggleable>
)