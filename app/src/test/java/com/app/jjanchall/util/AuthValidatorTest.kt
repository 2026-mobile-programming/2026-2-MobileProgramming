package com.app.jjanchall.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun email() {
        assertTrue(AuthValidator.isValidEmail("example@sungshin.ac.kr"))
        assertTrue(AuthValidator.isValidEmail("  a.b+c@test.com "))
        assertFalse(AuthValidator.isValidEmail(""))
        assertFalse(AuthValidator.isValidEmail("example"))
        assertFalse(AuthValidator.isValidEmail("example@"))
        assertFalse(AuthValidator.isValidEmail("example@sungshin"))
    }

    @Test
    fun normalizeEmail() {
        assertEquals("abc@test.com", AuthValidator.normalizeEmail("  ABC@Test.com "))
    }

    @Test
    fun password() {
        assertFalse(AuthValidator.isValidPassword("12345"))
        assertTrue(AuthValidator.isValidPassword("123456"))
    }

    @Test
    fun nickname() {
        assertFalse(AuthValidator.isValidNickname("짠"))
        assertTrue(AuthValidator.isValidNickname("짠순이"))
        assertTrue(AuthValidator.isValidNickname("가나다라마바사아자차"))
        assertFalse(AuthValidator.isValidNickname("가나다라마바사아자차카"))
        assertFalse(AuthValidator.isValidNickname("   "))
    }
}
