package com.maxsaluian.android.reader.ui

import androidx.appcompat.widget.Toolbar

interface OnToolbarInflated {

    fun onToolbarInflated(toolbar: Toolbar, isNavigableUp: Boolean = true)
}