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

                    // 월 선택 시 카테고리별 + 월별 통계 함께 갱신
                    loadStatistics()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

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

    private fun createTabIndicator(title: String): TextView {
        return TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 14f
            includeFontPadding = false
        }
    }

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

    // SQLite에서 카테고리별 통계와 최근 6개월 지출 조회
    private fun loadStatistics() {
        val currentRequest = ++requestId
        val month = selectedMonth
        val appContext = applicationContext

        Thread {
            try {
                // 선택한 월을 포함한 6개월 목록 (오래된 월부터)
                val sixMonths = (5 downTo 0).map {
                    month.minusMonths(it.toLong())
                }

                val (categoryAmounts, monthlyItems) =
                    ExpenseDbHelper(appContext).use { db ->

                        // 선택한 월의 카테고리별 합계
                        val categories = db.getExpensesForMonth(
                            month.toString()
                        )
                            .groupBy { it.category }
                            .map { (category, expenses) ->
                                category to expenses.sumOf { it.amount }
                            }
                            .filter { it.second > 0 }
                            .sortedByDescending { it.second }

                        // 6개월의 월별 합계
                        val monthly = sixMonths.map { yearMonth ->
                            val total = db.getExpensesForMonth(
                                yearMonth.toString()
                            ).sumOf { it.amount.toLong() }

                            MonthlyBarChartView.MonthlyItem(
                                month = "${yearMonth.monthValue}월",
                                amount = total
                            )
                        }

                        categories to monthly
                    }

                // 도넛 차트 데이터
                val chartData = categoryAmounts.map { (category, amount) ->
                    ExpenseDonutChartView.ChartItem(
                        amount = amount,
                        color = ContextCompat.getColor(
                            appContext,
                            getCategoryColor(category)
                        )
                    )
                }

                // 6개월 전체 지출
                val monthlyTotal = monthlyItems.sumOf { it.amount }

                runOnUiThread {
                    // 이전 조회 결과가 늦게 도착하면 무시
                    if (
                        isFinishing ||
                        isDestroyed ||
                        currentRequest != requestId
                    ) {
                        return@runOnUiThread
                    }

                    // 카테고리별 갱신
                    binding.donutChart.setChartData(chartData)
                    showCategoryStatistics(categoryAmounts)

                    // 월별 갱신
                    binding.monthlyBarChart.setChartData(monthlyItems)

                    // 6개월 총 지출 금액 표시
                    val numberFormat =
                        NumberFormat.getNumberInstance(Locale.KOREA)

                    binding.tvMonthlyTotal.text =
                        "${numberFormat.format(monthlyTotal)}원"
                }

            } catch (e: Exception) {
                Log.e(
                    "StatisticsActivity",
                    "통계 데이터 조회 실패",
                    e
                )
            }
        }.start()
    }

    // Canvas 아래 카테고리별 지출 목록
    private fun showCategoryStatistics(
        categoryAmounts: List<Pair<String, Int>>
    ) {
        val container = binding.layoutCategoryStats
        container.removeAllViews()

        val totalAmount = categoryAmounts.sumOf {
            it.second.toLong()
        }

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

            // 카테고리 목록 한 줄
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

            row.addView(
                categoryDot,
                LinearLayout.LayoutParams(
                    dotSize,
                    dotSize
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(
                        R.dimen.spacing_md
                    )
                }
            )

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

            // 카테고리별 지출 비율
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

            // 실제 지출 금액
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

            // 항목 사이 구분선
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
