package com.app.jjanchall.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.app.jjanchall.R
import com.app.jjanchall.data.local.BudgetPreferences
import com.app.jjanchall.data.local.BudgetPreferences.AlertType
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.ActivityBudgetBinding
import com.app.jjanchall.util.addAmountFormat
import com.app.jjanchall.util.amountValue
import com.app.jjanchall.util.applySystemBarsPadding
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

/** SETTING-02 월 예산 설정 */
class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var budgetPrefs: BudgetPreferences

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    // 이번 달 사용 금액 (SQLite에서 불러온 뒤 채워짐)
    private var monthlySpent = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()
        budgetPrefs = BudgetPreferences(this)

        binding.appBar.tvAppBarTitle.setText(R.string.budget_title)
        binding.appBar.btnBack.setOnClickListener { finish() }

        // 예산을 입력하는 대로 사용 현황 다시 계산
        binding.etBudget.addAmountFormat { budget ->
            binding.tvBudgetError.isVisible = false
            showUsage(budget)
        }

        val savedBudget = budgetPrefs.monthlyBudget
        if (savedBudget > 0) binding.etBudget.setText(savedBudget.toString())
        binding.rgAlert.check(radioIdOf(budgetPrefs.alertType))

        binding.btnSave.setOnClickListener { save() }

        loadMonthlySpent()
    }

    private fun loadMonthlySpent() {
        Thread {
            val spent = runCatching {
                ExpenseDbHelper(applicationContext).use { db ->
                    db.getExpensesForMonth(YearMonth.now().toString()).sumOf { it.amount }
                }
            }.getOrDefault(0)

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                monthlySpent = spent
                showUsage(binding.etBudget.amountValue())
            }
        }.start()
    }

    /** "312,400원 / 300,000원" + 진행 막대 + 상태 문구 */
    private fun showUsage(budget: Int) {
        binding.tvBudgetUsed.text = getString(
            R.string.budget_used_format,
            numberFormat.format(monthlySpent),
            numberFormat.format(budget),
        )

        if (budget <= 0) {
            binding.progressBudget.progress = 0
            binding.tvBudgetStatus.setText(R.string.budget_status_empty)
            setStatusColor(R.color.text_secondary, R.color.bg_brand)
            return
        }

        val percent = (monthlySpent.toLong() * 100 / budget).toInt()
        binding.progressBudget.progress = percent.coerceAtMost(100)

        when {
            monthlySpent > budget -> {
                binding.tvBudgetStatus.text =
                    getString(R.string.budget_status_over, numberFormat.format(monthlySpent - budget))
                setStatusColor(R.color.text_danger, R.color.bg_danger)
            }
            percent >= WARNING_PERCENT -> {
                binding.tvBudgetStatus.text = getString(R.string.budget_status_warning, percent)
                setStatusColor(R.color.text_danger, R.color.bg_brand)
            }
            else -> {
                binding.tvBudgetStatus.text =
                    getString(R.string.budget_status_left, numberFormat.format(budget - monthlySpent))
                setStatusColor(R.color.text_secondary, R.color.bg_brand)
            }
        }
    }

    private fun setStatusColor(textColorRes: Int, barColorRes: Int) {
        binding.tvBudgetStatus.setTextColor(ContextCompat.getColor(this, textColorRes))
        binding.progressBudget.setIndicatorColor(ContextCompat.getColor(this, barColorRes))
    }

    private fun save() {
        val budget = binding.etBudget.amountValue()
        if (budget <= 0) {
            binding.tvBudgetError.isVisible = true
            return
        }

        budgetPrefs.monthlyBudget = budget
        budgetPrefs.alertType = alertTypeOf(binding.rgAlert.checkedRadioButtonId)
        Toast.makeText(this, R.string.budget_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun radioIdOf(type: AlertType) = when (type) {
        AlertType.PERCENT_80 -> R.id.rb_alert_80
        AlertType.OVER -> R.id.rb_alert_over
        AlertType.NONE -> R.id.rb_alert_none
    }

    private fun alertTypeOf(radioId: Int) = when (radioId) {
        R.id.rb_alert_80 -> AlertType.PERCENT_80
        R.id.rb_alert_none -> AlertType.NONE
        else -> AlertType.OVER
    }

    companion object {
        private const val WARNING_PERCENT = 80

        fun newIntent(context: Context) = Intent(context, BudgetActivity::class.java)
    }
}
