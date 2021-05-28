package com.maxsaluian.android.reader.data.model.cross

import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = ["feedUrl", "entryUrl"],
    indices = [(Index(value = ["entryUrl"]))]
)
data class FeedEntryCrossRef(
    val feedUrl: String,
    val entryUrl: String
)