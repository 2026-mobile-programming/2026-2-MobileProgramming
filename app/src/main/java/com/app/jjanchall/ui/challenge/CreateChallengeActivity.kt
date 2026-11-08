package com.app.jjanchall.ui.challenge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.jjanchall.R
import com.app.jjanchall.databinding.ActivityCreateChallengeBinding

/** CHALLENGE-02 챌린지 만들기 화면 */
class CreateChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateChallengeBinding

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
    }
}
