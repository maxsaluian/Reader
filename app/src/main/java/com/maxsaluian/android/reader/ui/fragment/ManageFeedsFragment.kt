package com.maxsaluian.android.reader.ui.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.maxsaluian.android.reader.R
import com.maxsaluian.android.reader.data.local.Preferences
import com.maxsaluian.android.reader.data.model.feed.FeedManageable
import com.maxsaluian.android.reader.ui.OnFinished
import com.maxsaluian.android.reader.ui.OnToolbarInflated
import com.maxsaluian.android.reader.ui.adapter.FeedManagerAdapter
import com.maxsaluian.android.reader.ui.dialog.ConfirmActionFragment
import com.maxsaluian.android.reader.ui.dialog.ConfirmActionFragment.Companion.EXPORT
import com.maxsaluian.android.reader.ui.dialog.ConfirmActionFragment.Companion.REMOVE
import com.maxsaluian.android.reader.ui.dialog.EditCategoryFragment
import com.maxsaluian.android.reader.ui.dialog.EditFeedFragment
import com.maxsaluian.android.reader.ui.dialog.SortFeedManagerFragment
import com.maxsaluian.android.reader.ui.viewmodel.ManageFeedsViewModel
import com.maxsaluian.android.reader.util.extensions.hide
import com.maxsaluian.android.reader.util.extensions.show

