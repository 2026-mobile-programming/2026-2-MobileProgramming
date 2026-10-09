package com.app.jjanchall.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.app.jjanchall.R
import com.app.jjanchall.data.local.AuthPreferences
import com.app.jjanchall.databinding.ActivityLoginBinding
import com.app.jjanchall.ui.main.MainActivity
import com.app.jjanchall.util.AuthValidator
import com.app.jjanchall.util.applySystemBarsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** LOGIN-01 로그인 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authPrefs: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authPrefs = AuthPreferences(this)

        // 자동 로그인: 로그인 화면을 보여주지 않고 바로 메인으로
        if (authPrefs.shouldAutoLogin()) {
            goToMain()
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        // 다시 입력하면 에러 문구 지우기
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        var valid = true
        when {
            email.isEmpty() -> {
                binding.tilEmail.error = getString(R.string.error_email_empty)
                valid = false
            }
            !AuthValidator.isValidEmail(email) -> {
                binding.tilEmail.error = getString(R.string.error_email_invalid)
                valid = false
            }
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_empty)
            valid = false
        }
        if (!valid) return

        if (authPrefs.login(email, password, binding.cbAutoLogin.isChecked)) {
            goToMain()
        } else {
            showLoginFailDialog()
        }
    }

    private fun showLoginFailDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.login_fail_title)
            .setMessage(R.string.login_fail_message)
            .setPositiveButton(R.string.common_confirm, null)
            .show()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
