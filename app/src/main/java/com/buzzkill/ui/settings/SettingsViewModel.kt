package com.buzzkill.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzzkill.BuildConfig
import com.buzzkill.data.HolidayProvider
import com.buzzkill.data.RuleRepository
import com.buzzkill.data.SettingsStore
import com.buzzkill.data.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = RuleRepository.get(app)
    private val settings = SettingsStore.get(app)

    val masterEnabled: StateFlow<Boolean> = settings.masterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val logActivity: StateFlow<Boolean> = settings.logActivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hideFromRecents: StateFlow<Boolean> = settings.hideFromRecents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setMasterEnabled(value: Boolean) = viewModelScope.launch {
        settings.setMasterEnabled(value)
    }

    fun setLogActivity(value: Boolean) = viewModelScope.launch {
        settings.setLogActivity(value)
    }

    fun setHideFromRecents(value: Boolean) = viewModelScope.launch {
        settings.setHideFromRecents(value)
    }

    fun exportRules(onResult: (String) -> Unit) = viewModelScope.launch {
        onResult(repository.exportJson())
    }

    fun importRules(json: String, onResult: (Int) -> Unit) = viewModelScope.launch {
        val count = runCatching { repository.importJson(json) }.getOrDefault(-1)
        onResult(count)
    }

    /** 节假日数据上次更新的时间（epoch 毫秒），从未更新则为 0。 */
    val holidayUpdated = MutableStateFlow(HolidayProvider.lastUpdated(getApplication()))
    val holidayUpdating = MutableStateFlow(false)

    fun updateHolidays(onResult: (Boolean) -> Unit) = viewModelScope.launch {
        holidayUpdating.value = true
        val result = withContext(Dispatchers.IO) { HolidayProvider.refresh(getApplication()) }
        holidayUpdating.value = false
        holidayUpdated.value = HolidayProvider.lastUpdated(getApplication())
        onResult(result.ok)
    }

    /** 当前安装的版本号，用于显示与更新比较。 */
    val appVersion: String = BuildConfig.VERSION_NAME

    val updateChecking = MutableStateFlow(false)

    /** 检查 GitHub 是否有新版本。结果为 null 表示检查失败（网络等原因）。 */
    fun checkUpdate(onResult: (UpdateChecker.Result?) -> Unit) = viewModelScope.launch {
        updateChecking.value = true
        val result = withContext(Dispatchers.IO) { UpdateChecker.check(appVersion) }
        updateChecking.value = false
        onResult(result)
    }
}
