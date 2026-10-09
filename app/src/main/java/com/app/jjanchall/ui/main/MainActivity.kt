package com.app.jjanchall.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.app.jjanchall.R
import com.app.jjanchall.data.local.AuthPreferences
import com.app.jjanchall.databinding.ActivityMainBinding
import com.app.jjanchall.ui.login.LoginActivity
import com.app.jjanchall.util.applySystemBarsPadding

/**
 * 메인 화면 (임시)
 * 지금은 로그인 확인용이고, 나중에 하단 탭 + 홈/기록/챌린지/설정 Fragment를 담는 화면이 돼요.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authPrefs: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authPrefs = AuthPreferences(this)

        // 로그인 정보가 없으면 로그인 화면으로
        val nickname = authPrefs.currentNickname()
        if (nickname == null) {
            goToLogin()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.tvWelcome.text = getString(R.string.main_welcome, nickname)
        binding.btnLogout.setOnClickListener {
            authPrefs.logout()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
