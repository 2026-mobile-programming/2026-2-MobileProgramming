package com.app.jjanchall.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.app.jjanchall.R
import com.app.jjanchall.data.auth.AuthRepository
import com.app.jjanchall.databinding.FragmentHomeBinding
import com.app.jjanchall.ui.record.ExpenseItem
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

/** HOME-01 홈 탭 */
class HomeFragment : Fragment() {

    // Fragment는 화면(View)이 먼저 사라질 수 있어서 binding을 onDestroyView에서 비워요
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // SQLite 연동 전 홈 화면 테스트용 지출 데이터
    // 추후 DB 담당 팀원의 실제 조회 결과로 교체 예정
    private val sampleExpenses = listOf(
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
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 기존 팀원 코드: 홈 화면 제목 표시
        binding.header.tvHeaderTitle.setText(R.string.app_name)

        // 기존 팀원 코드: 로그인한 사용자의 닉네임 표시
        val nickname = AuthRepository(requireContext()).currentNickname.orEmpty()
        binding.tvSummaryLabel.text = getString(R.string.home_summary_label, nickname)

        // 현재 연도와 월
        val currentMonth = YearMonth.now()

        // TODO: SQLite 연동 후 실제 지출 데이터로 교체
        // 현재 월에 해당하는 지출만 선택
        val monthlyExpenses = sampleExpenses.filter { expense ->
            expense.date.startsWith(currentMonth.toString())
        }

        // 이번 달 총 지출 금액 계산
        val monthlyTotal = monthlyExpenses.sumOf { it.amount }

        // 천 단위 쉼표를 적용하여 표시
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
        binding.tvSummaryAmount.text = "${numberFormat.format(monthlyTotal)}원"

        // 날짜가 최신인 지출 2건 선택
        // 날짜가 같으면 sampleExpenses에 먼저 있는 항목을 우선 표시
        val recentExpenses = sampleExpenses
            .sortedByDescending { it.date }
            .take(2)

        // 첫 번째 최근 지출 표시
        if (recentExpenses.isNotEmpty()) {
            val first = recentExpenses[0]

            binding.tvRecentStore1.text = first.store
            binding.tvRecentCategory1.text = first.category
            binding.tvRecentAmount1.text =
                "-${numberFormat.format(first.amount)}원"
        } else {
            binding.tvRecentStore1.text = ""
            binding.tvRecentCategory1.text = ""
            binding.tvRecentAmount1.text = ""
        }

        // 두 번째 최근 지출 표시
        if (recentExpenses.size >= 2) {
            val second = recentExpenses[1]

            binding.tvRecentStore2.text = second.store
            binding.tvRecentCategory2.text = second.category
            binding.tvRecentAmount2.text =
                "-${numberFormat.format(second.amount)}원"
        } else {
            binding.tvRecentStore2.text = ""
            binding.tvRecentCategory2.text = ""
            binding.tvRecentAmount2.text = ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
