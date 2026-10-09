package com.app.jjanchall.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * 비밀번호를 그대로 저장하지 않고 "소금(salt) + SHA-256 해시"로 바꿔 저장해요.
 * 같은 비밀번호라도 계정마다 salt가 달라서 저장값이 달라져요.
 */
object PasswordHasher {

    private const val SALT_BYTES = 16

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Base64.getDecoder().decode(salt))
        val hashed = digest.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashed)
    }

    fun matches(password: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(
            hash(password, salt).toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8),
        )
}
