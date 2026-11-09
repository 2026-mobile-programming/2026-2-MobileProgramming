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
import java.text.SimpleDateFormat
import java.util.TimeZone

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.security.SecureRandom

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

            val maxDays = getMaxTargetDays()

            if (maxDays == null || targetDays < maxDays) {

                targetDays += 1
                updateTargetDays()

            } else {

                Toast.makeText(
                    this,
                    R.string.challenge_target_limit_error,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        // 챌린지 시작하기 버튼
        binding.btnStartChallenge.setOnClickListener {

            // 입력한 챌린지 이름 가져오기
            val challengeName = binding.etChallengeName.text
                ?.toString()
                ?.trim()
                .orEmpty()

            val startDate = selectedStartDate
            val endDate = selectedEndDate
            val maxDays = getMaxTargetDays()

            // 이전 이름 오류 메시지 초기화
            binding.tilChallengeName.error = null

            when {

                // 1. 챌린지 이름이 비어 있는 경우
                challengeName.isBlank() -> {
                    binding.tilChallengeName.error =
                        getString(R.string.challenge_name_required)

                    binding.etChallengeName.requestFocus()
                }

                // 2. 시작일을 선택하지 않은 경우
                startDate == null -> {
                    Toast.makeText(
                        this,
                        R.string.challenge_start_date_required,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 3. 종료일을 선택하지 않은 경우
                endDate == null -> {
                    Toast.makeText(
                        this,
                        R.string.challenge_end_date_required,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 4. 종료일이 시작일보다 빠른 경우
                endDate < startDate -> {
                    Toast.makeText(
                        this,
                        R.string.challenge_date_order_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 5. 목표 일수가 유효하지 않은 경우
                maxDays == null || targetDays !in 1..maxDays -> {
                    Toast.makeText(
                        this,
                        R.string.challenge_target_invalid,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 6. 모든 입력값이 올바르면 Firestore에 저장
                else -> {
                    saveChallenge(
                        title = challengeName,
                        startDate = startDate,
                        endDate = endDate,
                        targetDays = targetDays
                    )
                }

            }
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
                syncTargetDaysWithPeriod()
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )


        // 오늘 날짜의 00:00:00을 최소 선택 날짜로 설정
        val minCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 종료일 선택 시 시작일보다 빠른 날짜를 선택할 수 없도록 제한
        if (target == binding.tvEndDate) {
            selectedStartDate?.let { startDate ->

                val parts = startDate.split('.')

                val startCalendar = Calendar.getInstance().apply {
                    set(
                        parts[0].toInt(),
                        parts[1].toInt() - 1,
                        parts[2].toInt(),
                        0, 0, 0
                    )
                    set(Calendar.MILLISECOND, 0)
                }

                // 시작일이 오늘보다 미래라면 시작일을 최소 날짜로 설정
                if (startCalendar.after(minCalendar)) {
                    minCalendar.timeInMillis = startCalendar.timeInMillis
                }
            }
        }

        // 최소 날짜 적용
        datePicker.datePicker.minDate = minCalendar.timeInMillis

        // 달력 표시
        datePicker.show()

    }

    private fun updateTargetDays() {
        binding.tvTargetDays.text = getString(
            R.string.challenge_target_days_format,
            targetDays
        )
    }

    private fun getMaxTargetDays(): Int? {

        // 시작일과 종료일이 모두 선택되어야 계산
        val startDate = selectedStartDate ?: return null
        val endDate = selectedEndDate ?: return null

        // yyyy.MM.dd 문자열을 날짜로 변환
        val dateFormat = SimpleDateFormat(
            "yyyy.MM.dd",
            Locale.KOREA
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val start = dateFormat.parse(startDate) ?: return null
        val end = dateFormat.parse(endDate) ?: return null

        // 시작일과 종료일을 모두 포함한 일수
        val diffMillis = end.time - start.time
        val totalDays = (diffMillis / 86_400_000L + 1).toInt()

        return totalDays.coerceAtLeast(1)
    }


    private fun syncTargetDaysWithPeriod() {

        val maxDays = getMaxTargetDays()

        if (maxDays != null && targetDays > maxDays) {
            targetDays = maxDays
        }

        updateTargetDays()
    }

    // 6자리 초대 코드 생성
    private fun generateInviteCode(): String {
        val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()

        return (1..6)
            .map { characters[random.nextInt(characters.length)] }
            .joinToString("")
    }

    // 챌린지 Firestore 저장
    private fun saveChallenge(
        title: String,
        startDate: String,
        endDate: String,
        targetDays: Int
    ) {
        // 로그인 여부 확인
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(
                this,
                "로그인 후 챌린지를 생성할 수 있어요",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        // 새로운 챌린지 문서 ID
        val challengeRef = db.collection("challenges").document()

        // 6자리 초대 코드
        val inviteCode = generateInviteCode()
        val codeRef = db.collection("inviteCodes").document(inviteCode)

        // 생성자를 첫 참여자로 등록
        val memberRef = challengeRef
            .collection("members")
            .document(uid)

        // Firestore에 저장할 챌린지 데이터
        val challengeData = hashMapOf<String, Any>(
            "title" to title,
            "startDate" to startDate.replace('.', '-'),
            "endDate" to endDate.replace('.', '-'),
            "targetDays" to targetDays,
            "inviteCode" to inviteCode,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )

        // 중복 클릭 방지
        binding.btnStartChallenge.isEnabled = false

        db.runTransaction { transaction ->

            // 초대 코드 중복 확인
            if (transaction.get(codeRef).exists()) {
                throw IllegalStateException(
                    "초대 코드가 중복됐어요. 다시 시도해 주세요."
                )
            }

            // 챌린지 저장
            transaction.set(challengeRef, challengeData)

            // 초대 코드와 챌린지 ID 연결
            transaction.set(
                codeRef,
                mapOf(
                    "challengeId" to challengeRef.id
                )
            )

            // 생성자를 첫 참여자로 저장
            transaction.set(
                memberRef,
                mapOf(
                    "uid" to uid,
                    "completedDays" to 0,
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            )

            inviteCode

        }.addOnSuccessListener { code ->

            // 저장 성공 후 초대 코드 표시
            binding.tvInviteCode.text = code

            Toast.makeText(
                this,
                "챌린지 생성 완료!",
                Toast.LENGTH_LONG
            ).show()

            // 중복 생성 방지: 성공 후 버튼 비활성화 유지

        }.addOnFailureListener { exception ->

            binding.btnStartChallenge.isEnabled = true

            Log.e(
                "ChallengeCreate",
                "Firestore 저장 실패",
                exception
            )

            Toast.makeText(
                this,
                "저장에 실패했어요. 다시 시도해 주세요.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


}
