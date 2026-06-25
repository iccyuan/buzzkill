package com.buzzkill.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzzkill.data.RuleRepository
import com.buzzkill.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = RuleRepository.get(app)
    private val settings = SettingsStore.get(app)

    val masterEnabled: StateFlow<Boolean> = settings.masterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val logActivity: StateFlow<Boolean> = settings.logActivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setMasterEnabled(value: Boolean) = viewModelScope.launch {
        settings.setMasterEnabled(value)
    }

    fun setLogActivity(value: Boolean) = viewModelScope.launch {
        settings.setLogActivity(value)
    }

    fun exportRules(onResult: (String) -> Unit) = viewModelScope.launch {
        onResult(repository.exportJson())
    }

    fun importRules(json: String, onResult: (Int) -> Unit) = viewModelScope.launch {
        val count = runCatching { repository.importJson(json) }.getOrDefault(-1)
        onResult(count)
    }
}
