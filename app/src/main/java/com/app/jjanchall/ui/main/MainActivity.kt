package com.app.jjanchall.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.app.jjanchall.R
import com.app.jjanchall.data.auth.AuthRepository
import com.app.jjanchall.databinding.ActivityMainBinding
import com.app.jjanchall.ui.challenge.ChallengeFragment
import com.app.jjanchall.ui.home.HomeFragment
import com.app.jjanchall.ui.login.LoginActivity
import com.app.jjanchall.ui.record.RecordFragment
import com.app.jjanchall.ui.setting.SettingFragment
import com.app.jjanchall.util.applySystemBarsPadding

/**
 * 메인 화면: 하단 탭 + 탭별 Fragment
 * - 탭 4개 Fragment를 처음에 한 번만 만들고, 탭을 누르면 보여주기/숨기기만 해요.
 *   (다른 탭에 갔다 와도 입력하던 내용·스크롤 위치가 그대로 남아요)
 * - 홈이 아닌 탭에서 뒤로가기를 누르면 홈 탭으로 돌아가요.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** 하단 탭 메뉴 id → 보여줄 Fragment */
    private val tabs: Map<Int, () -> Fragment> = linkedMapOf(
        R.id.tab_home to ::HomeFragment,
        R.id.tab_record to ::RecordFragment,
        R.id.tab_challenge to ::ChallengeFragment,
        R.id.tab_setting to ::SettingFragment,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 로그인 정보가 없으면 로그인 화면으로
        if (!AuthRepository(this).isLoggedIn()) {
            goToLogin()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(includeIme = false)

        if (savedInstanceState == null) {
            addAllTabs()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
        // 지금 보고 있는 탭을 다시 누르면 아무것도 안 해요
        binding.bottomNav.setOnItemReselectedListener { }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNav.selectedItemId != R.id.tab_home) {
                    binding.bottomNav.selectedItemId = R.id.tab_home
                } else {
                    finish()
                }
            }
        })
    }

    /** 4개 탭 Fragment를 모두 붙이고 홈만 보이게 해요. */
    private fun addAllTabs() {
        val transaction = supportFragmentManager.beginTransaction()
        tabs.forEach { (tabId, create) ->
            val fragment = create()
            transaction.add(R.id.fragment_container, fragment, tagOf(tabId))
            if (tabId != R.id.tab_home) transaction.hide(fragment)
        }
        transaction.commit()
    }

    private fun showTab(tabId: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        tabs.keys.forEach { id ->
            val fragment = supportFragmentManager.findFragmentByTag(tagOf(id)) ?: return@forEach
            if (id == tabId) transaction.show(fragment) else transaction.hide(fragment)
        }
        transaction.commit()
    }

    private fun tagOf(tabId: Int) = "tab_$tabId"

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
