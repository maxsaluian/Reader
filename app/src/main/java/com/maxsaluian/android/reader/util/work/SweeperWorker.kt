package com.maxsaluian.android.reader.util.work

import android.content.Context
import androidx.work.*
import com.maxsaluian.android.reader.data.Repository
import java.util.concurrent.TimeUnit

class SweeperWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val repo = Repository.get()

    override fun doWork(): Result {
        repo.deleteLeftoverItems() // Just in case
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "com.maxsaluian.android.reader.utils.work.SweeperWorker"

        fun start(context: Context) {
            val request = PeriodicWorkRequest.Builder(
                SweeperWorker::class.java, 3, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}