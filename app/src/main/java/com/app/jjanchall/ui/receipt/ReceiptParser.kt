
package com.app.jjanchall.ui.receipt

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

// 영수증에서 추출한 정보
data class ReceiptInfo(
    val amount: String?,
    val date: String?,
    val store: String?
)

object ReceiptParser {

    // OCR로 인식한 글자와 이미지 내 위치
    private data class OcrLine(
        val text: String,
        val box: Rect
    )

    // 금액 후보
    private data class MoneyCandidate(
        val amount: Long,
        val line: OcrLine,
        val hasGrouping: Boolean,
        val hasWon: Boolean
    )

    // 결제 금액을 나타내는 단어
    private val amountKeywords = listOf(
        "실결제금액",
        "결제금액",
        "합계금액",
        "합계",
        "총매출액",
        "총구매액",
        "거래금액",
        "거래액",
        "승인금액",
        "받은금액",
        "총액"
    )

    // 금액으로 착각하면 안 되는 항목
    private val amountExcluded = listOf(
        "카드번호",
        "승인번호",
        "사업자번호",
        "사업자등록번호",
        "가맹점번호",
        "전화번호",
        "처리번호",
        "부가세",
        "공급가액",
        "과세금액",
        "할인금액",
        "거스름돈",
        "잔돈",
        "CATID",
        "TID",
        "TEL",
        "JFNO"
    )

    // 가게 이름을 나타내는 단어
    private val storeKeywords = listOf(
        "가맹점명",
        "매장명",
        "상호명",
        "사업장명",
        "상호"
    )

    // 가게 이름으로 착각하면 안 되는 문구
    private val storeExcluded = listOf(
        "카드",
        "매출표",
        "매출전표",
        "고객용",
        "영수증",
        "승인",
        "사업자",
        "전화",
        "TEL",
        "합계",
        "금액",
        "감사합니다",
        "이미지로저장",
        "EasyCheck",
        "Easy(eck",
        "KICC",
        "VAN",
        "POS",
        "CATID",
        "TID",
        "가맹점번호",
        "가맹점주소",
        "신용승인",
        "실제와",
        "신고",
        "안내",
        "포상금",
        "주소",
        "대표",
        "고객센터",
        "거래",
        "유효기간",
        "처리번호",
        "전자서명",
        "결제",
        "서울특별시",
        "서울 ",
        "경기도",
        "인천광역시",
        "부산광역시",
        "대구광역시",
        "대전광역시",
        "광주광역시",
        "울산광역시",
        "세종특별자치시",
        "남부순환로",
        "대로",
        "번길"
    )

    // 금액 형식 검색
    // 예: 3,000 / 12,700 / 47.000I / 16000
    // 카드번호처럼 하이픈으로 이어진 숫자는 제외
    private val moneyRegex = Regex(
        """(?<![\d,./*%-])(\d{1,3}(?:[,\.]\d{3})+|\d{3,8})(원|[Il|])?(?![\d,./*%-])"""
    )

    // OCR 분석 시작
    fun parse(result: Text): ReceiptInfo {

        val lines = result.textBlocks
            .flatMap { block -> block.lines }
            .mapNotNull { line ->
                val box = line.boundingBox
                    ?: return@mapNotNull null

                OcrLine(
                    text = line.text.trim(),
                    box = box
                )
            }
            .filter { it.text.isNotBlank() }
            .sortedWith(
                compareBy<OcrLine> { it.box.centerY() }
                    .thenBy { it.box.left }
            )

        return ReceiptInfo(
            amount = extractAmount(lines),
            date = extractDate(result.text),
            store = extractStore(lines)
        )
    }

    // 공백 제거
    private fun compact(value: String): String {
        return value.replace(Regex("""\s+"""), "")
    }

    // ---------------------------------------
    // 1. 날짜 추출
    // ---------------------------------------

