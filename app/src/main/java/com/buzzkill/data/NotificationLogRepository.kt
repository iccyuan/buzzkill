package com.buzzkill.data

import android.content.Context
import com.buzzkill.data.db.AppDatabase
import com.buzzkill.data.db.NotificationLogDao
import com.buzzkill.data.model.NotificationLog
import kotlinx.coroutines.flow.Flow

/** Access to the rolling notification-activity log. */
class NotificationLogRepository private constructor(private val dao: NotificationLogDao) {

    // Inserts since the last prune. We prune every PRUNE_EVERY inserts instead of running
    // a COUNT(*) on every notification — the hot path stays a single INSERT.
    private val sincePrune = java.util.concurrent.atomic.AtomicInteger(0)

    fun observeRecent(limit: Int = MAX_ROWS): Flow<List<NotificationLog>> = dao.observeRecent(limit)
    suspend fun recent(limit: Int = MAX_ROWS): List<NotificationLog> = dao.recent(limit)
    suspend fun clear() = dao.clear()
    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun add(log: NotificationLog) {
        dao.insert(log)
        // Keep the table bounded without counting on every insert.
        if (sincePrune.incrementAndGet() >= PRUNE_EVERY) {
            sincePrune.set(0)
            dao.prune(MAX_ROWS)
        }
    }

    companion object {
        const val MAX_ROWS = 1000
        private const val PRUNE_EVERY = 100

        @Volatile
        private var instance: NotificationLogRepository? = null

        fun get(context: Context): NotificationLogRepository =
            instance ?: synchronized(this) {
                instance ?: NotificationLogRepository(
                    AppDatabase.get(context).notificationLogDao()
                ).also { instance = it }
            }
    }
}
