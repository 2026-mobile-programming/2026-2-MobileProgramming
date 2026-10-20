package com.app.jjanchall.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.app.jjanchall.R
import com.app.jjanchall.data.auth.AuthRepository
import com.app.jjanchall.data.local.Expense
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.FragmentHomeBinding
import com.app.jjanchall.ui.record.ExpenseEditActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

/** HOME-01 홈 탭 */
class HomeFragment : Fragment() {

    // Fragment는 화면(View)이 먼저 사라질 수 있어서 binding을 onDestroyView에서 비워요
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var requestId = 0

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

        // 직접 입력 화면 이동
        binding.btnManual.setOnClickListener {
            startActivity(ExpenseEditActivity.newIntent(requireContext()))
        }

        // 영수증 화면(#7) 머지 후 연결
        binding.btnReceipt.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.setting_coming_soon,
                Toast.LENGTH_SHORT
            ).show()
        }

        // 최근 지출 전체 보기 → 지출 기록 탭 이동
        binding.tvViewAllExpenses.setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottom_nav)
                .selectedItemId = R.id.tab_record
        }

        // 통계 보기 버튼은 Issue #18 통계 화면 구현 후 연결
    }

    // 하단 탭은 Fragment를 숨겼다 보여주는 방식이라 onResume이 다시 불리지 않음 → 탭이 보일 때도 갱신
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!hidden && _binding != null) {
            updateHomeExpenses()
        }
    }

    override fun onResume() {
        super.onResume()

        // 홈 화면으로 돌아올 때 SQLite 지출 내역 갱신
        updateHomeExpenses()
    }

    private fun updateHomeExpenses() {
        val appContext = context?.applicationContext ?: return
        val currentMonth = YearMonth.now()
        val currentRequest = ++requestId

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

                // 이번 달 지출 합계
                val monthlyTotal = monthlyExpenses.sumOf { it.amount }

                // 카테고리별 금액 합산 후 상위 3개 선택
                val topCategories = monthlyExpenses
                    .groupBy { it.category }
                    .map { (category, expenses) ->
                        category to expenses.sumOf { it.amount }
                    }
                    .sortedByDescending { it.second }
                    .take(3)

                activity?.runOnUiThread {
                    val currentBinding = _binding ?: return@runOnUiThread

                    // 이전 조회 결과가 나중에 도착하면 무시
                    if (currentRequest != requestId) {
                        return@runOnUiThread
                    }

                    val numberFormat =
                        NumberFormat.getNumberInstance(Locale.KOREA)

                    // 이번 달 지출 합계
                    currentBinding.tvSummaryAmount.text =
                        "${numberFormat.format(monthlyTotal)}원"

                    // 카테고리 TOP 3
                    showTopCategories(
                        currentBinding,
                        topCategories,
                        numberFormat
                    )

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
                    if (_binding != null && currentRequest == requestId) {
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

    // 카테고리별 지출 상위 3개 표시
    private fun showTopCategories(
        currentBinding: FragmentHomeBinding,
        topCategories: List<Pair<String, Int>>,
        numberFormat: NumberFormat
    ) {
        val rows = listOf(
            currentBinding.layoutTopCategory1,
            currentBinding.layoutTopCategory2,
            currentBinding.layoutTopCategory3
        )

        val names = listOf(
            currentBinding.tvTopCategory1,
            currentBinding.tvTopCategory2,
            currentBinding.tvTopCategory3
        )

        val progresses = listOf(
            currentBinding.progressTopCategory1,
            currentBinding.progressTopCategory2,
            currentBinding.progressTopCategory3
        )

        val amounts = listOf(
            currentBinding.tvTopAmount1,
            currentBinding.tvTopAmount2,
            currentBinding.tvTopAmount3
        )

        // 지출 데이터가 없으면 안내 문구 표시
        currentBinding.tvTop3Empty.visibility =
            if (topCategories.isEmpty()) View.VISIBLE else View.GONE

        // 1위 카테고리 금액을 기준으로 막대 길이 계산
        val maxAmount = topCategories.firstOrNull()?.second ?: 0

        for (index in rows.indices) {
            if (index >= topCategories.size) {
                rows[index].visibility = View.GONE
                continue
            }

            val (category, amount) = topCategories[index]

            rows[index].visibility = View.VISIBLE
            names[index].text = category
            amounts[index].text = "${numberFormat.format(amount)}원"

            val progress =
                if (maxAmount > 0) {
                    (amount.toDouble() / maxAmount * 100)
                        .toInt()
                        .coerceIn(0, 100)
                } else {
                    0
                }

            progresses[index].progress = progress

            // 순위가 아닌 실제 카테고리 기준으로 색상 설정
            progresses[index].progressTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    getCategoryColor(category)
                )
        }
    }

    // 카테고리별 진행 막대 색상
    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "식비" -> R.color.chart_food
            "카페", "카페·간식", "간식" -> R.color.chart_cafe
            "교통" -> R.color.chart_transport
            "쇼핑" -> R.color.chart_shopping
            "생활" -> R.color.chart_living
            else -> R.color.chart_etc
        }
    }

    // 최근 지출 2건 표시
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
        requestId++
        _binding = null
    }
}
