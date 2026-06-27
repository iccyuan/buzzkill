package com.buzzkill.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzzkill.data.NotificationLogRepository
import com.buzzkill.data.RuleRepository
import com.buzzkill.data.db.AppCount
import com.buzzkill.data.model.Rule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InsightsViewModel(app: Application) : AndroidViewModel(app) {

    data class State(
        val total: Int = 0,
        val matched: Int = 0,
        val topApps: List<AppCount> = emptyList(),
        val topRules: List<Rule> = emptyList(),
    )

    private val logRepo = NotificationLogRepository.get(app)
    private val ruleRepo = RuleRepository.get(app)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val rules = ruleRepo.allOnce().filter { it.fireCount > 0 }.sortedByDescending { it.fireCount }.take(8)
        _state.value = State(
            total = logRepo.total(),
            matched = logRepo.matched(),
            topApps = logRepo.topApps(),
            topRules = rules,
        )
    }
}
