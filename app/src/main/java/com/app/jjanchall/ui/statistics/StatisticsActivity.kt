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
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

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

                    // 선택한 월의 차트와 목록을 함께 갱신
                    loadCategoryStatistics()
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
            .setIndicator(createTabIndicator("카테고리별"))
            .setContent(binding.tabCategory.id)

        val monthlyTab = tabHost.newTabSpec("monthly")
            .setIndicator(createTabIndicator("월별"))
            .setContent(binding.tabMonthly.id)

        tabHost.addTab(categoryTab)
        tabHost.addTab(monthlyTab)

        // Figma 기준 탭 너비 고정
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
            LinearLayout.LayoutParams(categoryWidth, tabHeight)

        tabWidget.getChildAt(1).layoutParams =
            LinearLayout.LayoutParams(monthlyWidth, tabHeight)

        tabHost.setOnTabChangedListener {
            updateTabAppearance(tabHost)
        }

        tabHost.currentTab = 0
        updateTabAppearance(tabHost)
    }

    // 탭 제목 생성
    private fun createTabIndicator(title: String): TextView {
        return TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 14f
            includeFontPadding = false
        }
    }

    // 선택된 탭의 색상 변경
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

            tabView.background = createTabBackground(isSelected, index)
        }
    }

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
                    // 이전 월의 조회 결과가 늦게 도착하면 무시
                    if (isFinishing || isDestroyed ||
                        currentRequest != requestId
                    ) {
                        return@runOnUiThread
                    }

                    // Canvas 도넛 차트 갱신
                    binding.donutChart.setChartData(chartData)

                    // 카테고리별 금액·비율 목록 갱신
                    showCategoryStatistics(categoryAmounts)
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

    // Canvas 아래에 카테고리별 지출 목록 표시
    private fun showCategoryStatistics(
        categoryAmounts: List<Pair<String, Int>>
    ) {
        val container = binding.layoutCategoryStats

        // 이전에 표시한 월의 목록 제거
        container.removeAllViews()

        val totalAmount = categoryAmounts.sumOf {
            it.second.toLong()
        }

        // 지출 내역이 없는 경우
        if (totalAmount == 0L) {
            val emptyText = TextView(this).apply {
                text = "선택한 월의 지출 내역이 없습니다."
                gravity = Gravity.CENTER
                setTextAppearance(
                    R.style.TextAppearance_JjanChall_Body2
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@StatisticsActivity,
                        R.color.text_secondary
                    )
                )
                setPadding(
                    0,
                    resources.getDimensionPixelSize(R.dimen.spacing_2xl),
                    0,
                    resources.getDimensionPixelSize(R.dimen.spacing_2xl)
                )
            }

            container.addView(emptyText)
            return
        }

        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

        categoryAmounts.forEachIndexed { index, (category, amount) ->

            // 목록 한 줄
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    resources.getDimensionPixelSize(R.dimen.spacing_lg),
                    0,
                    resources.getDimensionPixelSize(R.dimen.spacing_lg)
                )
            }

            // 카테고리 색상 점
            val categoryDot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(
                        ContextCompat.getColor(
                            this@StatisticsActivity,
                            getCategoryColor(category)
                        )
                    )
                }
            }

            val dotSize = resources.getDimensionPixelSize(
                R.dimen.spacing_md
            )

            val dotParams = LinearLayout.LayoutParams(
                dotSize,
                dotSize
            ).apply {
                marginEnd = resources.getDimensionPixelSize(
                    R.dimen.spacing_md
                )
            }

            row.addView(categoryDot, dotParams)

            // 카테고리 이름
            val nameText = TextView(this).apply {
                text = category
                setTextAppearance(
                    R.style.TextAppearance_JjanChall_Body2
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@StatisticsActivity,
                        R.color.text_primary
                    )
                )
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            row.addView(
                nameText,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            // 해당 카테고리의 지출 비율
            val percentage = amount.toDouble() / totalAmount * 100.0

            val percentageText = TextView(this).apply {
                text = String.format(
                    Locale.KOREA,
                    "%.0f%%",
                    percentage
                )
                gravity = Gravity.END
                setTextAppearance(
                    R.style.TextAppearance_JjanChall_Caption
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@StatisticsActivity,
                        R.color.text_secondary
                    )
                )
            }

            row.addView(
                percentageText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(
                        R.dimen.spacing_lg
                    )
                }
            )

            // 카테고리별 실제 지출 금액
            val amountText = TextView(this).apply {
                text = "${numberFormat.format(amount)}원"
                gravity = Gravity.END
                setTextAppearance(
                    R.style.TextAppearance_JjanChall_Label
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@StatisticsActivity,
                        R.color.text_primary
                    )
                )
            }

            row.addView(
                amountText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            container.addView(row)

            // 마지막 항목을 제외하고 구분선 표시
            if (index < categoryAmounts.lastIndex) {
                val divider = View(this).apply {
                    setBackgroundColor(
                        ContextCompat.getColor(
                            this@StatisticsActivity,
                            R.color.border_default
                        )
                    )
                }

                container.addView(
                    divider,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (resources.displayMetrics.density + 0.5f).toInt()
                    )
                )
            }
        }
    }

    // 짠챌 공통 카테고리 색상
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
