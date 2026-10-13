package com.app.jjanchall.data.auth

import android.content.Context
import com.app.jjanchall.data.local.AuthPreferences
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest

/**
 * 로그인·회원가입 담당.
 * - 계정: Firebase Auth (이메일/비밀번호), 닉네임은 계정의 displayName에 저장
 * - 자동 로그인 여부: SharedPreferences (AuthPreferences)
 * 결과는 메인 스레드에서 onResult로 전달돼요.
 */
class AuthRepository(context: Context) {

    sealed interface Result {
        data object Success : Result
        data class Failure(val error: AuthError) : Result
    }

    private val auth = FirebaseAuth.getInstance()
    private val prefs = AuthPreferences(context)

    /** 지금 로그인한 사용자의 고유 ID. 챌린지(Firestore)에서 사용자를 구분할 때 써요. */
    val currentUid: String? get() = auth.currentUser?.uid

    val currentNickname: String? get() = auth.currentUser?.displayName

    val currentEmail: String? get() = auth.currentUser?.email

    /** 설정 화면의 자동 로그인 스위치 */
    var autoLogin: Boolean
        get() = prefs.autoLogin
        set(value) {
            prefs.autoLogin = value
        }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    /** 앱을 다시 켰을 때 로그인 화면을 건너뛸지 */
    fun shouldAutoLogin(): Boolean = isLoggedIn() && prefs.autoLogin

    fun signUp(nickname: String, email: String, password: String, onResult: (Result) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .continueWithTask { task ->
                // 가입 실패 시 Firebase 오류를 그대로 넘겨야 이메일 중복 등을 구분할 수 있어요
                if (!task.isSuccessful) throw task.exception ?: IllegalStateException("sign up failed")
                val user = task.result.user ?: throw IllegalStateException("user is null")
                user.updateProfile(userProfileChangeRequest { displayName = nickname })
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    prefs.autoLogin = false
                    onResult(Result.Success)
                } else {
                    onResult(Result.Failure(task.exception.toAuthError()))
                }
            }
    }

    fun login(email: String, password: String, autoLogin: Boolean, onResult: (Result) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    prefs.autoLogin = autoLogin
                    onResult(Result.Success)
                } else {
                    onResult(Result.Failure(task.exception.toAuthError()))
                }
            }
    }

    fun logout() {
        auth.signOut()
        prefs.clear()
    }

    private fun Exception?.toAuthError(): AuthError = when (this) {
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_DUPLICATE
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidUserException -> AuthError.INVALID_CREDENTIALS
        is FirebaseAuthInvalidCredentialsException -> AuthError.INVALID_CREDENTIALS
        is FirebaseNetworkException -> AuthError.NETWORK
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
        else -> AuthError.UNKNOWN
    }
}
