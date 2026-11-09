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

/** CHALLENGE-01 무지출 챌린지 탭 */
class ChallengeFragment : Fragment() {

    // Fragment는 화면(View)이 먼저 사라질 수 있어서 binding을 onDestroyView에서 비워요
    private var _binding: FragmentChallengeBinding? = null
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

        // Firestore 연동 전 화면 테스트용 데이터
        val previewChallenges = listOf(
            ChallengeCardUiModel(
                title = "10월 무지출 10일 도전",
                dDay = "D-23",
                completedDays = 4,
                targetDays = 10,
                progressText = "나 4 / 10일",
                participantsText = "참여 3명 · 1등"
            ),
            ChallengeCardUiModel(
                title = "배달 없는 주말",
                dDay = "D-3",
                completedDays = 2,
                targetDays = 4,
                progressText = "나 2 / 4일",
                participantsText = "참여 2명"
            )
        )

        // RecyclerView 설정
        binding.rvOngoingChallenges.layoutManager =
            LinearLayoutManager(requireContext())

        // 챌린지 데이터를 Adapter에 전달
        binding.rvOngoingChallenges.adapter =
            ChallengeAdapter(previewChallenges) { challenge ->

                // 선택한 챌린지의 상세 화면으로 이동
                val intent = Intent(
                    requireContext(),
                    ChallengeDetailActivity::class.java
                )

                // 클릭한 챌린지 이름 전달
                intent.putExtra("challenge_title", challenge.title)

                startActivity(intent)
            }


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

    override fun onDestroyView() {
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
