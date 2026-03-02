package com.astute.calories.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.astute.calories.data.repository.DailyLogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dailyLogRepository: DailyLogRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val yesterday = LocalDate.now().minusDays(1)
        // Delete anything older than yesterday
        dailyLogRepository.deleteEntriesBefore(yesterday)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_reset"
    }
}
