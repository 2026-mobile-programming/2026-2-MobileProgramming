package com.app.jjanchall.util

/**
 * 로그인·회원가입 입력값 검사.
 * 안드로이드 클래스를 쓰지 않아서 단위 테스트로 바로 확인할 수 있어요.
 */
object AuthValidator {

    const val PASSWORD_MIN_LENGTH = 6
    const val NICKNAME_MIN_LENGTH = 2
    const val NICKNAME_MAX_LENGTH = 10

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** 앞뒤 공백을 지우고 소문자로 맞춘 이메일. 계정을 찾을 때 이 값을 키로 써요. */
    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= PASSWORD_MIN_LENGTH

    fun isValidNickname(nickname: String): Boolean =
        nickname.trim().length in NICKNAME_MIN_LENGTH..NICKNAME_MAX_LENGTH
}
