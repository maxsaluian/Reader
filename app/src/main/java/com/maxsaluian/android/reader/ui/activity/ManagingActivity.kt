package com.maxsaluian.android.reader.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.maxsaluian.android.reader.R
import com.maxsaluian.android.reader.ui.fragment.*
import com.maxsaluian.android.reader.util.Utils

class ManagingActivity : AppCompatActivity(),
    ManageFeedsFragment.Callbacks,
    FeedAddingFragment.Callbacks,
    SettingsFragment.Callbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_managing)
        Utils.setStatusBarMode(this)

        if (getCurrentFragment() == null) {
            when (intent.getIntExtra(EXTRA_MANAGING, FeedListFragment.ITEM_ADD_FEEDS)) {
                FeedListFragment.ITEM_ADD_FEEDS -> AddFeedsFragment.newInstance()
                FeedListFragment.ITEM_MANAGE_FEEDS -> ManageFeedsFragment.newInstance()
                FeedListFragment.ITEM_SETTINGS -> SettingsFragment.newInstance()
                else -> throw IllegalArgumentException()
            }.let { fragment ->
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
    }

    override fun onNewFeedAdded(feedId: String) {
        Intent().apply {
            putExtra(MainActivity.EXTRA_FEED_ID, feedId)
        }.also { intent ->
            setResult(Activity.RESULT_OK, intent)
        }
        finish()
    }

    override fun onQuerySubmitted(query: String) {
        replaceFragment(SearchFeedsFragment.newInstance(query))
    }

    override fun onImportOpmlSelected() {
        TODO("Not yet implemented")
    }


    override fun onAddFeedsSelected() {
        replaceFragment(AddFeedsFragment.newInstance())
        supportActionBar?.title = getString(R.string.add_feeds)
    }

    override fun onExportOpmlSelected() {
        TODO("Not yet implemented")
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onToolbarInflated(toolbar: Toolbar, isNavigableUp: Boolean) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(isNavigableUp)
    }

    override fun onFinished() {
        finish()
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fragment_container)
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        if (addToBackStack) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }

    companion object {

        private const val EXTRA_MANAGING = "com.maxsaluian.android.reader.managing"

        fun newIntent(packageContext: Context, item: Int): Intent {
            return Intent(packageContext, ManagingActivity::class.java).apply {
                putExtra(EXTRA_MANAGING, item)
            }
        }
    }
}