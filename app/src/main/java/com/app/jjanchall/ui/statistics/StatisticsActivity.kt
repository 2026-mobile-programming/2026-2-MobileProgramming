package com.app.jjanchall.ui.statistics

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TabHost
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.jjanchall.R
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.ActivityStatisticsBinding
import java.time.YearMonth

/** HOME-02 통계 화면 */
class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding

    private var selectedMonth = YearMonth.now()
    private var requestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()

        // 뒤로 가기 → 홈 화면 복귀
        binding.btnBack.setOnClickListener {
            finish()
        }

        setupMonthSpinner()
        setupTabHost()
    }

    // 시스템 상태바 및 내비게이션 바와 겹치지 않도록 처리
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

    // 최근 12개월 선택 Spinner
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

                    // 선택한 월의 실제 SQLite 통계 갱신
                    loadCategoryStatistics()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

    // Android TabHost 구성
    private fun setupTabHost() {
        val tabHost = findViewById<TabHost>(android.R.id.tabhost)

        tabHost.setup()

        val categoryTab = tabHost.newTabSpec("category")
            .setIndicator(
                createTabIndicator("카테고리별")
            )
            .setContent(binding.tabCategory.id)

        val monthlyTab = tabHost.newTabSpec("monthly")
            .setIndicator(
                createTabIndicator("월별")
            )
            .setContent(binding.tabMonthly.id)

        tabHost.addTab(categoryTab)
        tabHost.addTab(monthlyTab)

        // Android 기본 탭 너비가 늘어나지 않도록 고정
        val tabWidget = tabHost.tabWidget

        val tabHeight = resources.getDimensionPixelSize(
            R.dimen.statistics_tab_height
        )

        val categoryWidth = resources.getDimensionPixelSize(
            R.dimen.statistics_tab_category_width
        )

        val monthlyWidth = resources.getDimensionPixelSize(
            R.dimen.statistics_tab_monthly_width
        )

        tabWidget.getChildAt(0).layoutParams =
            LinearLayout.LayoutParams(
                categoryWidth,
                tabHeight
            )

        tabWidget.getChildAt(1).layoutParams =
            LinearLayout.LayoutParams(
                monthlyWidth,
                tabHeight
            )

        // 탭 선택에 따라 디자인 변경
        tabHost.setOnTabChangedListener {
            updateTabAppearance(tabHost)
        }

        tabHost.currentTab = 0
        updateTabAppearance(tabHost)
    }

    // TabHost에 표시할 탭 제목 생성
    private fun createTabIndicator(title: String): TextView {
        return TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 14f
            includeFontPadding = false
        }
    }

    // 선택 탭의 색상 변경
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

    // Figma처럼 좌우 끝이 둥근 탭 배경
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
                (resources.displayMetrics.density + 0.5f).toInt(),
                ContextCompat.getColor(
                    this@StatisticsActivity,
                    R.color.bg_brand
                )
            )

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

    // SQLite에서 선택한 월의 카테고리별 지출 조회
    private fun loadCategoryStatistics() {
        val currentRequest = ++requestId
        val month = selectedMonth.toString()
        val appContext = applicationContext

        Thread {
            try {
                val categoryAmounts = ExpenseDbHelper(appContext).use { db ->
                    db.getExpensesForMonth(month)
                        .groupBy { it.category }
                        .map { (category, expenses) ->
                            category to expenses.sumOf { it.amount }
                        }
                        .filter { it.second > 0 }
                        .sortedByDescending { it.second }
                }

                val chartData = categoryAmounts.map { (category, amount) ->
                    ExpenseDonutChartView.ChartItem(
                        amount = amount,
                        color = ContextCompat.getColor(
                            appContext,
                            getCategoryColor(category)
                        )
                    )
                }

                runOnUiThread {
                    // 이전 조회 결과가 늦게 도착하면 무시
                    if (isFinishing || isDestroyed ||
                        currentRequest != requestId
                    ) {
                        return@runOnUiThread
                    }

                    // Canvas 도넛 차트 갱신
                    binding.donutChart.setChartData(chartData)
                }

            } catch (e: Exception) {
                Log.e(
                    "StatisticsActivity",
                    "카테고리별 통계 조회 실패",
                    e
                )
            }
        }.start()
    }

    // 기존 짠챌 카테고리 색상 사용
    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "식비" -> R.color.chart_food
            "카페", "카페·간식", "간식" -> R.color.chart_cafe
            "교통" -> R.color.chart_transport
            "쇼핑" -> R.color.chart_shopping
            "생활" -> R.color.chart_living
            else -> R.color.chart_etc
        }
    }

    override fun onDestroy() {
        requestId++
        super.onDestroy()
    }
}
