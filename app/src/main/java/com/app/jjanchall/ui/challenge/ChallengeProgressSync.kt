
package com.app.jjanchall.ui.challenge

import android.content.Context
import android.util.Log
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors

object ChallengeProgressSync {

    // 동기화 요청을 순서대로 실행하는 단일 작업 스레드
    private val syncExecutor =
        Executors.newSingleThreadExecutor()

    fun sync(context: Context) {
        val uid = FirebaseAuth.getInstance()
            .currentUser?.uid ?: return

        val appContext = context.applicationContext

        syncExecutor.execute {

            // 동기화 대기 중 계정이 바뀌었다면 실행하지 않음
            if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
                return@execute
            }

            // 1. SQLite에서 지출 날짜 조회
            val spentDates = try {
                ExpenseDbHelper(appContext).use { db ->
                    db.getAllExpenses()
                        .filter { it.amount > 0 }
                        .mapNotNull { expense ->
                            runCatching {
                                LocalDate.parse(expense.date)
                            }.getOrNull()
                        }
                        .toSet()
                }
            } catch (e: Exception) {
                Log.e(
                    "ChallengeProgressSync",
                    "지출 조회 실패",
                    e
                )
                return@execute
            }

            val firestore = FirebaseFirestore.getInstance()

            // 2. 현재 사용자가 참여한 챌린지 조회
            val members = try {
                Tasks.await(
                    firestore.collectionGroup("members")
                        .whereEqualTo("uid", uid)
                        .get()
                )
            } catch (e: Exception) {
                Log.e(
                    "ChallengeProgressSync",
                    "참여 챌린지 조회 실패",
                    e
                )
                return@execute
            }

            // 3. 각 챌린지 성공 일수 계산 및 저장
            for (member in members.documents) {

                if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
                    return@execute
                }

                val challengeRef =
                    member.reference.parent.parent ?: continue

                try {
                    val challenge = Tasks.await(
                        challengeRef.get()
                    )

                    val startText =
                        challenge.getString("startDate") ?: continue

                    val endText =
                        challenge.getString("endDate") ?: continue

                    val start = runCatching {
                        LocalDate.parse(startText)
                    }.getOrNull() ?: continue

                    val end = runCatching {
                        LocalDate.parse(endText)
                    }.getOrNull() ?: continue

                    val lastDate = minOf(
                        end,
                        LocalDate.now()
                    )

                    val completedDays =
                        if (lastDate.isBefore(start)) {
                            0
                        } else {
                            val totalDays =
                                ChronoUnit.DAYS.between(
                                    start,
                                    lastDate
                                ) + 1

                            val spentCount =
                                spentDates.count { date ->
                                    !date.isBefore(start) &&
                                            !date.isAfter(lastDate)
                                }

                            (totalDays - spentCount).toInt()
                        }

                    // 이전 업데이트 완료 후 다음 작업 진행
                    Tasks.await(
                        member.reference.update(
                            "completedDays",
                            completedDays
                        )
                    )

                } catch (e: Exception) {
                    Log.e(
                        "ChallengeProgressSync",
                        "챌린지 진행률 동기화 실패",
                        e
                    )
                }
            }
        }
    }
}
