package com.app.jjanchall.data.local

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth

/** 월 예산과 알림 시점을 SharedPreferences에 계정(uid)별로 저장 */
class BudgetPreferences(context: Context) {

    enum class AlertType { PERCENT_80, OVER, NONE }

    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val uid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    /** 한 달 예산 (원). 0이면 설정 안 함 */
    var monthlyBudget: Int
        get() = prefs.getInt(key(KEY_BUDGET), 0)
        set(value) = prefs.edit { putInt(key(KEY_BUDGET), value) }

    var alertType: AlertType
        get() = prefs.getString(key(KEY_ALERT_TYPE), null)
            ?.let { name -> AlertType.entries.find { it.name == name } }
            ?: AlertType.OVER
        set(value) = prefs.edit { putString(key(KEY_ALERT_TYPE), value.name) }

    private fun key(name: String) = "${name}_$uid"

    companion object {
        private const val PREF_NAME = "jjanchall_budget"
        private const val KEY_BUDGET = "monthly_budget"
        private const val KEY_ALERT_TYPE = "alert_type"
    }
}
