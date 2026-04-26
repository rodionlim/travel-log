package com.wanderlog.android.data.repository

import com.wanderlog.android.data.local.dao.ExpenseDao
import com.wanderlog.android.data.local.entity.ExpenseEntity
import com.wanderlog.android.data.sync.SyncMetadataStamp
import com.wanderlog.android.domain.model.Expense
import com.wanderlog.android.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao,
    private val syncMetadataStamp: SyncMetadataStamp
) : ExpenseRepository {

    override fun getExpensesForTrip(tripId: String): Flow<List<Expense>> =
        dao.getExpensesForTrip(tripId).map { it.map(ExpenseEntity::toDomain) }

    override fun getTotalSpent(tripId: String): Flow<Double?> =
        dao.getTotalSpent(tripId)

    override suspend fun insertExpense(expense: Expense) {
        val now = syncMetadataStamp.now()
        val deviceId = syncMetadataStamp.currentDeviceId()
        dao.insertExpense(
            ExpenseEntity.fromDomain(
                expense = expense,
                createdAt = now,
                updatedAt = now,
                lastModifiedByDeviceId = deviceId
            )
        )
    }

    override suspend fun updateExpense(expense: Expense) =
        dao.updateExpense(
            id = expense.id,
            title = expense.title,
            amount = expense.amount,
            currencyCode = expense.currencyCode,
            category = expense.category.name,
            date = expense.date,
            notes = expense.notes,
            updatedAt = syncMetadataStamp.now(),
            lastModifiedByDeviceId = syncMetadataStamp.currentDeviceId()
        )

    override suspend fun deleteExpense(expense: Expense) {
        dao.markDeleted(
            expenseId = expense.id,
            deletedAt = syncMetadataStamp.now(),
            lastModifiedByDeviceId = syncMetadataStamp.currentDeviceId()
        )
    }
}
