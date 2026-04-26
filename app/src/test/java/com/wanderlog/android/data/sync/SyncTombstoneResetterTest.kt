package com.wanderlog.android.data.sync

import com.wanderlog.android.data.local.WanderlogDatabase
import com.wanderlog.android.data.local.dao.AttachmentDao
import com.wanderlog.android.data.local.dao.ExpenseDao
import com.wanderlog.android.data.local.dao.ItineraryItemDao
import com.wanderlog.android.data.local.dao.PackingItemDao
import com.wanderlog.android.data.local.dao.TripDao
import com.wanderlog.android.data.local.dao.TripDayDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncTombstoneResetterTest {

    @Test
    fun `reset purges deleted rows from all sync tables`() = runTest {
        val database = mockk<WanderlogDatabase>(relaxed = true)
        val tripDao = mockk<TripDao>(relaxed = true)
        val tripDayDao = mockk<TripDayDao>(relaxed = true)
        val itineraryItemDao = mockk<ItineraryItemDao>(relaxed = true)
        val expenseDao = mockk<ExpenseDao>(relaxed = true)
        val packingItemDao = mockk<PackingItemDao>(relaxed = true)
        val attachmentDao = mockk<AttachmentDao>(relaxed = true)
        val resetter = SyncTombstoneResetter(
            database = database,
            tripDao = tripDao,
            tripDayDao = tripDayDao,
            itineraryItemDao = itineraryItemDao,
            expenseDao = expenseDao,
            packingItemDao = packingItemDao,
            attachmentDao = attachmentDao
        )

        resetter.resetInternal()

        coVerify(exactly = 1) { attachmentDao.purgeDeletedAttachments() }
        coVerify(exactly = 1) { itineraryItemDao.purgeDeletedItems() }
        coVerify(exactly = 1) { expenseDao.purgeDeletedExpenses() }
        coVerify(exactly = 1) { packingItemDao.purgeDeletedItems() }
        coVerify(exactly = 1) { tripDayDao.purgeDeletedDays() }
        coVerify(exactly = 1) { tripDao.purgeDeletedTrips() }
    }
}
