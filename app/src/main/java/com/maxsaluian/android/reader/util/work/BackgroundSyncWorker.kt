package com.maxsaluian.android.reader.util.work

import android.content.Context
import androidx.work.*
import com.maxsaluian.android.reader.data.Repository
import com.maxsaluian.android.reader.data.local.Preferences
import com.maxsaluian.android.reader.data.model.cross.FeedWithEntries
import com.maxsaluian.android.reader.data.model.entry.Entry
import com.maxsaluian.android.reader.data.model.entry.EntryToggleable
import com.maxsaluian.android.reader.data.remote.FeedParser
import java.util.concurrent.TimeUnit

open class BackgroundSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    val repo = Repository.get()
    val parser = FeedParser(repo.networkMonitor)

    override suspend fun doWork(): Result {
        val feedUrls = repo.getFeedUrlsSynchronously()
        if (feedUrls.isEmpty()) return Result.success()

        for (url in feedUrls) {
            val storedEntries = repo.getEntriesToggleableByFeedSynchronously(url)
            val storedEntryIds: List<String> = storedEntries.map { it.url }
            val feedWithEntries: FeedWithEntries? = parser.getFeedSynchronously(url)

            feedWithEntries?.let { fwe ->
                val newEntries = fwe.entries.filterNot { storedEntryIds.contains(it.url) }
                handleRetrievedData(fwe, storedEntries, newEntries)
            }
        }

        return Result.success()
    }

    fun handleRetrievedData(
        fwe: FeedWithEntries,
        storedEntries: List<EntryToggleable>,
        newEntries: List<Entry>
    ) {
        val entryIds = fwe.entries.map { it.url }
        val oldEntries = storedEntries.filterNot { entryIds.contains(it.url) }
        val entriesToDelete = if (Preferences.keepOldUnreadEntries(context)) {
            oldEntries.filter { !it.isStarred && it.isRead }
        } else {
            oldEntries.filter { !it.isStarred }
        }

        repo.handleBackgroundUpdate(fwe.feed.url, newEntries, entriesToDelete, fwe.feed.imageUrl)
    }

    companion object {

        private const val WORK_NAME =
            "com.maxsaluian.android.reader.utils.work.BackgroundSyncWorker"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        fun start(context: Context) {
            val request = PeriodicWorkRequest.Builder(
                BackgroundSyncWorker::class.java, 24, TimeUnit.HOURS
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runOnce(context: Context) {
            val request = OneTimeWorkRequest.Builder(BackgroundSyncWorker::class.java)
                .setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}