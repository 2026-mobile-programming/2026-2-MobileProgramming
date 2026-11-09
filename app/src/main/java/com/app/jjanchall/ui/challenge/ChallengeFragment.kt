package com.app.jjanchall.ui.challenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.jjanchall.R
import com.app.jjanchall.databinding.FragmentChallengeBinding
import android.content.Intent
import com.app.jjanchall.databinding.DialogJoinChallengeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.widget.doAfterTextChanged
import android.widget.Toast
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** CHALLENGE-01 무지출 챌린지 탭 */
class ChallengeFragment : Fragment() {

    // Fragment는 화면(View)이 먼저 사라질 수 있어서 binding을 onDestroyView에서 비워요
    private var _binding: FragmentChallengeBinding? = null

    private var challengesListener: ListenerRegistration? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChallengeBinding.inflate(
            inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.tvHeaderTitle.setText(R.string.challenge_title)

        // RecyclerView 설정
        binding.rvOngoingChallenges.layoutManager =
            LinearLayoutManager(requireContext())

        // Firestore에서 내 챌린지 목록 불러오기
        observeMyChallenges()

        // 챌린지 만들기 화면으로 이동
        binding.btnCreateChallenge.setOnClickListener {
            val intent = Intent(
                requireContext(),
                CreateChallengeActivity::class.java
            )
            startActivity(intent)
        }

        // 초대 코드로 참여하기 다이얼로그 열기
        binding.btnJoinChallenge.setOnClickListener {
            showJoinChallengeDialog()
        }
    }

    private fun observeMyChallenges() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(
                requireContext(),
                "로그인이 필요해요",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        challengesListener?.remove()

        challengesListener = db.collection("challenges")
            .whereEqualTo("createdBy", uid)
            .addSnapshotListener { snapshot, error ->

                // 화면이 사라졌다면 처리하지 않음
                val viewBinding = _binding
                    ?: return@addSnapshotListener

                if (error != null) {
                    Log.e("ChallengeFragment", "챌린지 조회 실패", error)

                    Toast.makeText(
                        requireContext(),
                        "챌린지 목록을 불러오지 못했어요",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addSnapshotListener
                }

                // Firestore 문서를 카드 데이터로 변환
                val challenges = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->

                        val title = document.getString("title")
                            ?: return@mapNotNull null

                        val targetDays = (
                                document.getLong("targetDays") ?: 1L
                                ).toInt().coerceAtLeast(1)

                        val endDate = document.getString("endDate")

                        ChallengeCardUiModel(
                            id = document.id,
                            title = title,
                            dDay = getChallengeDday(endDate),
                            completedDays = 0,
                            targetDays = targetDays,
                            progressText = "나 0 / ${targetDays}일",
                            participantsText = "참여 1명"
                        )
                    }

                // RecyclerView 갱신
                viewBinding.rvOngoingChallenges.adapter =
                    ChallengeAdapter(challenges) { challenge ->

                        val intent = Intent(
                            requireContext(),
                            ChallengeDetailActivity::class.java
                        )

                        // Firestore 챌린지 문서 ID 전달
                        intent.putExtra("challenge_id", challenge.id)

                        // 기존 제목 전달도 유지
                        intent.putExtra("challenge_title", challenge.title)

                        intent.putExtra(
                            "challenge_title",
                            challenge.title
                        )

                        startActivity(intent)
                    }
            }
    }

    private fun getChallengeDday(endDate: String?): String {

        if (endDate == null) return ""

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

    override fun onDestroyView() {
        challengesListener?.remove()
        challengesListener = null

        super.onDestroyView()
        binding.rvOngoingChallenges.adapter = null
        _binding = null
    }

    private fun showJoinChallengeDialog() {

        // 다이얼로그 XML 연결
        val dialogBinding = DialogJoinChallengeBinding.inflate(
            layoutInflater
        )

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // 임시 테스트용 초대 코드
        // 실제 유효성 검사는 Firestore 연동 시 구현
        val previewInviteCode = "K7Q2MX"

        // 처음에는 찾은 챌린지를 숨김
        dialogBinding.layoutFoundChallenge.visibility = View.GONE

        // 초대 코드가 입력될 때마다 검사
        dialogBinding.etInviteCode.doAfterTextChanged { editable ->
            dialogBinding.tilInviteCode.error = null

            val enteredCode = editable
                ?.toString()
                ?.trim()
                ?.uppercase()
                .orEmpty()

            // 테스트용 코드와 일치하는지 확인
            val isValidPreviewCode =
                enteredCode == previewInviteCode

            // 일치하면 표시, 아니면 숨김
            dialogBinding.layoutFoundChallenge.visibility =
                if (isValidPreviewCode) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        // 취소 버튼
        dialogBinding.btnCancelJoin.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmJoin.setOnClickListener {

            // 사용자가 입력한 초대 코드 가져오기
            val enteredCode = dialogBinding.etInviteCode.text
                ?.toString()
                ?.trim()
                ?.uppercase()
                .orEmpty()

            // 이전 오류 메시지 초기화
            dialogBinding.tilInviteCode.error = null

            when {
                // 1. 아무것도 입력하지 않은 경우
                enteredCode.isBlank() -> {
                    dialogBinding.tilInviteCode.error =
                        getString(R.string.challenge_join_code_required)
                }

                // 2. 영문/숫자 6자리 형식이 아닌 경우
                !enteredCode.matches(Regex("[A-Z0-9]{6}")) -> {
                    dialogBinding.tilInviteCode.error =
                        getString(R.string.challenge_join_code_invalid)
                }

                // 3. 임시 테스트 코드와 일치하지 않는 경우
                enteredCode != previewInviteCode -> {
                    dialogBinding.tilInviteCode.error =
                        getString(R.string.challenge_join_code_not_found)
                }

                // 4. 유효한 테스트 코드인 경우
                else -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.challenge_join_preview_notice,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

}