package com.app.jjanchall.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.jjanchall.R
import com.app.jjanchall.data.auth.AuthRepository
import com.app.jjanchall.data.local.SettingPreferences
import com.app.jjanchall.databinding.FragmentSettingBinding
import com.app.jjanchall.ui.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** SETTING-01 설정 탭 */
class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var authRepository: AuthRepository
    private lateinit var settingPrefs: SettingPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authRepository = AuthRepository(requireContext())
        settingPrefs = SettingPreferences(requireContext())

        binding.header.tvHeaderTitle.setText(R.string.setting_title)
        bindProfile()
        bindSwitches()

        // 선택 기능: 담당이 정해지면 화면 연결
        listOf(binding.rowMonthlyBudget, binding.rowAiReport, binding.rowCategory).forEach { row ->
            row.setOnClickListener {
                Toast.makeText(requireContext(), R.string.setting_coming_soon, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }
    }

    private fun bindProfile() {
        val nickname = authRepository.currentNickname.orEmpty()
        binding.tvNickname.text = nickname
        binding.tvEmail.text = authRepository.currentEmail.orEmpty()
        binding.tvAvatar.text = nickname.take(1)
    }

    private fun bindSwitches() {
        // 저장된 값을 먼저 넣고 나서 리스너를 달아야 화면을 열 때 저장이 다시 일어나지 않아요
        binding.switchBudgetAlert.isChecked = settingPrefs.budgetAlert
        binding.switchDailyReminder.isChecked = settingPrefs.dailyReminder
        binding.switchAutoLogin.isChecked = authRepository.autoLogin

        binding.switchBudgetAlert.setOnCheckedChangeListener { _, checked -> settingPrefs.budgetAlert = checked }
        binding.switchDailyReminder.setOnCheckedChangeListener { _, checked -> settingPrefs.dailyReminder = checked }
        binding.switchAutoLogin.setOnCheckedChangeListener { _, checked -> authRepository.autoLogin = checked }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.setting_logout_title)
            .setMessage(R.string.setting_logout_message)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.setting_logout) { _, _ -> logout() }
            .show()
    }

    private fun logout() {
        authRepository.logout()
        val intent = Intent(requireContext(), LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
