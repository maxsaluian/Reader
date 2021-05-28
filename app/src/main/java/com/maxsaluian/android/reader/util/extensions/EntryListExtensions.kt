package com.maxsaluian.android.reader.util.extensions

import com.maxsaluian.android.reader.data.model.entry.Entry
import com.maxsaluian.android.reader.data.model.entry.EntryLight

fun List<Entry>.sortedByDate() = this.sortedByDescending { it.date }

@JvmName("sortedByDateEntryLight")
fun List<EntryLight>.sortedByDate() = this.sortedByDescending { it.date }

fun List<EntryLight>.sortedUnreadOnTop() = this.sortedByDate().sortedBy { it.isRead }