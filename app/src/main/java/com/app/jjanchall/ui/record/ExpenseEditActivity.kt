package com.app.jjanchall.ui.record

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.app.jjanchall.R
import com.app.jjanchall.data.local.Expense
import com.app.jjanchall.data.local.ExpenseCategory
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.ActivityExpenseEditBinding
import com.app.jjanchall.util.applySystemBarsPadding
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * RECORD-02 지출 추가 · 수정
 * - EXTRA_EXPENSE_ID 없이 열면 추가, 있으면 그 지출을 불러와서 수정
 */
class ExpenseEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseEditBinding

    private var expenseId = NEW_EXPENSE
    private var selectedDate: LocalDate = LocalDate.now()

    // 수정할 때는 원래 시간 유지 (같은 날짜 안 정렬 순서가 바뀌지 않게)
    private var originalTime: String? = null

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    private val dateFormatter by lazy {
        DateTimeFormatter.ofPattern(getString(R.string.expense_date_format))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityExpenseEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, NEW_EXPENSE)

        binding.appBar.tvAppBarTitle.setText(
            if (isEditMode()) R.string.expense_edit_title else R.string.expense_add_title
        )
        binding.appBar.btnBack.setImageResource(R.drawable.ic_close)
        binding.appBar.btnBack.contentDescription = getString(R.string.common_close)
        binding.appBar.btnBack.setOnClickListener { finish() }

        setupCategorySpinner()
        setupAmountFormat()
        showDate()

        binding.tvDate.setOnClickListener { showDatePicker() }
        binding.etStore.doAfterTextChanged { binding.tilStore.error = null }
        binding.btnReceiptAutofill.setOnClickListener {
            Toast.makeText(this, R.string.setting_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.btnSave.setOnClickListener { save() }

        if (isEditMode()) loadExpense()
    }

    private fun isEditMode() = expenseId != NEW_EXPENSE

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, R.layout.item_spinner_category, ExpenseCategory.names)
        adapter.setDropDownViewResource(R.layout.item_spinner_category)
        binding.spCategory.adapter = adapter
    }

    /** 금액을 입력하면 4800 → 4,800 처럼 쉼표 넣기 */
    private fun setupAmountFormat() {
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                binding.tvAmountError.isVisible = false
                // 9자리(9억 미만)까지만 받기
                val digits = s?.filter { it.isDigit() }?.toString().orEmpty().take(MAX_AMOUNT_DIGITS)
                val formatted = if (digits.isEmpty()) "" else numberFormat.format(digits.toLong())
                if (formatted != s.toString()) {
                    binding.etAmount.removeTextChangedListener(this)
                    binding.etAmount.setText(formatted)
                    binding.etAmount.setSelection(formatted.length)
                    binding.etAmount.addTextChangedListener(this)
                }
            }
        })
    }

    private fun showDate() {
        binding.tvDate.text = selectedDate.format(dateFormatter)
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                showDate()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth,
        ).show()
    }

    private fun loadExpense() {
        Thread {
            val expense = ExpenseDbHelper(applicationContext).use { it.getExpenseById(expenseId) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (expense == null) {
                    Toast.makeText(this, R.string.expense_not_found, Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }
                fillForm(expense)
            }
        }.start()
    }

    private fun fillForm(expense: Expense) {
        binding.etAmount.setText(expense.amount.toString())
        binding.etStore.setText(expense.store)
        binding.etMemo.setText(expense.memo)

        val categoryIndex = ExpenseCategory.names.indexOf(expense.category)
            .takeIf { it >= 0 } ?: ExpenseCategory.names.indexOf(ExpenseCategory.DEFAULT)
        binding.spCategory.setSelection(categoryIndex)

        selectedDate = runCatching { LocalDate.parse(expense.date) }.getOrDefault(LocalDate.now())
        originalTime = expense.time
        showDate()
    }

    private fun save() {
        val amount = binding.etAmount.text?.filter { it.isDigit() }?.toString()?.toIntOrNull() ?: 0
        val store = binding.etStore.text?.toString().orEmpty().trim()

        var valid = true
        if (amount <= 0) {
            binding.tvAmountError.isVisible = true
            valid = false
        }
        if (store.isEmpty()) {
            binding.tilStore.error = getString(R.string.expense_error_store)
            valid = false
        }
        if (!valid) return

        val expense = Expense(
            id = if (isEditMode()) expenseId else 0L,
            category = binding.spCategory.selectedItem as String,
            store = store,
            memo = binding.etMemo.text?.toString().orEmpty().trim(),
            amount = amount,
            date = selectedDate.toString(), // yyyy-MM-dd
            time = originalTime ?: LocalTime.now().format(TIME_FORMATTER),
        )

        // 저장하는 동안 버튼을 막아 두 번 저장되지 않게
        binding.btnSave.isEnabled = false
        Thread {
            val success = runCatching {
                ExpenseDbHelper(applicationContext).use { db ->
                    if (isEditMode()) db.updateExpense(expense) > 0 else db.insertExpense(expense) != -1L
                }
            }.getOrDefault(false)

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (success) {
                    Toast.makeText(this, R.string.expense_saved, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    binding.btnSave.isEnabled = true
                    Toast.makeText(this, R.string.expense_save_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    companion object {
        private const val EXTRA_EXPENSE_ID = "expense_id"
        private const val NEW_EXPENSE = -1L
        private const val MAX_AMOUNT_DIGITS = 9
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

        /** 지출 추가 화면 */
        fun newIntent(context: Context) = Intent(context, ExpenseEditActivity::class.java)

        /** 지출 수정 화면 */
        fun editIntent(context: Context, expenseId: Long) =
            newIntent(context).putExtra(EXTRA_EXPENSE_ID, expenseId)
    }
}
