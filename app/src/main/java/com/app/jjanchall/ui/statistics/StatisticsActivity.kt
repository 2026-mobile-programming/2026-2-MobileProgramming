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

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding

    private var selectedMonth = YearMonth.now()
    private var requestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()

        binding.appBar.tvAppBarTitle.text = "통계"
        binding.appBar.btnBack.setOnClickListener {
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
                    loadStatistics()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) = Unit
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
            includeFontPadding = false

            setTextAppearance(
                R.style.TextAppearance_JjanChall_Body2
            )
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
                    if (isSelected) R.color.text_on_brand
                    else R.color.text_primary
                )
            )

            tabView.background = createTabBackground(
                isSelected,
                index
            )
        }
    }

    private fun createTabBackground(
        isSelected: Boolean,
        index: Int
    ): GradientDrawable {
        val radius = resources.getDimension(R.dimen.radius_md)
        val strokeWidth = resources.getDimensionPixelSize(
            R.dimen.statistics_divider_height
        )

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
                strokeWidth,
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

    private fun loadStatistics() {
        val currentRequest = ++requestId
        val month = selectedMonth
        val appContext = applicationContext

        Thread {
            try {
                val sixMonths = (5 downTo 0).map {
                    month.minusMonths(it.toLong())
                }

                val (categoryAmounts, monthlyItems) =
                    ExpenseDbHelper(appContext).use { db ->

                        val categories = db.getExpensesForMonth(
                            month.toString()
                        )
                            .groupBy { it.category }
                            .map { (category, expenses) ->
                                category to expenses.sumOf { it.amount }
                            }
                            .filter { it.second > 0 }
                            .sortedByDescending { it.second }

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

                val chartData = categoryAmounts.map { (category, amount) ->
                    ExpenseDonutChartView.ChartItem(
                        amount = amount,
                        color = ContextCompat.getColor(
                            appContext,
                            getCategoryColor(category)
                        )
                    )
                }

                val monthlyTotal = monthlyItems.sumOf { it.amount }

                runOnUiThread {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        currentRequest != requestId
                    ) {
                        return@runOnUiThread
                    }

                    binding.donutChart.setChartData(chartData)
                    showCategoryStatistics(categoryAmounts)

                    binding.monthlyBarChart.setChartData(monthlyItems)

                    binding.tvMonthlyTotal.text =
                        "${formatAmount(monthlyTotal)}원"

                    showMonthlyExpenses(sixMonths, monthlyItems)
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

                val padding = resources.getDimensionPixelSize(
                    R.dimen.spacing_2xl
                )

                setPadding(0, padding, 0, padding)
            }

            container.addView(emptyText)
            return
        }

        categoryAmounts.forEachIndexed { index, (category, amount) ->

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val padding = resources.getDimensionPixelSize(
                    R.dimen.spacing_lg
                )

                setPadding(0, padding, 0, padding)
            }

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
                    marginEnd = dotSize
                }
            )

            val nameText = TextView(this).apply {
                text = category
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END

                setTextAppearance(
                    R.style.TextAppearance_JjanChall_Body2
                )

                setTextColor(
                    ContextCompat.getColor(
                        this@StatisticsActivity,
                        R.color.text_primary
                    )
                )
            }

            row.addView(
                nameText,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

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

            val amountText = TextView(this).apply {
                text = "${formatAmount(amount.toLong())}원"
                gravity = Gravity.END

                setTextAppearance(
                    R.style.TextAppearance_JjanChall_BodyStrong
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

            if (index < categoryAmounts.lastIndex) {
                addStatisticsDivider(container)
            }
        }
    }

    private fun showMonthlyExpenses(
        months: List<YearMonth>,
        monthlyItems: List<MonthlyBarChartView.MonthlyItem>
    ) {
        val container = binding.layoutMonthlyExpenses
        container.removeAllViews()

        val monthlyData = months.zip(monthlyItems).asReversed()

        monthlyData.forEachIndexed { index, (yearMonth, item) ->

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val padding = resources.getDimensionPixelSize(
                    R.dimen.spacing_lg
                )

                setPadding(0, padding, 0, padding)
            }

            val monthText = TextView(this).apply {
                text = "${yearMonth.year}년 ${yearMonth.monthValue}월"

                setTextAppearance(
                    R.style.TextAppearance_JjanChall_Body2
                )

                setTextColor(
                    ContextCompat.getColor(
                        this@StatisticsActivity,
                        if (yearMonth == selectedMonth) {
                            R.color.text_brand
                        } else {
                            R.color.text_primary
                        }
                    )
                )
            }

            row.addView(
                monthText,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val amountText = TextView(this).apply {
                text = "${formatAmount(item.amount)}원"
                gravity = Gravity.END

                setTextAppearance(
                    R.style.TextAppearance_JjanChall_BodyStrong
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

            if (index < monthlyData.lastIndex) {
                addStatisticsDivider(container)
            }
        }
    }

    private fun addStatisticsDivider(container: LinearLayout) {
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
                resources.getDimensionPixelSize(
                    R.dimen.statistics_divider_height
                )
            )
        )
    }

    private fun formatAmount(amount: Long): String {
        return NumberFormat.getNumberInstance(
            Locale.KOREA
        ).format(amount)
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
