package com.app.jjanchall.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.jjanchall.R
import com.app.jjanchall.data.auth.AuthRepository
import com.app.jjanchall.data.local.Expense
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.FragmentHomeBinding
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

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
        binding.tvSummaryLabel.text =
            getString(R.string.home_summary_label, nickname)
    }

    // 하단 탭은 Fragment를 숨겼다 보여주는 방식이라 onResume이 다시 불리지 않음 → 탭이 보일 때도 갱신
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) updateHomeExpenses()
    }

    override fun onResume() {
        super.onResume()

        // 홈 화면으로 돌아올 때 SQLite 지출 내역 갱신
        updateHomeExpenses()
    }

    private fun updateHomeExpenses() {
        val appContext = context?.applicationContext ?: return
        val currentMonth = YearMonth.now()

        // SQLite 조회는 메인 스레드 외부에서 실행
        Thread {
            try {
                val (monthlyExpenses, recentExpenses) =
                    ExpenseDbHelper(appContext).use { db ->
                        val monthly = db.getExpensesForMonth(
                            currentMonth.toString()
                        )

                        // DB에서 날짜 및 시간 내림차순으로 반환
                        val recent = db.getAllExpenses().take(2)

                        Pair(monthly, recent)
                    }

                val monthlyTotal = monthlyExpenses.sumOf { it.amount }

                activity?.runOnUiThread {
                    val currentBinding = _binding ?: return@runOnUiThread

                    val numberFormat =
                        NumberFormat.getNumberInstance(Locale.KOREA)

                    // 이번 달 지출 합계
                    currentBinding.tvSummaryAmount.text =
                        "${numberFormat.format(monthlyTotal)}원"

                    // 최근 지출 2건
                    showRecentExpenses(
                        currentBinding,
                        recentExpenses,
                        numberFormat
                    )
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "홈 지출 내역 조회 실패", e)

                activity?.runOnUiThread {
                    if (_binding != null) {
                        Toast.makeText(
                            requireContext(),
                            "홈 지출 내역을 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.start()
    }

    private fun showRecentExpenses(
        currentBinding: FragmentHomeBinding,
        recentExpenses: List<Expense>,
        numberFormat: NumberFormat
    ) {
        if (recentExpenses.isNotEmpty()) {
            val first = recentExpenses[0]

            currentBinding.tvRecentStore1.text = first.store
            currentBinding.tvRecentCategory1.text = first.category
            currentBinding.tvRecentAmount1.text =
                "-${numberFormat.format(first.amount)}원"
        } else {
            currentBinding.tvRecentStore1.text = ""
            currentBinding.tvRecentCategory1.text = ""
            currentBinding.tvRecentAmount1.text = ""
        }

        if (recentExpenses.size >= 2) {
            val second = recentExpenses[1]

            currentBinding.tvRecentStore2.text = second.store
            currentBinding.tvRecentCategory2.text = second.category
            currentBinding.tvRecentAmount2.text =
                "-${numberFormat.format(second.amount)}원"
        } else {
            currentBinding.tvRecentStore2.text = ""
            currentBinding.tvRecentCategory2.text = ""
            currentBinding.tvRecentAmount2.text = ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
