package com.app.jjanchall.data.auth

import androidx.annotation.StringRes
import com.app.jjanchall.R

/** Firebase 오류를 화면에 보여줄 문구로 바꿔요. */
enum class AuthError(@StringRes val messageRes: Int) {
    EMAIL_DUPLICATE(R.string.error_email_duplicate),
    WEAK_PASSWORD(R.string.error_password_short),
    INVALID_CREDENTIALS(R.string.login_fail_message),
    NETWORK(R.string.error_network),
    TOO_MANY_REQUESTS(R.string.error_too_many_requests),
    UNKNOWN(R.string.error_unknown),
}
