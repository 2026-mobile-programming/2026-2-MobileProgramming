package com.app.jjanchall.ui.challenge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.jjanchall.R
import com.app.jjanchall.databinding.ActivityChallengeDetailBinding
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Calendar
import androidx.recyclerview.widget.LinearLayoutManager

class ChallengeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChallengeDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChallengeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상단 제목
        // 선택한 챌린지 이름 가져오기
        val challengeTitle = intent.getStringExtra("challenge_title")
            ?: getString(R.string.challenge_detail_title)

        // 상단 제목에 표시
        binding.appBar.tvAppBarTitle.text = challengeTitle


        // 뒤로가기 버튼
        binding.appBar.btnBack.setOnClickListener {
            finish()
        }
        // 임시 데이터로 2026년 10월 달력 표시
        setupCalendar(2026, 10)

// 임시 참여자 순위 데이터
        val previewRanking = listOf(
            ChallengeRankingUiModel(
                rank = 1,
                name = "짠돌이",
                days = 5,
                isMe = false
            ),
            ChallengeRankingUiModel(
                rank = 2,
                name = "나",
                days = 4,
                isMe = true
            ),
            ChallengeRankingUiModel(
                rank = 3,
                name = "알뜰살뜰",
                days = 2,
                isMe = false
            )
        )

// RecyclerView에 순위 데이터 연결
        binding.rvChallengeRanking.layoutManager =
            LinearLayoutManager(this)

        binding.rvChallengeRanking.adapter =
            ChallengeRankingAdapter(previewRanking)

    }

    private fun setupCalendar(year: Int, month: Int) {

        val grid = binding.gridNoSpendCalendar
        grid.removeAllViews()

        // 임시 데이터: Firestore 연동 후 실제 기록으로 교체
        val noSpendDays = setOf(2, 4, 5, 6)
        val spentDays = setOf(1, 3, 7)

        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")

        // dp 단위를 실제 화면 픽셀로 변환
        fun dp(value: Int): Int =
            (value * resources.displayMetrics.density + 0.5f).toInt()

        // 달력 칸 하나 만들기
        fun addCell(text: String, day: Int? = null) {

            val position = grid.childCount
            val row = position / 7
            val column = position % 7

            val cell = TextView(this).apply {
                this.text = text
                textSize = if (day == null) 12f else 14f
                gravity = Gravity.CENTER
                includeFontPadding = false

                setTextColor(
                    ContextCompat.getColor(
                        this@ChallengeDetailActivity,
                        R.color.text_primary
                    )
                )

                if (day != null) {
                    val backgroundShape = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()

                        when (day) {
                            in noSpendDays -> {
                                setColor(
                                    ContextCompat.getColor(
                                        this@ChallengeDetailActivity,
                                        R.color.bg_brand
                                    )
                                )
                            }

                            in spentDays -> {
                                setColor(
                                    ContextCompat.getColor(
                                        this@ChallengeDetailActivity,
                                        R.color.border_default
                                    )
                                )
                            }

                            8 -> {
                                setColor(
                                    ContextCompat.getColor(
                                        this@ChallengeDetailActivity,
                                        R.color.bg_default
                                    )
                                )
                                setStroke(
                                    dp(2),
                                    ContextCompat.getColor(
                                        this@ChallengeDetailActivity,
                                        R.color.border_brand
                                    )
                                )
                            }

                            else -> {
                                setColor(
                                    ContextCompat.getColor(
                                        this@ChallengeDetailActivity,
                                        R.color.bg_default
                                    )
                                )
                                setStroke(
                                    dp(1),
                                    ContextCompat.getColor(
                                        this@ChallengeDetailActivity,
                                        R.color.border_default
                                    ),
                                    dp(3).toFloat(),
                                    dp(3).toFloat()
                                )
                            }
                        }
                    }

                    background = backgroundShape

                    if (day in noSpendDays) {
                        setTextColor(
                            ContextCompat.getColor(
                                this@ChallengeDetailActivity,
                                R.color.text_on_brand
                            )
                        )
                    }
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(36)
                rowSpec = GridLayout.spec(row)
                columnSpec = GridLayout.spec(
                    column, 1, GridLayout.FILL, 1f
                )
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }

            grid.addView(cell, params)
        }

        // 1. 요일 표시
        weekdays.forEach { addCell(it) }

        // 2. 1일 이전 빈칸
        repeat(firstDayOfWeek) {
            addCell("")
        }

        // 3. 실제 날짜 표시
        for (day in 1..lastDay) {
            addCell(day.toString(), day)
        }
    }

}
