package com.app.jjanchall.ui.record

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.app.jjanchall.R
import com.app.jjanchall.data.local.Expense
import com.app.jjanchall.data.local.ExpenseCategory
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.ActivityExpenseEditBinding
import com.app.jjanchall.ui.receipt.ReceiptActivity
import com.app.jjanchall.util.addAmountFormat
import com.app.jjanchall.util.amountValue
import com.app.jjanchall.util.applySystemBarsPadding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * RECORD-02 지출 추가 · 수정
 * - EXTRA_EXPENSE_ID 없이 열면 추가
 * - EXTRA_EXPENSE_ID가 있으면 기존 지출 수정
 */
class ExpenseEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseEditBinding

    private var expenseId = NEW_EXPENSE
    private var selectedDate: LocalDate = LocalDate.now()

    // 수정할 때는 원래 시간 유지
    private var originalTime: String? = null
    private var expenseLoaded = false

    private val dateFormatter by lazy {
        DateTimeFormatter.ofPattern(
            getString(R.string.expense_date_format)
        )
    }

    // 영수증 OCR 화면에서 입력 결과 받아오기
    private val receiptLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {

            val data = result.data

            if (data != null) {

                // 금액 받아오기
                val amount = data.getStringExtra(
                    ReceiptActivity.EXTRA_AMOUNT
                )

                if (amount != null) {
                    binding.etAmount.setText(amount)
                }

                // 날짜 받아오기
                val date = data.getStringExtra(
                    ReceiptActivity.EXTRA_DATE
                )

                if (date != null) {
                    val parsedDate = runCatching {
                        LocalDate.parse(
                            date,
                            DateTimeFormatter.ofPattern(
                                "yyyy.MM.dd"
                            )
                        )
                    }.getOrNull()

                    if (parsedDate != null) {
                        selectedDate = parsedDate
                        showDate()
                    } else {
                        Toast.makeText(
                            this,
                            "영수증 날짜를 확인해 주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // 가게 이름 받아오기
                val storeName = data.getStringExtra(
                    ReceiptActivity.EXTRA_STORE_NAME
                )

                if (storeName != null) {
                    binding.etStore.setText(storeName)
                }

                Toast.makeText(
                    this,
                    "영수증 정보가 입력되었습니다. 확인 후 저장해 주세요.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityExpenseEditBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        expenseId = intent.getLongExtra(
            EXTRA_EXPENSE_ID,
            NEW_EXPENSE
        )

        // OCR 화면을 연 동안 재생성되어도 선택 날짜와 기존 지출 시각 유지
        selectedDate = savedInstanceState?.getString(STATE_SELECTED_DATE)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: LocalDate.now()
        originalTime = savedInstanceState?.getString(STATE_ORIGINAL_TIME)
        expenseLoaded = savedInstanceState?.getBoolean(STATE_EXPENSE_LOADED) ?: false

        binding.appBar.tvAppBarTitle.setText(
            if (isEditMode()) {
                R.string.expense_edit_title
            } else {
                R.string.expense_add_title
            }
        )

        binding.appBar.btnBack.setImageResource(
            R.drawable.ic_close
        )

        binding.appBar.btnBack.contentDescription =
            getString(R.string.common_close)

        binding.appBar.btnBack.setOnClickListener {
            finish()
        }

        setupCategorySpinner()
        setupAmountFormat()
        showDate()

        // 날짜 선택
        binding.tvDate.setOnClickListener {
            showDatePicker()
        }

        // 가게 이름 오류 메시지 초기화
        binding.etStore.doAfterTextChanged {
            binding.tilStore.error = null
        }

        // 영수증으로 자동 채우기
        binding.btnReceiptAutofill.setOnClickListener {
            val receiptIntent = Intent(
                this,
                ReceiptActivity::class.java
            )

            receiptLauncher.launch(receiptIntent)
        }

        // 지출 저장
        binding.btnSave.setOnClickListener {
            save()
        }

        // 기존 지출 수정 모드
        if (isEditMode() && !expenseLoaded) {
            loadExpense()
        }
    }

    private fun isEditMode(): Boolean {
        return expenseId != NEW_EXPENSE
    }

    // 카테고리 선택
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_category,
            ExpenseCategory.names
        )

        adapter.setDropDownViewResource(
            R.layout.item_spinner_category
        )

        binding.spCategory.adapter = adapter
    }

    // 금액에 자동으로 쉼표 추가
    // 예: 4800 -> 4,800
    private fun setupAmountFormat() {
        binding.etAmount.addAmountFormat { binding.tvAmountError.isVisible = false }
    }

    // 선택된 날짜 표시
    private fun showDate() {
        binding.tvDate.text =
            selectedDate.format(dateFormatter)
    }

    // 날짜 선택 달력
    private fun showDatePicker() {

        DatePickerDialog(
            this,
            { _, year, month, day ->

                selectedDate = LocalDate.of(
                    year,
                    month + 1,
                    day
                )

                showDate()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    // 기존 지출 불러오기
    private fun loadExpense() {

        Thread {

            val expense = ExpenseDbHelper(
                applicationContext
            ).use {
                it.getExpenseById(expenseId)
            }

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (expense == null) {
                    Toast.makeText(
                        this,
                        R.string.expense_not_found,
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@runOnUiThread
                }

                fillForm(expense)
            }

        }.start()
    }

    // 기존 지출 정보를 화면에 표시
    private fun fillForm(expense: Expense) {

        binding.etAmount.setText(
            expense.amount.toString()
        )

        binding.etStore.setText(expense.store)
        binding.etMemo.setText(expense.memo)

        val categoryIndex = ExpenseCategory.names
            .indexOf(expense.category)
            .takeIf { it >= 0 }
            ?: ExpenseCategory.names.indexOf(
                ExpenseCategory.DEFAULT
            )

        binding.spCategory.setSelection(categoryIndex)

        selectedDate = runCatching {
            LocalDate.parse(expense.date)
        }.getOrDefault(LocalDate.now())

        originalTime = expense.time
        expenseLoaded = true

        showDate()
    }

    // 지출 저장
    private fun save() {
        val amount = binding.etAmount.amountValue()
        val store = binding.etStore.text?.toString().orEmpty().trim()

        var valid = true

        if (amount <= 0) {
            binding.tvAmountError.isVisible = true
            valid = false
        }

        if (store.isEmpty()) {
            binding.tilStore.error =
                getString(R.string.expense_error_store)

            valid = false
        }

        if (!valid) {
            return
        }

        val expense = Expense(
            id = if (isEditMode()) expenseId else 0L,
            category = binding.spCategory.selectedItem as String,
            store = store,
            memo = binding.etMemo.text
                ?.toString()
                .orEmpty()
                .trim(),
            amount = amount,
            date = selectedDate.toString(),
            time = originalTime
                ?: LocalTime.now().format(TIME_FORMATTER)
        )

        // 저장 중 중복 클릭 방지
        binding.btnSave.isEnabled = false

        Thread {

            val success = runCatching {

                ExpenseDbHelper(applicationContext).use { db ->

                    if (isEditMode()) {
                        db.updateExpense(expense) > 0
                    } else {
                        db.insertExpense(expense) != -1L
                    }
                }

            }.getOrDefault(false)

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                if (success) {

                    Toast.makeText(
                        this,
                        R.string.expense_saved,
                        Toast.LENGTH_SHORT
                    ).show()

                    setResult(RESULT_OK)
                    finish()

                } else {

                    binding.btnSave.isEnabled = true

                    Toast.makeText(
                        this,
                        R.string.expense_save_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_DATE, selectedDate.toString())
        outState.putString(STATE_ORIGINAL_TIME, originalTime)
        outState.putBoolean(STATE_EXPENSE_LOADED, expenseLoaded)
        super.onSaveInstanceState(outState)
    }

    companion object {

        private const val STATE_SELECTED_DATE = "selected_date"
        private const val STATE_ORIGINAL_TIME = "original_time"
        private const val STATE_EXPENSE_LOADED = "expense_loaded"

        private const val EXTRA_EXPENSE_ID = "expense_id"
        private const val NEW_EXPENSE = -1L

        private val TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm")

        // 지출 추가 화면
        fun newIntent(context: Context) =
            Intent(context, ExpenseEditActivity::class.java)

        // 지출 수정 화면
        fun editIntent(
            context: Context,
            expenseId: Long
        ) = newIntent(context).putExtra(
            EXTRA_EXPENSE_ID,
            expenseId
        )
    }
}
