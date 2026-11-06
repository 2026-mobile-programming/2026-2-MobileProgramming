package com.app.jjanchall.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

// 9자리(9억 미만)까지만 받기
private const val MAX_AMOUNT_DIGITS = 9

/** 금액 입력칸: 4800 → 4,800 처럼 쉼표를 넣고, 바뀔 때마다 숫자 값을 알려줌 */
fun EditText.addAmountFormat(onAmountChanged: (Int) -> Unit = {}) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            val digits = s?.filter { it.isDigit() }?.toString().orEmpty().take(MAX_AMOUNT_DIGITS)
            val formatted = if (digits.isEmpty()) "" else numberFormat.format(digits.toLong())
            if (formatted != s.toString()) {
                removeTextChangedListener(this)
                setText(formatted)
                setSelection(formatted.length)
                addTextChangedListener(this)
            }
            onAmountChanged(digits.toIntOrNull() ?: 0)
        }
    })
}

/** 쉼표를 뺀 금액 값 (비어 있으면 0) */
fun EditText.amountValue(): Int =
    text?.filter { it.isDigit() }?.toString()?.toIntOrNull() ?: 0
