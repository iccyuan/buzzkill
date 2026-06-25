package com.buzzkill.data

import android.content.Context
import com.buzzkill.data.db.BuzzJson
import com.buzzkill.data.model.DayType
import kotlinx.serialization.Serializable

/**
 * Classifies a calendar date as a Chinese statutory holiday, make-up workday,
 * weekend, or ordinary workday, using the bundled `assets/holidays.json` calendar
 * with a weekday/weekend fallback for dates/years not present in the data.
 */
object HolidayProvider {

    @Serializable
    private data class Calendar(
        val years: Map<String, YearData> = emptyMap(),
    )

    @Serializable
    private data class YearData(
        val holidays: List<String> = emptyList(),
        val workdays: List<String> = emptyList(),
    )

    @Volatile private var holidaySet: Set<String> = emptySet()
    @Volatile private var workdaySet: Set<String> = emptySet()
    @Volatile private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            runCatching {
                val json = context.applicationContext.assets.open("holidays.json")
                    .bufferedReader().use { it.readText() }
                val cal = BuzzJson.decodeFromString(Calendar.serializer(), json)
                holidaySet = cal.years.values.flatMap { it.holidays }.toSet()
                workdaySet = cal.years.values.flatMap { it.workdays }.toSet()
            }
            loaded = true
        }
    }

    /**
     * @param month 1-based month, @param isoDayOfWeek 1 = Monday … 7 = Sunday.
     */
    fun dayType(year: Int, month: Int, day: Int, isoDayOfWeek: Int): DayType {
        val key = "%04d-%02d-%02d".format(year, month, day)
        return when {
            holidaySet.contains(key) -> DayType.LEGAL_HOLIDAY
            workdaySet.contains(key) -> DayType.MAKEUP_WORKDAY
            isoDayOfWeek == 6 || isoDayOfWeek == 7 -> DayType.WEEKEND
            else -> DayType.WORKDAY
        }
    }
}
