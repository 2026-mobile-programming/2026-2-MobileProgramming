package com.app.jjanchall.ui.challenge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.jjanchall.databinding.ItemChallengeBinding

// 화면에 표시할 챌린지 카드 데이터
data class ChallengeCardUiModel(
    val id: String,
    val title: String,
    val dDay: String,
    val completedDays: Int,
    val targetDays: Int,
    val progressText: String,
    val participantsText: String
)

// 챌린지 목록 Adapter
class ChallengeAdapter(
    private val items: List<ChallengeCardUiModel>,
    private val onChallengeClick: (ChallengeCardUiModel) -> Unit
) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    // 카드 하나의 View를 관리
    class ChallengeViewHolder(
        val binding: ItemChallengeBinding
    ) : RecyclerView.ViewHolder(binding.root)

    // 카드 레이아웃 생성
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChallengeViewHolder {

        val binding = ItemChallengeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ChallengeViewHolder(binding)
    }

    // 각 카드에 데이터 표시
    override fun onBindViewHolder(
        holder: ChallengeViewHolder,
        position: Int
    ) {
        val item = items[position]
        val binding = holder.binding

        binding.tvChallengeName.text = item.title
        binding.tvChallengeDday.text = item.dDay

        binding.pbChallengeProgress.max =
            item.targetDays.coerceAtLeast(1)

        binding.pbChallengeProgress.progress =
            item.completedDays.coerceIn(
                0,
                item.targetDays.coerceAtLeast(1)
            )

        binding.tvChallengeProgress.text = item.progressText
        binding.tvChallengeParticipants.text = item.participantsText

        // 챌린지 카드 클릭 이벤트
        binding.root.setOnClickListener {
            onChallengeClick(item)
        }
    }

    // 전체 챌린지 카드 개수
    override fun getItemCount(): Int = items.size
}
