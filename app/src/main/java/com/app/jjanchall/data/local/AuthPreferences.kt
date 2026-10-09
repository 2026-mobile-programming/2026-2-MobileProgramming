package com.app.jjanchall.data.local

import android.content.Context
import androidx.core.content.edit
import com.app.jjanchall.util.AuthValidator
import com.app.jjanchall.util.PasswordHasher

/**
 * SharedPreferences에 계정과 로그인 상태를 저장해요. (서버 없이 기기 안에만 저장)
 *
 * 저장 형태 (파일 이름: jjanchall_auth)
 *   account_<이메일>_nickname / _salt / _hash  → 계정 정보 (비밀번호는 해시만 저장)
 *   session_email                             → 지금 로그인한 이메일
 *   auto_login                                → 자동 로그인 체크 여부
 */
class AuthPreferences(context: Context) {

    enum class SignUpResult { SUCCESS, DUPLICATE_EMAIL }

    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isRegistered(email: String): Boolean =
        prefs.contains(key(email, KEY_HASH))

    fun signUp(nickname: String, email: String, password: String): SignUpResult {
        if (isRegistered(email)) return SignUpResult.DUPLICATE_EMAIL

        val salt = PasswordHasher.newSalt()
        prefs.edit {
            putString(key(email, KEY_NICKNAME), nickname.trim())
            putString(key(email, KEY_SALT), salt)
            putString(key(email, KEY_HASH), PasswordHasher.hash(password, salt))
        }
        return SignUpResult.SUCCESS
    }

    /** 이메일·비밀번호가 맞으면 로그인 상태를 저장하고 true를 돌려줘요. */
    fun login(email: String, password: String, autoLogin: Boolean): Boolean {
        val salt = prefs.getString(key(email, KEY_SALT), null) ?: return false
        val hash = prefs.getString(key(email, KEY_HASH), null) ?: return false
        if (!PasswordHasher.matches(password, salt, hash)) return false

        prefs.edit {
            putString(KEY_SESSION_EMAIL, AuthValidator.normalizeEmail(email))
            putBoolean(KEY_AUTO_LOGIN, autoLogin)
        }
        return true
    }

    fun logout() {
        prefs.edit {
            remove(KEY_SESSION_EMAIL)
            remove(KEY_AUTO_LOGIN)
        }
    }

    /** 앱을 다시 켰을 때 로그인 화면을 건너뛸지 */
    fun shouldAutoLogin(): Boolean =
        prefs.getBoolean(KEY_AUTO_LOGIN, false) && currentEmail() != null

    fun currentEmail(): String? = prefs.getString(KEY_SESSION_EMAIL, null)

    fun currentNickname(): String? =
        currentEmail()?.let { prefs.getString(key(it, KEY_NICKNAME), null) }

    private fun key(email: String, field: String) =
        "account_${AuthValidator.normalizeEmail(email)}_$field"

    companion object {
        private const val PREF_NAME = "jjanchall_auth"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_SESSION_EMAIL = "session_email"
        private const val KEY_AUTO_LOGIN = "auto_login"
    }
}
