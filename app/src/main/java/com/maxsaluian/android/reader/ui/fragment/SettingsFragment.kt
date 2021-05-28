package com.maxsaluian.android.reader.ui.fragment

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.maxsaluian.android.reader.R
import com.maxsaluian.android.reader.data.local.Preferences
import com.maxsaluian.android.reader.ui.OnToolbarInflated
import com.maxsaluian.android.reader.ui.dialog.AboutFragment
import com.maxsaluian.android.reader.util.Utils
import com.maxsaluian.android.reader.util.work.BackgroundSyncWorker

class SettingsFragment : VisibleFragment(), AboutFragment.Callback {

    interface Callbacks : OnToolbarInflated

    private lateinit var toolbar: Toolbar
    private lateinit var scrollView: ScrollView
    private lateinit var autoUpdateSwitch: SwitchCompat
    private lateinit var browserSwitch: SwitchCompat
    private lateinit var syncSwitch: SwitchCompat
    private lateinit var keepEntriesSwitch: SwitchCompat
    private lateinit var themeSpinner: Spinner
    private lateinit var sortFeedsSpinner: Spinner
    private lateinit var sortEntriesSpinner: Spinner

    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        toolbar = view.findViewById(R.id.toolbar)
        scrollView = view.findViewById(R.id.scroll_view)
        autoUpdateSwitch = view.findViewById(R.id.auto_update_switch)
        browserSwitch = view.findViewById(R.id.browser_switch)
        syncSwitch = view.findViewById(R.id.sync_switch)
        keepEntriesSwitch = view.findViewById(R.id.keep_entries_switch)
        themeSpinner = view.findViewById(R.id.theme_spinner)
        sortFeedsSpinner = view.findViewById(R.id.sort_feeds_spinner)
        sortEntriesSpinner = view.findViewById(R.id.sort_entries_spinner)
        setupToolbar()
        setHasOptionsMenu(true)
        return view
    }

    private fun setupToolbar() {
        toolbar.title = getString(R.string.settings)
        callbacks?.onToolbarInflated(toolbar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        themeSpinner.apply {
            adapter = arrayOf(
                getString(R.string.system_default),
                getString(R.string.light),
                getString(R.string.dark)
            ).run { getDefaultAdapter(context, this) }
            setSelection(Preferences.getTheme(context))
            onItemSelectedListener = getSpinnerListener(context, ACTION_SAVE_THEME)
        }

        sortFeedsSpinner.apply {
            adapter = arrayOf(
                getString(R.string.title),
                getString(R.string.unread_items)
            ).run { getDefaultAdapter(context, this) }
            setSelection(Preferences.getFeedsOrder(context))
            onItemSelectedListener = getSpinnerListener(context, ACTION_SAVE_FEEDS_ORDER)
        }

        sortEntriesSpinner.apply {
            adapter = arrayOf(
                getString(R.string.date_published),
                getString(R.string.unread_on_top)
            ).run { getDefaultAdapter(context, this) }
            setSelection(Preferences.getEntriesOrder(context))
            onItemSelectedListener = getSpinnerListener(context, ACTION_SAVE_ENTRIES_ORDER)
        }

        autoUpdateSwitch.apply {
            isChecked = Preferences.getAutoUpdateSetting(context)
            setOnCheckedChangeListener { _, isOn ->
                Preferences.saveAutoUpdateSetting(context, isOn)
            }
        }

        keepEntriesSwitch.apply {
            isChecked = Preferences.keepOldUnreadEntries(context)
            setOnCheckedChangeListener { _, isOn ->
                Preferences.setKeepOldUnreadEntries(context, isOn)
            }
        }

        syncSwitch.apply {
            isChecked = Preferences.syncInBackground(context)
            setOnCheckedChangeListener { _, isOn ->
                Preferences.setSyncInBackground(context, isOn)
                if (isOn) BackgroundSyncWorker.start(context) else BackgroundSyncWorker.cancel(
                    context
                )
            }
        }

        browserSwitch.apply {
            // Values are reversed on purpose
            isChecked = !Preferences.getBrowserSetting(context)
            setOnCheckedChangeListener { _, isOn ->
                Preferences.setBrowserSetting(context, !isOn)
            }
        }

    }


    private fun getDefaultAdapter(context: Context, items: Array<String>): ArrayAdapter<String> {
        return ArrayAdapter(context, android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun getSpinnerListener(
        context: Context,
        action: Int
    ): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (action) {
                    ACTION_SAVE_THEME -> {
                        if (Build.VERSION.SDK_INT < 29) {
                            if (position == 0) {
                                Preferences.saveTheme(context, position)
                                setDarkMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                            }
                            if (position == 2) {
                                Preferences.saveTheme(context, position)
                                setDarkMode(AppCompatDelegate.MODE_NIGHT_YES)
                            } else {
                                Preferences.saveTheme(context, position)
                                Utils.setTheme(position)
                            }
                        } else {
                            Preferences.saveTheme(context, position)
                            Utils.setTheme(position)
                        }
                    }
                    ACTION_SAVE_FEEDS_ORDER -> Preferences.saveFeedsOrder(context, position)
                    ACTION_SAVE_ENTRIES_ORDER -> Preferences.saveEntriesOrder(
                        context,
                        position
                    )
                    ACTION_SAVE_FONT -> Preferences.saveFont(context, position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {} // Do nothing
        }
    }

    override fun onGoToRepoClicked() {
        Utils.openLink(requireActivity(), scrollView, Uri.parse(GITHUB_REPO))
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    companion object {

        private const val GITHUB_REPO = "https://www.github.com/maxsaluian/reader"
        private const val ACTION_SAVE_THEME = 0
        private const val ACTION_SAVE_FEEDS_ORDER = 1
        private const val ACTION_SAVE_ENTRIES_ORDER = 2
        private const val ACTION_SAVE_FONT = 3

        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    private fun setDarkMode(@AppCompatDelegate.NightMode darkMode: Int) {
        AppCompatDelegate.setDefaultNightMode(darkMode)

    }
}