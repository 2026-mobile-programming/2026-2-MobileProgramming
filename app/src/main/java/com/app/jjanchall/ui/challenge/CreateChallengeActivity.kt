package com.app.jjanchall.ui.challenge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.jjanchall.R
import com.app.jjanchall.databinding.ActivityCreateChallengeBinding
import android.app.DatePickerDialog
import android.widget.TextView
import java.util.Calendar
import java.util.Locale
import android.widget.Toast

/** CHALLENGE-02 챌린지 만들기 화면 */
class CreateChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateChallengeBinding

    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null

    private var targetDays = 10


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // XML 레이아웃 연결
        binding = ActivityCreateChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상단 바 제목 설정
        binding.appBar.tvAppBarTitle.setText(
            R.string.challenge_create_title
        )

        // 뒤로가기 버튼
        binding.appBar.btnBack.setOnClickListener {
            finish()
        }

        // 시작일 선택
        binding.tvStartDate.setOnClickListener {
            showDatePicker(binding.tvStartDate)
        }

        // 종료일 선택
        binding.tvEndDate.setOnClickListener {
            showDatePicker(binding.tvEndDate)
        }

// 목표 일수 초기 표시
        updateTargetDays()

// 목표 일수 감소
        binding.btnTargetMinus.setOnClickListener {
            targetDays = (targetDays - 1).coerceAtLeast(1)
            updateTargetDays()
        }

// 목표 일수 증가
        binding.btnTargetPlus.setOnClickListener {
            targetDays += 1
            updateTargetDays()
        }


    }

    private fun showDatePicker(target: TextView) {

        val today = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->

                val selectedDate = String.format(
                    Locale.KOREA,
                    "%04d.%02d.%02d",
                    year,
                    month + 1,
                    day
                )

                if (target == binding.tvStartDate) {

                    // 시작일 저장
                    selectedStartDate = selectedDate
                    binding.tvStartDate.text = selectedDate

                    // 기존 종료일이 새 시작일보다 빠르면 초기화
                    if (selectedEndDate != null &&
                        selectedEndDate!! < selectedDate) {

                        selectedEndDate = null
                        binding.tvEndDate.setText(
                            R.string.challenge_end_date
                        )

                        Toast.makeText(
                            this,
                            R.string.challenge_date_reset,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else if (target == binding.tvEndDate) {

                    // 종료일이 시작일보다 빠른 경우
                    if (selectedStartDate != null &&
                        selectedDate < selectedStartDate!!) {

                        Toast.makeText(
                            this,
                            R.string.challenge_date_order_error,
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        // 정상적인 종료일 저장
                        selectedEndDate = selectedDate
                        binding.tvEndDate.text = selectedDate
                    }
                }
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }

    private fun updateTargetDays() {
        binding.tvTargetDays.text = getString(
            R.string.challenge_target_days_format,
            targetDays
        )
    }


}
