package com.app.jjanchall.ui.challenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.jjanchall.R
import com.app.jjanchall.databinding.FragmentChallengeBinding

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
            ChallengeAdapter(previewChallenges)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvOngoingChallenges.adapter = null
        _binding = null
    }
}
