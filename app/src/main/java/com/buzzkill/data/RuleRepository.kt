package com.buzzkill.data

import android.content.Context
import com.buzzkill.data.db.AppDatabase
import com.buzzkill.data.db.BuzzJson
import com.buzzkill.data.db.RuleDao
import com.buzzkill.data.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer

/**
 * Single point of access to rule storage. Holds a process-wide singleton so the
 * listener service and UI share the same database instance.
 */
class RuleRepository private constructor(private val dao: RuleDao) {

    fun observeAll(): Flow<List<Rule>> = dao.observeAll()
    fun observeById(id: Long): Flow<Rule?> = dao.observeById(id)
    suspend fun enabledRules(): List<Rule> = dao.enabledRules()
    suspend fun byId(id: Long): Rule? = dao.byId(id)

    suspend fun upsert(rule: Rule): Long {
        return if (rule.id == 0L) {
            val order = dao.maxSortOrder() + 1
            dao.insert(rule.copy(sortOrder = order))
        } else {
            dao.update(rule)
            rule.id
        }
    }

    suspend fun delete(rule: Rule) = dao.delete(rule)
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
    suspend fun incrementFireCount(id: Long) = dao.incrementFireCount(id)

    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.setSortOrder(id, index) }
    }

    /** Serialize all rules to JSON for backup/export. */
    suspend fun exportJson(): String =
        BuzzJson.encodeToString(ListSerializer(Rule.serializer()), dao.allOnce())

    /** Import rules from exported JSON, inserting as new rows. */
    suspend fun importJson(json: String): Int {
        val rules = BuzzJson.decodeFromString(ListSerializer(Rule.serializer()), json)
        rules.forEach { dao.insert(it.copy(id = 0)) }
        return rules.size
    }

    companion object {
        @Volatile
        private var instance: RuleRepository? = null

        fun get(context: Context): RuleRepository =
            instance ?: synchronized(this) {
                instance ?: RuleRepository(AppDatabase.get(context).ruleDao())
                    .also { instance = it }
            }
    }
}
