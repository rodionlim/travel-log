package com.wanderlog.android.data.sync

import androidx.room.withTransaction
import com.wanderlog.android.data.local.WanderlogDatabase
import com.wanderlog.android.data.local.dao.AttachmentDao
import com.wanderlog.android.data.local.dao.ExpenseDao
import com.wanderlog.android.data.local.dao.ItineraryItemDao
import com.wanderlog.android.data.local.dao.PackingItemDao
import com.wanderlog.android.data.local.dao.TripDao
import com.wanderlog.android.data.local.dao.TripDayDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncTombstoneResetter @Inject constructor(
    private val database: WanderlogDatabase,
    private val tripDao: TripDao,
    private val tripDayDao: TripDayDao,
    private val itineraryItemDao: ItineraryItemDao,
    private val expenseDao: ExpenseDao,
    private val packingItemDao: PackingItemDao,
    private val attachmentDao: AttachmentDao
) {

    suspend fun reset() {
        database.withTransaction { resetInternal() }
    }

    internal suspend fun resetInternal() {
        attachmentDao.purgeDeletedAttachments()
        itineraryItemDao.purgeDeletedItems()
        expenseDao.purgeDeletedExpenses()
        packingItemDao.purgeDeletedItems()
        tripDayDao.purgeDeletedDays()
        tripDao.purgeDeletedTrips()
    }
}
