package com.app.jjanchall.ui.statistics

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TabHost
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.jjanchall.R
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

                    // 다음 단계에서 SQLite 통계 조회 연결
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

        // 카테고리별 탭
        val categoryTab = tabHost.newTabSpec("category")
            .setIndicator(
                createTabIndicator(
                    "카테고리별",
                    R.dimen.statistics_tab_category_width
                )
            )
            .setContent(binding.tabCategory.id)

        // 월별 탭
        val monthlyTab = tabHost.newTabSpec("monthly")
            .setIndicator(
                createTabIndicator(
                    "월별",
                    R.dimen.statistics_tab_monthly_width
                )
            )
            .setContent(binding.tabMonthly.id)

        tabHost.addTab(categoryTab)
        tabHost.addTab(monthlyTab)

        // 탭을 누를 때마다 선택 색상 변경
        tabHost.setOnTabChangedListener {
            updateTabAppearance(tabHost)
        }

        // 처음에는 카테고리별 탭 선택
        tabHost.currentTab = 0
        updateTabAppearance(tabHost)
    }

    // Figma 크기에 맞는 탭 글자 생성
    private fun createTabIndicator(
        title: String,
        widthRes: Int
    ): TextView {
        return TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 14f

            layoutParams = ViewGroup.LayoutParams(
                resources.getDimensionPixelSize(widthRes),
                resources.getDimensionPixelSize(
                    R.dimen.statistics_tab_height
                )
            )
        }
    }

    // 선택된 탭은 초록색, 나머지는 흰색
    private fun updateTabAppearance(tabHost: TabHost) {
        val selectedIndex = tabHost.currentTab
        val tabWidget = tabHost.tabWidget

        for (index in 0 until tabWidget.tabCount) {
            val tabView = tabWidget.getChildAt(index) as? TextView
                ?: continue

            val isSelected = index == selectedIndex

            tabView.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) R.color.bg_default
                    else R.color.text_primary
                )
            )

            tabView.background = createTabBackground(
                isSelected,
                index
            )
        }
    }

    // Figma처럼 양쪽 끝만 둥근 탭 배경
    private fun createTabBackground(
        isSelected: Boolean,
        index: Int
    ): GradientDrawable {

        val radius = resources.getDimension(R.dimen.radius_md)

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE

            setColor(
                ContextCompat.getColor(
                    this@StatisticsActivity,
                    if (isSelected) R.color.bg_brand
                    else R.color.bg_default
                )
            )

            setStroke(
                resources.displayMetrics.density.toInt().coerceAtLeast(1),
                ContextCompat.getColor(
                    this@StatisticsActivity,
                    R.color.bg_brand
                )
            )

            // 첫 번째 탭은 왼쪽, 두 번째 탭은 오른쪽만 둥글게
            cornerRadii = if (index == 0) {
                floatArrayOf(
                    radius, radius,
                    0f, 0f,
                    0f, 0f,
                    radius, radius
                )
            } else {
                floatArrayOf(
                    0f, 0f,
                    radius, radius,
                    radius, radius,
                    0f, 0f
                )
            }
        }
    }
}
