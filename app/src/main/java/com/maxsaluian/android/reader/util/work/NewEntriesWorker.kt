package com.maxsaluian.android.reader.util.work

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.maxsaluian.android.reader.data.local.Preferences
import com.maxsaluian.android.reader.data.model.cross.FeedWithEntries
import java.util.concurrent.TimeUnit

class NewEntriesWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : BackgroundSyncWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val feedUrls = repo.getFeedUrlsSynchronously()
        if (feedUrls.isEmpty()) return Result.success()
        val lastIndex = Preferences.getLastPolledIndex(context)
        val newIndex = if (lastIndex + 1 >= feedUrls.size) 0 else lastIndex + 1
        val url = feedUrls[newIndex]

        val feedData = repo.getFeedTitleWithEntriesToggleableSynchronously(url)
        feedData.feedTitle // Need user-set title saved in DB
        val storedEntries = feedData.entriesToggleable
        val storedEntryIds: List<String> = storedEntries.map { it.url }
        val feedWithEntries: FeedWithEntries? = parser.getFeedSynchronously(url)

        feedWithEntries?.let { fwe ->
            val newEntries = fwe.entries.filterNot { storedEntryIds.contains(it.url) }
            handleRetrievedData(fwe, storedEntries, newEntries)

            if (newEntries.isNotEmpty()) {
                Intent(ACTION_SHOW_NOTIFICATION).apply {
                }.also { intent -> context.sendOrderedBroadcast(intent, PERM_PRIVATE) }
            }
        }

        Preferences.saveLastPolledIndex(context, newIndex)
        return Result.success()
    }

    companion object {

        private const val WORK_NAME = "com.maxsaluian.android.reader.utils.work.NewEntriesWorker"
        const val ACTION_SHOW_NOTIFICATION =
            "com.maxsaluian.android.reader.utils.work.SHOW_NOTIFICATION"
        const val PERM_PRIVATE = "com.maxsaluian.android.reader.PRIVATE"

        fun start(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
            val request = PeriodicWorkRequest.Builder(
                NewEntriesWorker::class.java, 20, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

    }
}