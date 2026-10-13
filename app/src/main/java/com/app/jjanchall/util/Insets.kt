package com.app.jjanchall.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 화면이 상태바·내비게이션바·키보드 밑으로 들어가지 않도록 여백을 넣어요.
 * (enableEdgeToEdge()를 쓴 화면의 루트 뷰에서 호출)
 *
 * @param includeIme 키보드가 올라올 때 그만큼 화면을 밀어 올릴지.
 *   하단 탭이 있는 화면은 탭이 키보드 위로 따라 올라오지 않게 false로 써요.
 */
fun View.applySystemBarsPadding(includeIme: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = if (includeIme) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime))
        insets
    }
}
