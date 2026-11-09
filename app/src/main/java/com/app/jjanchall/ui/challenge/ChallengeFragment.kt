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
import com.google.firebase.firestore.FieldValue
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.AggregateSource


/** CHALLENGE-01 무지출 챌린지 탭 */
class ChallengeFragment : Fragment() {

    // Fragment는 화면(View)이 먼저 사라질 수 있어서 binding을 onDestroyView에서 비워요
    private var _binding: FragmentChallengeBinding? = null

    private var challengesListener: ListenerRegistration? = null

    private var challengeQueryVersion = 0
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

        val uid = FirebaseAuth.getInstance()
            .currentUser?.uid

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

        // 내가 참여자로 등록된 모든 챌린지 검색
        challengesListener = db.collectionGroup("members")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->

                val viewBinding = _binding
                    ?: return@addSnapshotListener

                if (error != null) {
                    Log.e(
                        "ChallengeFragment",
                        "참여 챌린지 조회 실패",
                        error
                    )

                    Toast.makeText(
                        viewBinding.root.context,
                        "챌린지 목록을 불러오지 못했어요",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addSnapshotListener
                }

                val currentVersion = ++challengeQueryVersion

                val memberDocuments =
                    snapshot?.documents.orEmpty()

                // 참여 중인 챌린지가 없는 경우
                if (memberDocuments.isEmpty()) {
                    viewBinding.rvOngoingChallenges.adapter =
                        ChallengeAdapter(emptyList()) { }

                    return@addSnapshotListener
                }

                // 참여자 문서에서 상위 챌린지 문서를 찾기
                val memberMap = memberDocuments.associateBy {
                    it.reference.parent.parent?.id
                }

                val challengeTasks = memberDocuments.mapNotNull {
                    it.reference.parent.parent?.get()
                }

                // 챌린지 상세 정보 조회
                Tasks.whenAllSuccess<DocumentSnapshot>(
                    challengeTasks
                ).addOnSuccessListener { documents ->

                    val currentBinding = _binding
                        ?: return@addOnSuccessListener

                    if (currentVersion != challengeQueryVersion) {
                        return@addOnSuccessListener
                    }

                    val challenges = documents.mapNotNull { document ->

                        if (!document.exists()) {
                            return@mapNotNull null
                        }

                        val title = document.getString("title")
                            ?: return@mapNotNull null

                        val targetDays = (
                                document.getLong("targetDays") ?: 1L
                                ).toInt().coerceAtLeast(1)

                        val completedDays = (
                                memberMap[document.id]
                                    ?.getLong("completedDays") ?: 0L
                                ).toInt().coerceAtLeast(0)

                        val endDate = document.getString("endDate")

                        ChallengeCardUiModel(
                            id = document.id,
                            title = title,
                            dDay = getChallengeDday(endDate),
                            completedDays = completedDays,
                            targetDays = targetDays,
                            progressText =
                                "나 $completedDays / ${targetDays}일",
                            participantsText = "참여 중"
                        )
                    }


// RecyclerView에 실제 챌린지 표시
                    val adapter = ChallengeAdapter(challenges) { challenge ->

                        val intent = Intent(
                            requireContext(),
                            ChallengeDetailActivity::class.java
                        )

                        intent.putExtra(
                            "challenge_id",
                            challenge.id
                        )

                        intent.putExtra(
                            "challenge_title",
                            challenge.title
                        )

                        startActivity(intent)
                    }

                    currentBinding.rvOngoingChallenges.adapter = adapter

                    // 각 챌린지의 실제 참여 인원수 조회
                    challenges.forEach { challenge ->

                        db.collection("challenges")
                            .document(challenge.id)
                            .collection("members")
                            .count()
                            .get(AggregateSource.SERVER)
                            .addOnSuccessListener { snapshot ->

                                // 이전 화면이나 오래된 조회 결과라면 무시
                                if (
                                    _binding?.rvOngoingChallenges?.adapter !== adapter ||
                                    currentVersion != challengeQueryVersion
                                ) {
                                    return@addOnSuccessListener
                                }

                                // 실제 참여 인원수 반영
                                adapter.updateParticipantCount(
                                    challenge.id,
                                    snapshot.count
                                )
                            }
                            .addOnFailureListener { exception ->

                                Log.e(
                                    "ChallengeFragment",
                                    "참여 인원수 조회 실패",
                                    exception
                                )
                            }
                    }

                }.addOnFailureListener { exception ->

                    if (_binding == null ||
                        currentVersion != challengeQueryVersion) {
                        return@addOnFailureListener
                    }

                    Log.e(
                        "ChallengeFragment",
                        "챌린지 정보 조회 실패",
                        exception
                    )

                    Toast.makeText(
                        requireContext(),
                        "챌린지 정보를 불러오지 못했어요",
                        Toast.LENGTH_SHORT
                    ).show()
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
        challengeQueryVersion++
        challengesListener = null

        super.onDestroyView()
        binding.rvOngoingChallenges.adapter = null
        _binding = null
    }


    private fun showJoinChallengeDialog() {

        val dialogBinding = DialogJoinChallengeBinding.inflate(
            layoutInflater
        )

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val db = FirebaseFirestore.getInstance()

        // 현재 입력값으로 찾은 챌린지 ID
        var foundChallengeId: String? = null
        var foundInviteCode: String? = null

        // 이전 조회 결과가 뒤늦게 도착하는 문제 방지
        var requestVersion = 0

        // 처음에는 찾은 챌린지 숨기기
        dialogBinding.layoutFoundChallenge.visibility = View.GONE

        // 입력한 코드로 실제 Firestore 조회
        dialogBinding.etInviteCode.doAfterTextChanged { editable ->

            val enteredCode = editable
                ?.toString()
                ?.trim()
                ?.uppercase()
                .orEmpty()

            dialogBinding.tilInviteCode.error = null

            foundChallengeId = null
            foundInviteCode = null
            dialogBinding.layoutFoundChallenge.visibility = View.GONE

            requestVersion++
            val currentVersion = requestVersion

            // 영문·숫자 6자리가 아니면 조회하지 않음
            if (!enteredCode.matches(Regex("[A-Z0-9]{6}"))) {
                return@doAfterTextChanged
            }

            // 초대 코드 조회
            db.collection("inviteCodes")
                .document(enteredCode)
                .get()
                .addOnSuccessListener codeResult@{ codeDocument ->

                    if (!dialog.isShowing ||
                        currentVersion != requestVersion) {
                        return@codeResult
                    }

                    val challengeId =
                        codeDocument.getString("challengeId")
                            ?: return@codeResult

                    // 해당 챌린지의 실제 정보 조회
                    db.collection("challenges")
                        .document(challengeId)
                        .get()
                        .addOnSuccessListener challengeResult@{ challengeDocument ->

                            if (!dialog.isShowing ||
                                currentVersion != requestVersion) {
                                return@challengeResult
                            }

                            if (!challengeDocument.exists() ||
                                challengeDocument.getString("inviteCode") != enteredCode) {
                                return@challengeResult
                            }

                            val title =
                                challengeDocument.getString("title")
                                    ?: return@challengeResult

                            foundChallengeId = challengeId
                            foundInviteCode = enteredCode

                            // 찾은 챌린지 표시
                            dialogBinding.tvFoundChallenge.text = title
                            dialogBinding.layoutFoundChallenge.visibility =
                                View.VISIBLE
                        }
                        .addOnFailureListener {
                            if (dialog.isShowing &&
                                currentVersion == requestVersion) {
                                dialogBinding.tilInviteCode.error =
                                    getString(R.string.challenge_join_lookup_failed)
                            }
                        }
                }
                .addOnFailureListener {
                    if (dialog.isShowing &&
                        currentVersion == requestVersion) {
                        dialogBinding.tilInviteCode.error =
                            getString(R.string.challenge_join_lookup_failed)
                    }
                }
        }

        // 취소 버튼
        dialogBinding.btnCancelJoin.setOnClickListener {
            dialog.dismiss()
        }

        // 실제 챌린지 참여하기
        dialogBinding.btnConfirmJoin.setOnClickListener {

            val enteredCode = dialogBinding.etInviteCode.text
                ?.toString()
                ?.trim()
                ?.uppercase()
                .orEmpty()

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val challengeId = foundChallengeId

            dialogBinding.tilInviteCode.error = null

            when {
                enteredCode.isBlank() -> {
                    dialogBinding.tilInviteCode.error =
                        getString(R.string.challenge_join_code_required)
                }

                !enteredCode.matches(Regex("[A-Z0-9]{6}")) -> {
                    dialogBinding.tilInviteCode.error =
                        getString(R.string.challenge_join_code_invalid)
                }

                uid == null -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.challenge_join_login_required,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                challengeId == null ||
                        foundInviteCode != enteredCode -> {

                    dialogBinding.tilInviteCode.error =
                        getString(R.string.challenge_join_code_not_found)
                }

                else -> {

                    val challengeRef = db.collection("challenges")
                        .document(challengeId)

                    val memberRef = challengeRef
                        .collection("members")
                        .document(uid)

                    // 중복 클릭 방지
                    dialogBinding.btnConfirmJoin.isEnabled = false

                    db.runTransaction { transaction ->

                        // 챌린지가 실제로 존재하고 코드도 맞는지 재확인
                        val challengeDocument =
                            transaction.get(challengeRef)

                        if (!challengeDocument.exists() ||
                            challengeDocument.getString("inviteCode") != enteredCode) {
                            throw IllegalStateException("Invalid invite code")
                        }

                        // 이미 참여했는지 확인
                        val existingMember =
                            transaction.get(memberRef)

                        if (existingMember.exists()) {
                            false
                        } else {

                            transaction.set(
                                memberRef,
                                mapOf(
                                    "uid" to uid,
                                    "completedDays" to 0,
                                    "joinedAt" to FieldValue.serverTimestamp()
                                )
                            )

                            true
                        }

                    }.addOnSuccessListener { joined ->

                        if (!dialog.isShowing) {
                            return@addOnSuccessListener
                        }

                        Toast.makeText(
                            requireContext(),
                            if (joined) {
                                R.string.challenge_join_success
                            } else {
                                R.string.challenge_join_already_member
                            },
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()

                    }.addOnFailureListener {

                        if (dialog.isShowing) {
                            dialogBinding.btnConfirmJoin.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                R.string.challenge_join_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

}