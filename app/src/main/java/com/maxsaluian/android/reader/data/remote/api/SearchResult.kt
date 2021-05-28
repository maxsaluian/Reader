package com.maxsaluian.android.reader.data.remote.api

import com.google.gson.annotations.SerializedName
import com.maxsaluian.android.reader.data.model.SearchResultItem

class SearchResult {
    @SerializedName("results")
    lateinit var items: List<SearchResultItem>
}