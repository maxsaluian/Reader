package com.maxsaluian.android.reader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.maxsaluian.android.reader.data.Repository
import com.maxsaluian.android.reader.data.local.Preferences
import com.maxsaluian.android.reader.data.local.database.FeedDatabase
import com.maxsaluian.android.reader.util.NetworkMonitor
import com.maxsaluian.android.reader.util.Utils
import com.maxsaluian.android.reader.util.work.BackgroundSyncWorker
import com.maxsaluian.android.reader.util.work.NewEntriesWorker
import com.maxsaluian.android.reader.util.work.SweeperWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Application : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Utils.setTheme(Preferences.getTheme(this))
        val database = FeedDatabase.build(this)
        val connectionMonitor = NetworkMonitor(this)
        Repository.initialize(database, connectionMonitor)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).let { notificationManager?.createNotificationChannel(it) }
        }

        delayedInit()
    }

    private fun delayedInit() {
        val isPolling = Preferences.getPollingSetting(this)
        val isSyncing = Preferences.syncInBackground(this)

        applicationScope.launch {
            if (isPolling) NewEntriesWorker.start(applicationContext)
            if (isSyncing) BackgroundSyncWorker.start(applicationContext)
            SweeperWorker.start(applicationContext)
        }
    }

    companion object {

        const val NOTIFICATION_CHANNEL_ID = "reader_new_entries"
    }
}