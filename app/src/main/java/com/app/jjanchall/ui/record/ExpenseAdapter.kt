package com.app.jjanchall.ui.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.jjanchall.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ExpenseItem: RecyclerView에 사용할 임시 지출 데이터입니다 추후 DB
// 담당 팀원의 데이터 모델로 변경 예정입니다
data class ExpenseItem(
    val category: String,
    val store: String,
    val memo: String,
    val amount: Int,
    val date: String
)

class ExpenseAdapter(
    private val expenses: List<ExpenseItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        // RecyclerView에서 날짜 헤더와 지출 항목을 구분하기 위한 값
        private const val TYPE_DATE = 0
        private const val TYPE_EXPENSE = 1
    }

    private sealed class ListItem {
        data class DateHeader(val date: String) : ListItem()
        data class ExpenseRow(val expense: ExpenseItem) : ListItem()
    }

    private val items: List<ListItem> = buildList {
        expenses
            .groupBy { it.date }
            .toSortedMap(compareByDescending { it })
            .forEach { (date, dateExpenses) ->

                // 날짜가 바뀔 때마다 날짜 헤더 추가
                add(ListItem.DateHeader(date))

                // 해당 날짜에 속한 지출 항목 추가
                dateExpenses.forEach { expense ->
                    add(ListItem.ExpenseRow(expense))
                }
            }
    }

    // 날짜 헤더 하나의 View를 관리하는 클래스
    class DateViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val date: TextView =
            itemView.findViewById(R.id.tvExpenseDate)
    }

    // 지출 항목 하나의 View를 관리하는 클래스
    class ExpenseViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val category: TextView =
            itemView.findViewById(R.id.tvExpenseCategory)

        val store: TextView =
            itemView.findViewById(R.id.tvExpenseStore)

        val memo: TextView =
            itemView.findViewById(R.id.tvExpenseMemo)

        val amount: TextView =
            itemView.findViewById(R.id.tvExpenseAmount)
    }

    // 현재 위치에 날짜 헤더와 지출 항목 중 무엇을 표시할지 결정
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.DateHeader -> TYPE_DATE
            is ListItem.ExpenseRow -> TYPE_EXPENSE
        }
    }

    // item_expense.xml과 item_expense_date.xml을 화면 항목으로 생성
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TYPE_DATE) {

            val view = inflater.inflate(
                R.layout.item_expense_date,
                parent,
                false
            )

            DateViewHolder(view)

        } else {

            val view = inflater.inflate(
                R.layout.item_expense,
                parent,
                false
            )

            ExpenseViewHolder(view)
        }
    }

    // 각 항목에 날짜 또는 지출 데이터를 연결
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = items[position]) {

            is ListItem.DateHeader -> {
                val dateHolder = holder as DateViewHolder

                // yyyy-MM-dd 날짜를 '10월 8일 (목)' 형태로 변환
                val parsedDate = LocalDate.parse(item.date)

                val formatter = DateTimeFormatter.ofPattern(
                    "M월 d일 (E)",
                    Locale.KOREAN
                )

                dateHolder.date.text =
                    parsedDate.format(formatter)
            }

            is ListItem.ExpenseRow -> {
                val expenseHolder = holder as ExpenseViewHolder
                val expense = item.expense

                expenseHolder.category.text = expense.category
                expenseHolder.store.text = expense.store
                expenseHolder.memo.text = expense.memo

                val formattedAmount =
                    NumberFormat.getNumberInstance(Locale.KOREA)
                        .format(expense.amount)

                expenseHolder.amount.text = "-${formattedAmount}원"
            }
        }
    }

    // 날짜 헤더와 지출 항목을 포함한 전체 항목 개수
    override fun getItemCount(): Int {
        return items.size
    }
}
