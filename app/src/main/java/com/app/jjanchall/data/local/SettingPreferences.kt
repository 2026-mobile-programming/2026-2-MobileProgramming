package com.app.jjanchall.data.local

import android.content.Context
import androidx.core.content.edit

/** 설정 화면의 알림 스위치 상태를 SharedPreferences에 저장해요. */
class SettingPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var budgetAlert: Boolean
        get() = prefs.getBoolean(KEY_BUDGET_ALERT, true)
        set(value) = prefs.edit { putBoolean(KEY_BUDGET_ALERT, value) }

    var dailyReminder: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDER, false)
        set(value) = prefs.edit { putBoolean(KEY_DAILY_REMINDER, value) }

    companion object {
        private const val PREF_NAME = "jjanchall_settings"
        private const val KEY_BUDGET_ALERT = "budget_alert"
        private const val KEY_DAILY_REMINDER = "daily_reminder"
    }
}