    private fun extractDate(text: String): String? {

        val regex = Regex(
            """(?<!\d)(20\d{2})[./-]\s*(\d{1,2})[./-]\s*(\d{1,2})(?!\d)"""
        )

        for (match in regex.findAll(text)) {

            val year = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            val day = match.groupValues[3].toInt()

            val date = String.format(
                Locale.KOREA,
                "%04d.%02d.%02d",
                year,
                month,
                day
            )

            val formatter = SimpleDateFormat(
                "yyyy.MM.dd",
                Locale.KOREA
            ).apply {
                isLenient = false
            }

            try {
                formatter.parse(date)
                return date
            } catch (_: Exception) {
                continue
            }
        }

        return null
    }

    // ---------------------------------------
    // 2. 결제 금액 추출
    // ---------------------------------------

    private fun extractAmount(lines: List<OcrLine>): String? {

        val candidates = mutableListOf<MoneyCandidate>()

        // 모든 OCR 줄에서 금액 후보 찾기
        for (line in lines) {

            if (isExcludedAmountLine(line.text)) {
                continue
            }

            for (match in moneyRegex.findAll(line.text)) {

                val numberText = match.groupValues[1]
                val unit = match.groupValues[2]

                val amount = numberText
                    .replace(",", "")
                    .replace(".", "")
                    .toLongOrNull()
                    ?: continue

                // 지나치게 작거나 큰 숫자는 제외
                if (amount !in 100L..100_000_000L) {
                    continue
                }

                candidates.add(
                    MoneyCandidate(
                        amount = amount,
                        line = line,
                        hasGrouping =
                            numberText.contains(",") ||
                                    numberText.contains("."),
                        hasWon = unit == "원"
                    )
                )
            }
        }

        if (candidates.isEmpty()) {
            return null
        }

        // 같은 금액이 여러 번 등장하는지 확인
        val frequency = candidates.groupingBy {
            it.amount
        }.eachCount()

        // 각 금액 후보의 점수 계산
        val scored = candidates.map { candidate ->

            val labelScore = getAmountLabelScore(
                candidate.line,
                lines
            )

            var score = 0

            // 3,000처럼 천 단위 구분 기호가 있으면 우선
            if (candidate.hasGrouping) {
                score += 150
            }

            // '원' 단위가 있으면 우선
            if (candidate.hasWon) {
                score += 50
            }

            // 합계 등의 위치와 관련이 있으면 우선
            score += labelScore

            // 동일 금액이 반복되면 추가 점수
            score += minOf(
                (frequency[candidate.amount] ?: 1) - 1,
                3
            ) * 20

            Triple(candidate, score, labelScore)
        }

        // 신뢰할 만한 금액 후보만 남기기
        val eligible = scored.filter { (candidate, _, labelScore) ->
            candidate.hasGrouping ||
                    candidate.hasWon ||
                    labelScore > 0
        }

        if (eligible.isEmpty()) {
            return null
        }

        // 같은 금액은 가장 높은 점수로 묶기
        val ranked = eligible
            .groupBy { it.first.amount }
            .map { (amount, entries) ->
                Triple(
                    amount,
                    entries.maxOf { it.second },
                    entries.maxOf { it.third }
                )
            }
            .sortedByDescending { it.second }

        val best = ranked.first()

        // 근거가 약한 금액 후보가 여러 개이고
        // 점수까지 비슷하면 임의로 선택하지 않음
        if (ranked.size > 1) {
            val second = ranked[1]

            if (
                best.third == 0 &&
                best.second - second.second < 20
            ) {
                return null
            }
        }

        return best.first.toString()
    }

    // 금액이 아닌 줄 제외
    private fun isExcludedAmountLine(text: String): Boolean {

        val normalized = compact(text)

        return amountExcluded.any { keyword ->
            normalized.contains(
                compact(keyword),
                ignoreCase = true
            )
        }
    }

