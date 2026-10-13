package com.app.jjanchall.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 화면이 상태바·내비게이션바·키보드 밑으로 들어가지 않도록 여백을 넣어요.
 * (enableEdgeToEdge()를 쓴 화면의 루트 뷰에서 호출)
 *
 * @param includeBottom 아래쪽(내비게이션 바·키보드) 여백도 넣을지.
 *   하단 탭이 있는 화면은 하단 탭이 직접 내비게이션 바 영역을 채우므로 false로 써요.
 */
fun View.applySystemBarsPadding(includeBottom: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val bottom = if (includeBottom) maxOf(bars.bottom, ime.bottom) else 0
        v.setPadding(bars.left, bars.top, bars.right, bottom)
        insets
    }
}
