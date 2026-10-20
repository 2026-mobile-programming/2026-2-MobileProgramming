package com.app.jjanchall.ui.record

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.jjanchall.R
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.FragmentRecordBinding
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

/** RECORD-01 지출 기록 탭 */
class RecordFragment : Fragment() {

    // Fragment는 화면(View)이 먼저 사라질 수 있어서 binding을 onDestroyView에서 비워요
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    // 현재 선택된 연도와 월을 저장
    private var selectedMonth: YearMonth = YearMonth.now()

    // 현재 선택된 카테고리를 저장
    private var selectedCategory = "전체 카테고리"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView에 지출 내역을 세로 방향으로 표시
        binding.recyclerViewExpenses.layoutManager =
            LinearLayoutManager(requireContext())

        // 월 선택 버튼 클릭 시 메뉴 표시
        binding.tvMonthSelect.setOnClickListener { anchor ->
            showMonthMenu(anchor)
        }

        // 카테고리 선택 버튼 클릭 시 메뉴 표시
        binding.tvCategoryFilter.setOnClickListener { anchor ->
            showCategoryMenu(anchor)
        }
    }

    // 하단 탭은 Fragment를 숨겼다 보여주는 방식이라 onResume이 다시 불리지 않음 → 탭이 보일 때도 갱신
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) updateExpenseList()
    }

    override fun onResume() {
        super.onResume()

        // 화면으로 돌아올 때 SQLite 지출 내역 다시 조회
        updateExpenseList()
    }

    // 최근 12개월 중 조회할 월을 선택
    private fun showMonthMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)

        val months = (0 until 12).map {
            YearMonth.now().minusMonths(it.toLong())
        }

        // 최근 월부터 선택 메뉴 생성
        months.forEachIndexed { index, month ->
            val title = "${month.year}년 ${month.monthValue}월"
            popup.menu.add(0, index, index, title)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            selectedMonth = months[menuItem.itemId]

            // 선택 월 변경 후 목록과 합계 갱신
            updateExpenseList()
            true
        }

        popup.show()
    }

    // 기존 전체 카테고리 필터 기능 유지
    private fun showCategoryMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)

        val categories = listOf(
            "전체 카테고리",
            "식비",
            "교통",
            "카페",
            "쇼핑",
            "생활",
            "기타"
        )

        categories.forEach { category ->
            popup.menu.add(category)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            selectedCategory = menuItem.title.toString()

            // 선택 카테고리 변경 후 목록과 합계 갱신
            updateExpenseList()
            true
        }

        popup.show()
    }

    // 선택한 월과 카테고리를 모두 적용하여 화면 갱신
    private fun updateExpenseList() {

        val requestMonth = selectedMonth
        val appContext = context?.applicationContext ?: return

        // 현재 선택한 연도 및 월 표시
        binding.tvMonthSelect.text = getString(
            R.string.record_month_format,
            selectedMonth.year,
            selectedMonth.monthValue
        )

        // 현재 선택한 카테고리 표시
        binding.tvCategoryFilter.text = selectedCategory

        // SQLite 조회는 메인 스레드 외부에서 실행
        Thread {
            try {
                val monthlyExpenses = ExpenseDbHelper(appContext).use { db ->
                    db.getExpensesForMonth(requestMonth.toString())
                }

                // 기존 RecyclerView 데이터 모델로 변환
                val expenseItems = monthlyExpenses.map { expense ->
                    ExpenseItem(
                        category = expense.category,
                        store = expense.store,
                        memo = expense.memo,
                        amount = expense.amount,
                        date = expense.date
                    )
                }

                activity?.runOnUiThread {
                    val currentBinding = _binding ?: return@runOnUiThread

                    // 조회 도중 다른 월을 선택했다면 이전 결과는 무시
                    if (selectedMonth != requestMonth) {
                        return@runOnUiThread
                    }

                    // 월별 데이터에 카테고리 조건 추가
                    val filteredExpenses =
                        if (selectedCategory == "전체 카테고리") {
                            expenseItems
                        } else {
                            expenseItems.filter { expense ->
                                expense.category == selectedCategory
                            }
                        }

                    // ExpenseAdapter의 날짜별 그룹화 기능 재사용
                    currentBinding.recyclerViewExpenses.adapter =
                        ExpenseAdapter(filteredExpenses)

                    // 현재 선택한 조건의 총 지출 금액 계산
                    val total = filteredExpenses.sumOf { it.amount }

                    val formattedTotal =
                        NumberFormat.getNumberInstance(Locale.KOREA)
                            .format(total)

                    currentBinding.tvMonthlyTotal.text = "${formattedTotal}원"

                    // 월 합계 제목도 선택 조건에 맞게 변경
                    val categoryLabel =
                        if (selectedCategory == "전체 카테고리") {
                            ""
                        } else {
                            "$selectedCategory "
                        }

                    currentBinding.tvMonthLabel.text = getString(
                        R.string.record_total_format,
                        selectedMonth.monthValue,
                        categoryLabel
                    )
                }

            } catch (e: Exception) {
                Log.e("RecordFragment", "지출 내역 조회 실패", e)

                activity?.runOnUiThread {
                    if (_binding != null) {
                        Toast.makeText(
                            requireContext(),
                            "지출 내역을 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Fragment의 View가 제거될 때 binding 참조 해제
        _binding = null
    }
}