class ManageFeedsFragment : VisibleFragment(),
    EditCategoryFragment.Callbacks,
    EditFeedFragment.Callback,
    ConfirmActionFragment.OnRemoveConfirmed,
    SortFeedManagerFragment.Callbacks,
    FeedManagerAdapter.ItemCheckBoxListener {

    interface Callbacks : OnToolbarInflated, OnFinished {
        fun onAddFeedsSelected()
        fun onExportOpmlSelected()
    }

    private lateinit var viewModel: ManageFeedsViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var selectAllCheckBox: CheckBox
    private lateinit var counterTextView: TextView
    private lateinit var emptyMessageTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedManagerAdapter
    private lateinit var speedDial: SpeedDialView
    private lateinit var searchItem: MenuItem

    private var callbacks: Callbacks? = null
    private val fragment = this@ManageFeedsFragment
    private val handler = Handler()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ManageFeedsViewModel::class.java)
        viewModel.setOrder(Preferences.getFeedManagerOrder(requireContext()))
        adapter = FeedManagerAdapter(this, viewModel.selectedItems)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_feeds, container, false)
        toolbar = view.findViewById(R.id.toolbar)
        progressBar = view.findViewById(R.id.progress_bar)
        selectAllCheckBox = view.findViewById(R.id.select_all_checkbox)
        counterTextView = view.findViewById(R.id.counter_text_view)
        emptyMessageTextView = view.findViewById(R.id.empty_message_text_view)
        speedDial = view.findViewById(R.id.speed_dial)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        toolbar.title = getString(R.string.manage_feeds)
        callbacks?.onToolbarInflated(toolbar)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar.show()
        setupSpeedDial()

        viewModel.feedsManageableLiveData.observe(viewLifecycleOwner, { feeds ->
            progressBar.hide()
            adapter.submitList(feeds)
            selectAllCheckBox.isChecked = feeds.size == viewModel.selectedItems.size
            if (feeds.size > 1) selectAllCheckBox.show() else selectAllCheckBox.hide()
            if (feeds.isEmpty()) emptyMessageTextView.show() else emptyMessageTextView.hide()
        })

        viewModel.anyIsSelected.observe(viewLifecycleOwner, { anyIsSelected ->
            updateCounter()
            if (anyIsSelected) {
                speedDial.show()
                speedDial.open()
            } else {
                speedDial.hide()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        toolbar.setOnClickListener { recyclerView.smoothScrollToPosition(0) }
        selectAllCheckBox.setOnClickListener {
            (it as CheckBox)
            if (it.isChecked) viewModel.resetSelection(adapter.currentList) else viewModel.resetSelection()
            adapter.toggleCheckBoxes(it.isChecked)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_manage_feeds, menu)
        searchItem = menu.findItem(R.id.menu_item_search)

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.clearQuery()
                resetSelection()
                return true
            }
        })

        (searchItem.actionView as SearchView).apply {
            if (viewModel.query.isNotEmpty()) {
                searchItem.expandActionView()
                setQuery(viewModel.query, false)
                clearFocus()
            }

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(queryText: String): Boolean = true

                override fun onQueryTextSubmit(queryText: String): Boolean {
                    viewModel.submitQuery(queryText)
                    clearFocus()
                    return true
                }
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_sort -> handleSortFeeds()
            R.id.menu_item_add_feeds -> {
                callbacks?.onAddFeedsSelected()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupSpeedDial() {
        speedDial.apply {
            addActionItem(defaultSpeedDialItem(R.id.fab_edit, R.drawable.ic_edit_light))
            addActionItem(defaultSpeedDialItem(R.id.fab_remove, R.drawable.ic_delete_light))

            setOnChangeListener(object : SpeedDialView.OnChangeListener {
                override fun onToggleChanged(isOpen: Boolean) {} // Blank on purpose

                override fun onMainActionSelected(): Boolean {
                    resetSelection()
                    return true
                }
            })

            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.fab_edit -> handleEditSelected()
                    R.id.fab_remove -> handleRemoveSelected()
                    R.id.fab_export -> handleExportSelected()
                }
                true
            }
        }
    }

    private fun defaultSpeedDialItem(id: Int, iconRes: Int): SpeedDialActionItem {
        return SpeedDialActionItem.Builder(id, iconRes)
            .setFabBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            .create()
    }

    private fun updateCounter() {
        val count = viewModel.selectedItems.size
        if (count > 0) {
            counterTextView.show()
            counterTextView.text = getString(R.string.number_selected, count)
        } else {
            counterTextView.hide()
        }
    }

    private fun handleEditSelected(): Boolean {
        val count = viewModel.selectedItems.size
        if (count > 1) {
            EditCategoryFragment.newInstance(viewModel.getCategories(), null, count).apply {
                setTargetFragment(fragment, 0)
                show(fragment.parentFragmentManager, "EditCategoryFragment")
            }
        } else {
            EditFeedFragment.newInstance(viewModel.selectedItems.first(), viewModel.getCategories())
                .apply {
                    setTargetFragment(fragment, 0)
                    show(fragment.parentFragmentManager, "EditFeedFragment")
                }
        }
        return true
    }

    override fun onFeedInfoSubmitted(title: String, category: String, isChanged: Boolean) {
        if (!isChanged) return
        viewModel.updateFeedDetails(viewModel.selectedItems.first().url, title, category)
        searchItem.collapseActionView()
        resetSelection()
        handler.postDelayed({
            Snackbar.make(
                recyclerView,
                getString(R.string.saved_changes_to, title),
                Snackbar.LENGTH_SHORT
            ).show()
        }, 250)
    }

    override fun onEditCategoryConfirmed(category: String) {
        val ids = mutableListOf<String>()
        for (feed in viewModel.selectedItems) ids.add(feed.url)
        viewModel.updateCategoryByFeedIds(ids, category)
        resetSelection()
        searchItem.collapseActionView()
        // Crude solution to Snackbar jumping: wait until keyboard is fully hidden
        handler.postDelayed({ showFeedsCategorizedNotice(category, ids.size) }, 400)
    }

    private fun showFeedsCategorizedNotice(category: String, count: Int) {
        val feedsUpdated = resources.getQuantityString(R.plurals.numberOfFeeds, count, count)
        Snackbar.make(
            recyclerView,
            getString(R.string.category_assigned, category, feedsUpdated),
            Snackbar.LENGTH_LONG
        ).setAction(R.string.done) { callbacks?.onFinished() }.show()
    }

    private fun handleRemoveSelected(): Boolean {
        val count = viewModel.selectedItems.size
        val title = if (count == 1) viewModel.selectedItems.first().title else null
        ConfirmActionFragment.newInstance(REMOVE, title, count).apply {
            setTargetFragment(fragment, 0)
            show(fragment.parentFragmentManager, "ConfirmActionFragment")
        }
        return true
    }

    override fun onRemoveConfirmed() {
        val feedIds = viewModel.selectedItems.map { feed -> feed.url }.toTypedArray()
        viewModel.deleteItems(*feedIds)

        if (feedIds.size == 1) {
            showFeedsRemovedNotice(title = viewModel.selectedItems.first().title)
        } else {
            showFeedsRemovedNotice(feedIds.size)
            // If last viewed feed was just deleted, prevent main page from loading it:
            val lastViewedFeedId = Preferences.getLastViewedFeedId(requireContext())
            if (feedIds.contains(lastViewedFeedId)) {
                Preferences.saveLastViewedFeedId(requireContext(), null)
            }
        }
        resetSelection()
    }

    private fun showFeedsRemovedNotice(count: Int = 1, title: String? = null) {
        val feedsRemoved =
            title ?: resources.getQuantityString(R.plurals.numberOfFeeds, count, count)
        Snackbar.make(
            recyclerView,
            getString(R.string.unsubscribed_message, feedsRemoved),
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.done) { callbacks?.onFinished() }.show()
    }

    private fun handleExportSelected(): Boolean {
        val count = viewModel.selectedItems.size
        val title = if (count == 1) viewModel.selectedItems.first().title else null
        ConfirmActionFragment.newInstance(EXPORT, title, count).apply {
            setTargetFragment(fragment, 0)
            show(fragment.parentFragmentManager, "ConfirmActionFragment")
        }
        return true
    }

    private fun handleSortFeeds(): Boolean {
        SortFeedManagerFragment.newInstance(viewModel.order).apply {
            setTargetFragment(fragment, 0)
            show(fragment.parentFragmentManager, "SortFeedManagerFragment")
        }
        return true
    }

    override fun onOrderSelected(order: Int) {
        viewModel.setOrder(order)
    }

    private fun resetSelection() {
        viewModel.resetSelection()
        selectAllCheckBox.isChecked = false
        adapter.toggleCheckBoxes(false)
    }

    override fun onItemClicked(feed: FeedManageable, isChecked: Boolean) {
        if (isChecked) {
            viewModel.addSelection(feed)
            selectAllCheckBox.isChecked = viewModel.selectedItems.size == adapter.currentList.size
        } else {
            viewModel.removeSelection(feed)
            selectAllCheckBox.isChecked = false
        }
        adapter.selectedItems = viewModel.selectedItems
    }

    override fun onAllItemsChecked(isChecked: Boolean) {
        selectAllCheckBox.isChecked = isChecked
    }

    override fun onStop() {
        super.onStop()
        context?.let { Preferences.saveFeedManagerOrder(it, viewModel.order) }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    companion object {

        fun newInstance(): ManageFeedsFragment {
            return ManageFeedsFragment()
        }
    }
}