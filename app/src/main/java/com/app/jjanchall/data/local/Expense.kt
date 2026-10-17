package com.app.jjanchall.data.local

data class Expense(
    val id: Long = 0L,
    val category: String,
    val store: String,
    val memo: String,
    val amount: Int,
    val date: String,
    val time: String = "00:00"
)
