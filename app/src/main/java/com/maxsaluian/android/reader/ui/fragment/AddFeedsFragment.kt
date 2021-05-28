package com.maxsaluian.android.reader.ui.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maxsaluian.android.reader.R
import com.maxsaluian.android.reader.ui.FeedRequestCallbacks
import com.maxsaluian.android.reader.ui.adapter.TopicAdapter
import com.maxsaluian.android.reader.ui.dialog.InputUrlFragment
import com.maxsaluian.android.reader.ui.viewmodel.AddFeedsViewModel
import java.util.*

class AddFeedsFragment : FeedAddingFragment(),
    TopicAdapter.OnItemClickListener,
    FeedRequestCallbacks {

    private lateinit var viewModel: AddFeedsViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var linearLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TopicAdapter
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AddFeedsViewModel::class.java)
        viewModel.initDefaultTopics(viewModel.defaultTopicsResId.map { getString(it) })
        adapter = TopicAdapter(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_feeds, container, false)
        toolbar = view.findViewById(R.id.toolbar)
        linearLayout = view.findViewById(R.id.linearLayout)
        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.recycler_view)
        setupRecyclerView()
        setupToolbar()
        return view
    }

    private fun setupRecyclerView() {
        val span =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 5
        recyclerView.layoutManager = GridLayoutManager(context, span)
        recyclerView.adapter = adapter.apply { numOfItems = if (span == 3) 9 else 10 }
    }

    private fun setupToolbar() {
        toolbar.title = getString(R.string.add_feeds)
        callbacks?.onToolbarInflated(toolbar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resultManager = RequestResultManager(viewModel, linearLayout, R.string.failed_to_get_feed)

        viewModel.feedIdsWithCategoriesLiveData.observe(viewLifecycleOwner, { data ->
            viewModel.onFeedDataRetrieved(data)
        })

        viewModel.topicBlocksLiveData.observe(viewLifecycleOwner, { topics ->
            adapter.submitList(topics.toMutableList())
        })

        viewModel.feedRequestLiveData.observe(viewLifecycleOwner, { feedWithEntries ->
            // A little delay to prevent resulting snackbar from jumping:
            Handler().postDelayed({ resultManager?.submitData(feedWithEntries) }, 250)
            if (viewModel.isActiveRequest) {
                parentFragmentManager.findFragmentByTag(InputUrlFragment.TAG).let { fragment ->
                    (fragment as? DialogFragment)?.dismiss()
                    viewModel.isActiveRequest = false
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(queryText: String): Boolean {
                if (queryText.isNotBlank()) callbacks?.onQuerySubmitted(queryText)
                return true
            }

            override fun onQueryTextChange(queryText: String?): Boolean {
                return true
            }
        })

    }

    override fun onRequestSubmitted(url: String, backup: String?) {
        viewModel.lastInputUrl = url
        val link = url.toLowerCase(Locale.ROOT).trim()
        if (link.contains("://")) {
            viewModel.requestFeed(url) // If scheme is provided, use as is
        } else {
            viewModel.requestFeed("https://$link", "http://$link")
        }
    }

    override fun onRequestDismissed() {
        // Wait for dialog to close fully to prevent snackbar from jumping
        Handler().postDelayed({ resultManager?.onRequestDismissed() }, 250)
    }


    override fun onTopicSelected(topic: String) {
        callbacks?.onQuerySubmitted(topic)
    }

    companion object {

        fun newInstance(): AddFeedsFragment {
            return AddFeedsFragment()
        }
    }
}