package com.astute.calories.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.astute.calories.data.repository.DailyLogRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DailyResetWorkerTest {

    @Test
    fun `doWork deletes entries strictly before yesterday`() = runTest {
        val repository: DailyLogRepository = mockk(relaxed = true)
        val context: Context = mockk(relaxed = true)
        val params: WorkerParameters = mockk(relaxed = true)
        val worker = DailyResetWorker(context, params, repository)
        val cutoffSlot = slot<LocalDate>()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { repository.deleteEntriesBefore(capture(cutoffSlot)) }
        // Yesterday (relative to whenever the worker runs) is preserved by passing it
        // as the cutoff: deleteEntriesBefore is exclusive.
        assertEquals(LocalDate.now().minusDays(1), cutoffSlot.captured)
    }
}
