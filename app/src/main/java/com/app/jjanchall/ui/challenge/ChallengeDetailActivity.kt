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
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

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
        // 선택한 챌린지의 실제 데이터 불러오기
        loadChallengeDetail()

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

    private fun loadChallengeDetail() {

        // 이전 화면에서 전달받은 챌린지 ID
        val challengeId = intent.getStringExtra("challenge_id")

        if (challengeId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "챌린지 정보를 찾을 수 없어요",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        // Firestore에서 해당 챌린지 조회
        FirebaseFirestore.getInstance()
            .collection("challenges")
            .document(challengeId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    Toast.makeText(
                        this,
                        "존재하지 않는 챌린지예요",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@addOnSuccessListener
                }

                // 실제 데이터 가져오기
                val title = document.getString("title").orEmpty()
                val startDate = document.getString("startDate").orEmpty()
                val endDate = document.getString("endDate").orEmpty()
                val inviteCode = document.getString("inviteCode").orEmpty()
                val targetDays = document.getLong("targetDays")
                    ?.toInt() ?: 0

                // 상단 챌린지 제목
                binding.appBar.tvAppBarTitle.text = title

                // 초대 코드
                binding.tvDetailInviteCode.text = getString(
                    R.string.challenge_detail_invite_format,
                    inviteCode
                )

                // 무지출 목표 표시
                // 실제 무지출 기록은 아직 연결 전이므로 0일
                binding.tvCalendarProgress.text = getString(
                    R.string.challenge_calendar_progress_format,
                    0,
                    targetDays
                )

                // 기간과 달력 월 설정
                if (startDate.length == 10 && endDate.length == 10) {

                    val startText = startDate.replace('-', '.')
                    val endText = endDate.substring(5).replace('-', '.')

                    binding.tvDetailPeriod.text = getString(
                        R.string.challenge_detail_period_format,
                        startText,
                        endText,
                        calculateDetailDday(endDate)
                    )

                    // 챌린지 시작일의 연도와 월로 달력 생성
                    val parts = startDate.split("-")
                    val year = parts[0].toIntOrNull()
                    val month = parts[1].toIntOrNull()

                    if (year != null && month != null &&
                        month in 1..12) {
                        setupCalendar(year, month)
                    }
                }
            }
            .addOnFailureListener { exception ->

                Log.e(
                    "ChallengeDetail",
                    "챌린지 상세 조회 실패",
                    exception
                )

                Toast.makeText(
                    this,
                    "챌린지 정보를 불러오지 못했어요",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun calculateDetailDday(endDate: String): String {

        return try {
            val format = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.KOREA
            ).apply {
                isLenient = false
            }

            val end = format.parse(endDate) ?: return ""

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val days = (
                    (end.time - today.timeInMillis) / 86_400_000L
                    ).toInt()

            when {
                days > 0 -> "D-$days"
                days == 0 -> "D-Day"
                else -> "D+${-days}"
            }

        } catch (e: Exception) {
            ""
        }
    }

}