    // '합계', '결제금액' 등과의 위치 관계 분석
    private fun getAmountLabelScore(
        target: OcrLine,
        lines: List<OcrLine>
    ): Int {

        var bestScore = 0

        for (label in lines) {

            if (isExcludedAmountLine(label.text)) {
                continue
            }

            val normalized = compact(label.text)

            val keyword = amountKeywords.firstOrNull {
                normalized.contains(compact(it))
            }

            // OCR이 '합'과 '계'를 따로 인식한 경우
            val isSplitTotal =
                normalized == "합" ||
                        normalized == "계"

            if (keyword == null && !isSplitTotal) {
                continue
            }

            val keywordWeight = when {
                isSplitTotal -> 5

                keyword == "실결제금액" ||
                        keyword == "결제금액" -> 60

                keyword == "합계금액" ||
                        keyword == "합계" -> 55

                keyword == "총매출액" ||
                        keyword == "총구매액" -> 45

                else -> 25
            }

            // 금액 키워드와 숫자가 같은 OCR 줄에 있는 경우
            if (label === target) {
                bestScore = maxOf(
                    bestScore,
                    130 + keywordWeight
                )
                continue
            }

            // 키워드 오른쪽의 숫자인 경우
            if (
                isSameRow(label.box, target.box) &&
                target.box.centerX() > label.box.centerX()
            ) {
                bestScore = maxOf(
                    bestScore,
                    100 + keywordWeight
                )
            }
        }

        return bestScore
    }

    // 두 OCR 영역이 같은 행에 있는지 확인
    private fun isSameRow(
        first: Rect,
        second: Rect
    ): Boolean {

        val tolerance = max(
            first.height(),
            second.height()
        ) * 0.75f

        return abs(
            first.centerY() - second.centerY()
        ) <= tolerance
    }

    // ---------------------------------------
    // 3. 가게 이름 추출
    // ---------------------------------------

    private fun extractStore(lines: List<OcrLine>): String? {

        // 1. 가맹점명, 매장명 등의 키워드 우선 탐색
        for (keyword in storeKeywords) {

            for (label in lines) {

                val position = label.text.indexOf(keyword)

                if (position < 0) {
                    continue
                }

                val afterKeyword = label.text
                    .substring(position + keyword.length)
                    .trim()
                    .trimStart(':', '：', '-', ' ')
                    .trim()

                if (isValidStore(afterKeyword)) {
                    return afterKeyword
                }

                // 오른쪽에 따로 인식된 가게 이름
                val rightCandidates = lines
                    .filter { candidate ->
                        candidate !== label &&
                                candidate.box.left >=
                                label.box.right - 10 &&
                                isSameRow(
                                    label.box,
                                    candidate.box
                                )
                    }
                    .sortedBy {
                        it.box.left - label.box.right
                    }

                for (candidate in rightCandidates) {

                    if (isValidStore(candidate.text)) {
                        return candidate.text
                    }
                }
            }
        }

        // 2. 명확한 키워드가 없는 경우 상단 탐색
        val topCandidates = lines
            .sortedBy { it.box.top }
            .take(10)

        for (candidate in topCandidates) {

            if (isValidStore(candidate.text)) {
                return candidate.text
            }
        }

        return null
    }

    // 가게 이름으로 적합한지 확인
    private fun isValidStore(value: String): Boolean {

        val name = value.trim()

        if (name.length !in 2..35) {
            return false
        }

        if (!name.any { it.isLetter() }) {
            return false
        }

        // 카드사, 주소, 안내 문구 제외
        if (storeExcluded.any {
                name.contains(it, ignoreCase = true)
            }) {
            return false
        }

        // 날짜가 포함된 문자열 제외
        if (name.contains(
                Regex("""\d{4}[./-]\d{1,2}[./-]\d{1,2}""")
            )) {
            return false
        }

        // 숫자가 너무 많은 문자열 제외
        if (name.count { it.isDigit() } > 4) {
            return false
        }

        // 안내 문장 제외
        val sentenceWords = listOf(
            "합니다",
            "하세요",
            "바랍니다",
            "있습니다",
            "됩니다",
            "주세요",
            "실제",
            "상이",
            "문의"
        )

        if (sentenceWords.any {
                name.contains(it)
            }) {
            return false
        }

        return true
    }
}
