package com.app.jjanchall.data.local

/** 지출 카테고리. DB에는 이 이름 그대로 저장 */
object ExpenseCategory {
    const val DEFAULT = "기타"

    val names = listOf("식비", "교통", "카페", "쇼핑", "생활", DEFAULT)
}
