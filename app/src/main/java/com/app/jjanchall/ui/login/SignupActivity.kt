package com.app.jjanchall.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.app.jjanchall.R
import com.app.jjanchall.data.auth.AuthError
import com.app.jjanchall.data.auth.AuthRepository
import com.app.jjanchall.databinding.ActivitySignupBinding
import com.app.jjanchall.ui.main.MainActivity
import com.app.jjanchall.util.AuthValidator
import com.app.jjanchall.util.applySystemBarsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** LOGIN-02 회원가입 */
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()
        authRepository = AuthRepository(this)

        // <include>로 가져온 공통 상단 바
        binding.appBar.tvAppBarTitle.setText(R.string.signup_title)
        binding.appBar.btnBack.setOnClickListener { finish() }

        binding.etNickname.doAfterTextChanged { binding.tilNickname.error = null }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
        binding.etPasswordConfirm.doAfterTextChanged { binding.tilPasswordConfirm.error = null }

        binding.btnSignup.setOnClickListener { attemptSignup() }
    }

    private fun attemptSignup() {
        val nickname = binding.etNickname.text?.toString().orEmpty().trim()
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        val passwordConfirm = binding.etPasswordConfirm.text?.toString().orEmpty()

        var valid = true

        when {
            nickname.isEmpty() -> {
                binding.tilNickname.error = getString(R.string.error_nickname_empty)
                valid = false
            }
            !AuthValidator.isValidNickname(nickname) -> {
                binding.tilNickname.error = getString(R.string.error_nickname_length)
                valid = false
            }
        }

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

        when {
            password.isEmpty() -> {
                binding.tilPassword.error = getString(R.string.error_password_empty)
                valid = false
            }
            !AuthValidator.isValidPassword(password) -> {
                binding.tilPassword.error = getString(R.string.error_password_short)
                valid = false
            }
        }

        if (passwordConfirm != password) {
            binding.tilPasswordConfirm.error = getString(R.string.error_password_mismatch)
            valid = false
        }

        if (!valid) return

        // 응답이 올 때까지 버튼을 막아 두 번 눌리지 않게
        binding.btnSignup.isEnabled = false
        authRepository.signUp(nickname, email, password) { result ->
            if (isFinishing || isDestroyed) return@signUp
            binding.btnSignup.isEnabled = true
            when (result) {
                AuthRepository.Result.Success -> onSignupSuccess(nickname)
                is AuthRepository.Result.Failure -> showSignupError(result.error)
            }
        }
    }

    private fun onSignupSuccess(nickname: String) {
        // 가입하면 Firebase에 바로 로그인된 상태라 메인으로 이동 (자동 로그인은 꺼진 상태)
        Toast.makeText(this, getString(R.string.signup_done, nickname), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun showSignupError(error: AuthError) {
        when (error) {
            AuthError.EMAIL_DUPLICATE -> binding.tilEmail.error = getString(error.messageRes)
            AuthError.INVALID_CREDENTIALS -> binding.tilEmail.error = getString(R.string.error_email_invalid)
            AuthError.WEAK_PASSWORD -> binding.tilPassword.error = getString(error.messageRes)
            else -> MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error_dialog_title)
                .setMessage(error.messageRes)
                .setPositiveButton(R.string.common_confirm, null)
                .show()
        }
    }
}
