package com.maxsaluian.android.reader.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxsaluian.android.reader.R

class SortFeedManagerFragment : BottomSheetDialogFragment() {

    interface Callbacks {
        fun onOrderSelected(order: Int)
    }

    private lateinit var radioGroupSortFeeds: RadioGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sort_feeds, container, false)
        radioGroupSortFeeds = view.findViewById(R.id.radioGroup_sort)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentSelection = arguments?.getInt(ARG_CURRENT_ORDER) ?: 0

        radioGroupSortFeeds.apply {
            check(
                when (currentSelection) {
                    SORT_BY_CATEGORY -> R.id.radioButton_category
                    SORT_BY_TITLE -> R.id.radioButton_title
                    else -> R.id.radioButton_added // Default
                }
            )

            setOnCheckedChangeListener { _, checkedId ->
                val order = when (checkedId) {
                    R.id.radioButton_category -> SORT_BY_CATEGORY
                    R.id.radioButton_title -> SORT_BY_TITLE
                    else -> SORT_BY_ADDED // Default
                }

                targetFragment?.let { fragment ->
                    (fragment as Callbacks).onOrderSelected(order)
                }
                dismiss()
            }
        }
    }

    companion object {
        private const val ARG_CURRENT_ORDER = "ARG_CURRENT_ORDER"

        const val SORT_BY_ADDED = 0
        const val SORT_BY_CATEGORY = 1
        const val SORT_BY_TITLE = 2

        fun newInstance(currentOrder: Int): SortFeedManagerFragment {
            val args = Bundle().apply {
                putInt(ARG_CURRENT_ORDER, currentOrder)
            }
            return SortFeedManagerFragment().apply {
                arguments = args
            }
        }
    }
}