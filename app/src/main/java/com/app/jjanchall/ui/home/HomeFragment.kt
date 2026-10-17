package com.app.jjanchall.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.app.jjanchall.R
import com.app.jjanchall.data.auth.AuthRepository
import com.app.jjanchall.databinding.FragmentHomeBinding

/** HOME-01 홈 탭 */
class HomeFragment : Fragment() {

    // Fragment는 화면(View)이 먼저 사라질 수 있어서 binding을 onDestroyView에서 비워요
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        binding.header.tvHeaderTitle.setText(R.string.app_name)

        val nickname = AuthRepository(requireContext()).currentNickname.orEmpty()
        binding.tvSummaryLabel.text = getString(R.string.home_summary_label, nickname)

        // TODO: SQLite 연동 후 실제 지출 데이터로 교체
        // Issue #8의 10월 샘플 지출 금액으로 합계 계산 테스트
        val testAmounts = listOf(4800, 1500, 3000, 18600)

        // 이번 달 지출 금액 합계 계산
        val monthlyTotal = testAmounts.sum()

        // 천 단위 쉼표를 적용해 홈 화면에 표시
        binding.tvSummaryAmount.text =
            "${String.format(java.util.Locale.KOREA, "%,d", monthlyTotal)}원"
        // TODO: SQLite 연동 후 최근 지출 데이터로 교체
        binding.tvRecentStore1.text = "GS25 성신여대점"
        binding.tvRecentCategory1.text = "식비"
        binding.tvRecentAmount1.text = "-4,800원"

        binding.tvRecentStore2.text = "버스"
        binding.tvRecentCategory2.text = "교통"
        binding.tvRecentAmount2.text = "-1,500원"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
