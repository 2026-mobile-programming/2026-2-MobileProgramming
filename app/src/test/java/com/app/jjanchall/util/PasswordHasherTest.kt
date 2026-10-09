package com.app.jjanchall.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun sameSaltSameHash() {
        val salt = PasswordHasher.newSalt()
        assertEquals(PasswordHasher.hash("pw1234", salt), PasswordHasher.hash("pw1234", salt))
    }

    @Test
    fun differentSaltDifferentHash() {
        assertNotEquals(
            PasswordHasher.hash("pw1234", PasswordHasher.newSalt()),
            PasswordHasher.hash("pw1234", PasswordHasher.newSalt()),
        )
    }

    @Test
    fun matches() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("pw1234", salt)
        assertTrue(PasswordHasher.matches("pw1234", salt, hash))
        assertFalse(PasswordHasher.matches("pw12345", salt, hash))
        assertFalse(hash.contains("pw1234"))
    }
}
