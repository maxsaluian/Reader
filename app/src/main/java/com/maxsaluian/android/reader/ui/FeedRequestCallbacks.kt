package com.maxsaluian.android.reader.ui

interface FeedRequestCallbacks {

    fun onRequestSubmitted(url: String, backup: String? = null)

    fun onRequestDismissed()
}