package com.app.jjanchall.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.jjanchall.R
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

    // RecyclerView에 표시할 임시 데이터
    // 추후 DB 담당 팀원이 구현한 ExpenseDao의 getMonthly()로 교체할 예정
    private val expenses = listOf(
        ExpenseItem(
            category = "식비",
            store = "GS25 성신여대점",
            memo = "영수증 · 삼각김밥, 음료",
            amount = 4800,
            date = "2026-10-08"
        ),
        ExpenseItem(
            category = "교통",
            store = "버스",
            memo = "직접 입력",
            amount = 1500,
            date = "2026-10-08"
        ),
        ExpenseItem(
            category = "카페",
            store = "학교 앞 카페",
            memo = "아이스 아메리카노",
            amount = 3000,
            date = "2026-10-07"
        ),
        ExpenseItem(
            category = "식비",
            store = "이마트24",
            memo = "영수증 · 장보기",
            amount = 18600,
            date = "2026-10-07"
        ),
        // 다른 월 조회 기능을 테스트하기 위한 샘플 데이터
        ExpenseItem(
            category = "식비",
            store = "편의점",
            memo = "간식",
            amount = 6500,
            date = "2026-09-19"
        ),
        ExpenseItem(
            category = "교통",
            store = "지하철",
            memo = "직접 입력",
            amount = 1550,
            date = "2026-09-10"
        ),
        ExpenseItem(
            category = "식비",
            store = "마트",
            memo = "장보기",
            amount = 24000,
            date = "2026-08-21"
        )
    )

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

        // 최초 화면 표시
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

        // 선택 월에 해당하는 지출 데이터만 가져오기
        val monthlyExpenses = expenses.filter { expense ->
            expense.date.startsWith(selectedMonth.toString())
        }

        // 월별 데이터에 카테고리 조건 추가
        val filteredExpenses =
            if (selectedCategory == "전체 카테고리") {
                monthlyExpenses
            } else {
                monthlyExpenses.filter { expense ->
                    expense.category == selectedCategory
                }
            }

        // ExpenseAdapter의 날짜별 그룹화 기능 재사용
        binding.recyclerViewExpenses.adapter =
            ExpenseAdapter(filteredExpenses)

        // 현재 선택한 연도 및 월 표시
        binding.tvMonthSelect.text = getString(
            R.string.record_month_format,
            selectedMonth.year,
            selectedMonth.monthValue
        )

        // 현재 선택한 카테고리 표시
        binding.tvCategoryFilter.text = selectedCategory

        // 현재 선택한 조건의 총 지출 금액 계산
        val total = filteredExpenses.sumOf { it.amount }

        val formattedTotal =
            NumberFormat.getNumberInstance(Locale.KOREA)
                .format(total)

        binding.tvMonthlyTotal.text = "${formattedTotal}원"

        // 월 합계 제목도 선택 조건에 맞게 변경
        val categoryLabel =
            if (selectedCategory == "전체 카테고리") {
                ""
            } else {
                "$selectedCategory "
            }

        binding.tvMonthLabel.text = getString(
            R.string.record_total_format,
            selectedMonth.monthValue,
            categoryLabel
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Fragment의 View가 제거될 때 binding 참조 해제
        _binding = null
    }
}
