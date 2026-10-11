package com.app.jjanchall.data.local

import android.content.Context
import androidx.core.content.edit

/**
 * 자동 로그인 설정을 SharedPreferences에 저장해요.
 * 계정(이메일·비밀번호·닉네임)은 Firebase Auth가 관리하고, 여기에는 체크박스 상태만 남겨요.
 */
class AuthPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var autoLogin: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOGIN, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_LOGIN, value) }

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREF_NAME = "jjanchall_auth"
        private const val KEY_AUTO_LOGIN = "auto_login"
    }
}
