package com.app.jjanchall.ui.challenge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.jjanchall.R
import com.app.jjanchall.databinding.ItemChallengeRankingBinding
import android.graphics.Typeface

// 참여자 순위 데이터
data class ChallengeRankingUiModel(
    val rank: Int,
    val name: String,
    val days: Int,
    val isMe: Boolean
)

class ChallengeRankingAdapter(
    private val items: List<ChallengeRankingUiModel>
) : RecyclerView.Adapter<ChallengeRankingAdapter.RankingViewHolder>() {

    class RankingViewHolder(
        val binding: ItemChallengeRankingBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RankingViewHolder {

        val binding = ItemChallengeRankingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RankingViewHolder,
        position: Int
    ) {
        val item = items[position]
        val binding = holder.binding
        val context = binding.root.context

        // 순위와 이름 표시
        binding.tvRankNumber.text = item.rank.toString()
        binding.tvRankName.text = item.name

        // 내 이름은 500, 다른 사람은 400
        binding.tvRankName.typeface = Typeface.create(
            binding.tvRankName.typeface,
            if (item.isMe) 500 else 400,
            false
        )



        // 무지출 일수 표시
        binding.tvRankDays.text = context.getString(
            R.string.challenge_ranking_days_format,
            item.days
        )

        // 내 순위인 경우 연한 초록색 배경
        val rowColor = if (item.isMe) {
            R.color.bg_brand_subtle
        } else {
            R.color.bg_default
        }

        binding.root.setCardBackgroundColor(
            ContextCompat.getColor(context, rowColor)
        )

        // 내 프로필 원은 초록색
        val avatarColor = if (item.isMe) {
            R.color.bg_brand
        } else {
            R.color.border_default
        }

        binding.cardRankingAvatar.setCardBackgroundColor(
            ContextCompat.getColor(context, avatarColor)
        )

        // 내 순위 숫자 강조
        val rankColor = if (item.isMe) {
            R.color.text_brand
        } else {
            R.color.text_primary
        }

        binding.tvRankNumber.setTextColor(
            ContextCompat.getColor(context, rankColor)
        )
    }

    override fun getItemCount(): Int = items.size
}