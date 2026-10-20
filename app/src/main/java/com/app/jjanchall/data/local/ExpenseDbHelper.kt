package com.app.jjanchall.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ExpenseDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "jjanchall.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_EXPENSES = "expenses"
        private const val COL_ID = "_id"
        private const val COL_CATEGORY = "category"
        private const val COL_STORE = "store"
        private const val COL_MEMO = "memo"
        private const val COL_AMOUNT = "amount"
        private const val COL_DATE = "date"
        private const val COL_TIME = "time"
    }

    // 데이터베이스와 지출 테이블 생성
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_EXPENSES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_STORE TEXT NOT NULL,
                $COL_MEMO TEXT NOT NULL,
                $COL_AMOUNT INTEGER NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_TIME TEXT NOT NULL DEFAULT '00:00'
            )
            """.trimIndent()
        )
    }

    // 이후 테이블 구조가 바뀌면 여기에 데이터베이스 마이그레이션 추가
    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // 현재 데이터베이스 버전은 1
    }

    // 지출 추가
    fun insertExpense(expense: Expense): Long {
        val values = ContentValues().apply {
            put(COL_CATEGORY, expense.category)
            put(COL_STORE, expense.store)
            put(COL_MEMO, expense.memo)
            put(COL_AMOUNT, expense.amount)
            put(COL_DATE, expense.date)
            put(COL_TIME, expense.time)
        }

        return writableDatabase.insert(
            TABLE_EXPENSES,
            null,
            values
        )
    }

    // 기존 지출 수정
    fun updateExpense(expense: Expense): Int {
        val values = ContentValues().apply {
            put(COL_CATEGORY, expense.category)
            put(COL_STORE, expense.store)
            put(COL_MEMO, expense.memo)
            put(COL_AMOUNT, expense.amount)
            put(COL_DATE, expense.date)
            put(COL_TIME, expense.time)
        }

        return writableDatabase.update(
            TABLE_EXPENSES,
            values,
            "$COL_ID = ?",
            arrayOf(expense.id.toString())
        )
    }

    // 지출 삭제
    fun deleteExpense(id: Long): Int {
        return writableDatabase.delete(
            TABLE_EXPENSES,
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
    }

    // 모든 지출 조회
    fun getAllExpenses(): List<Expense> {
        return readExpenses(null, null)
    }

    // 특정 월의 지출 조회: 예) "2026-10"
    fun getExpensesForMonth(yearMonth: String): List<Expense> {
        return readExpenses(
            "$COL_DATE LIKE ?",
            arrayOf("$yearMonth%")
        )
    }

    // 지출 한 건 조회 (수정 화면용)
    fun getExpenseById(id: Long): Expense? {
        return readExpenses(
            "$COL_ID = ?",
            arrayOf(id.toString())
        ).firstOrNull()
    }

    // 데이터베이스에서 지출 목록 읽기
    private fun readExpenses(
        selection: String?,
        selectionArgs: Array<String>?
    ): List<Expense> {
        val result = mutableListOf<Expense>()

        readableDatabase.query(
            TABLE_EXPENSES,
            arrayOf(
                COL_ID,
                COL_CATEGORY,
                COL_STORE,
                COL_MEMO,
                COL_AMOUNT,
                COL_DATE,
                COL_TIME
            ),
            selection,
            selectionArgs,
            null,
            null,
            "$COL_DATE DESC, $COL_TIME DESC"
        ).use { cursor ->

            while (cursor.moveToNext()) {
                result.add(
                    Expense(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow(COL_ID)
                        ),
                        category = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_CATEGORY)
                        ),
                        store = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_STORE)
                        ),
                        memo = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_MEMO)
                        ),
                        amount = cursor.getInt(
                            cursor.getColumnIndexOrThrow(COL_AMOUNT)
                        ),
                        date = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_DATE)
                        ),
                        time = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_TIME)
                        )
                    )
                )
            }
        }

        return result
    }
}
