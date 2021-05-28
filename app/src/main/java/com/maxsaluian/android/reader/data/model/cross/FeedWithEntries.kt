package com.maxsaluian.android.reader.data.model.cross

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.maxsaluian.android.reader.data.model.entry.Entry
import com.maxsaluian.android.reader.data.model.feed.Feed

data class FeedWithEntries(
    @Embedded val feed: Feed,
    @Relation(
        parentColumn = "url",
        entityColumn = "url",
        associateBy = Junction(
            value = FeedEntryCrossRef::class,
            parentColumn = "feedUrl",
            entityColumn = "entryUrl"
        )
    )
    val entries: List<Entry>
)