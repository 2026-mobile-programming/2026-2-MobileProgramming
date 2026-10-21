
package com.app.jjanchall.ui.statistics

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TabHost
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.jjanchall.databinding.ActivityStatisticsBinding
import java.time.YearMonth

/** HOME-02 통계 화면 */
class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding

    private var selectedMonth = YearMonth.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태 표시줄 및 하단 시스템 바와 화면이 겹치지 않도록 처리
        setupSystemBars()

        // 뒤로 가기 → 홈 화면으로 복귀
        binding.btnBack.setOnClickListener {
            finish()
        }

        setupMonthSpinner()
        setupTabHost()
    }

    // 시스템 바 영역만큼 화면 여백 적용
    private fun setupSystemBars() {
        val initialLeft = binding.root.paddingLeft
        val initialTop = binding.root.paddingTop
        val initialRight = binding.root.paddingRight
        val initialBottom = binding.root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(binding.root)
    }

    // 최근 12개월 중 조회할 월 선택
    private fun setupMonthSpinner() {
        val months = (0 until 12).map {
            YearMonth.now().minusMonths(it.toLong())
        }

        val monthLabels = months.map {
            "${it.year}년 ${it.monthValue}월"
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            monthLabels
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerMonth.adapter = adapter

        binding.spinnerMonth.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedMonth = months[position]

                    // 추후 선택한 월의 SQLite 통계 조회
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

    // 실제 Android TabHost 구성
    private fun setupTabHost() {
        val tabHost = findViewById<TabHost>(android.R.id.tabhost)

        tabHost.setup()

        val categoryTab = tabHost.newTabSpec("category")
            .setIndicator("카테고리별")
            .setContent(binding.tabCategory.id)

        val monthlyTab = tabHost.newTabSpec("monthly")
            .setIndicator("월별")
            .setContent(binding.tabMonthly.id)

        tabHost.addTab(categoryTab)
        tabHost.addTab(monthlyTab)

        tabHost.currentTab = 0
    }
}
