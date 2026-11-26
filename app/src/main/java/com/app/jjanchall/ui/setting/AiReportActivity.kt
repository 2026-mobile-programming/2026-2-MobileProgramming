package com.app.jjanchall.ui.setting

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.app.jjanchall.R
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.ActivityAiReportBinding
import com.app.jjanchall.util.applySystemBarsPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AiReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiReportBinding
    private var selectedMonth = YearMonth.now()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid
    private var reportOwner: String? = null
    private var reportMonth: YearMonth? = null
    private var reportJob: Job? = null
    private var monthPickerDialog: AlertDialog? = null
    private var requestVersion = 0L
    private val reportCache by lazy {
        getSharedPreferences("ai_report_cache", MODE_PRIVATE)
    }

    private val model by lazy {
        Firebase.ai(app = getReportFirebaseApp(applicationContext)).generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(mapOf(
                    "summary" to Schema.string(),
                    "tips" to Schema.array(Schema.string(), minItems = 3, maxItems = 3)
                ))
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAiReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()
        binding.appBar.tvAppBarTitle.setText(R.string.setting_ai_report)
        binding.appBar.btnBack.setOnClickListener { finish() }

        val currentMonth = YearMonth.now()
        selectedMonth = savedInstanceState?.getString("selectedMonth")?.let {
            runCatching { YearMonth.parse(it) }.getOrNull()
        }?.takeIf { it.year >= 1 && !it.isAfter(currentMonth) } ?: currentMonth
        updateMonthLabel()
        binding.tvReportMonth.setOnClickListener { showMonthPicker() }
        binding.btnRegenerate.setOnClickListener { regenerate(force = true) }

        val previous = savedInstanceState?.takeIf {
            uid != null && it.getString("owner") == uid &&
                it.getString("month") == selectedMonth.toString() &&
                it.getString("summary") != null
        }
        if (previous != null) {
            reportOwner = uid
            reportMonth = selectedMonth
            showReport(
                previous.getString("summary").orEmpty(),
                previous.getStringArrayList("tips").orEmpty()
            )
        }

        regenerate()
    }

    private fun updateMonthLabel() {
        binding.tvReportMonth.text = getString(
            R.string.ai_report_month, selectedMonth.year, selectedMonth.monthValue
        )
        binding.tvReportMonth.contentDescription = getString(
            R.string.ai_report_month_action, selectedMonth.year, selectedMonth.monthValue
        )
    }

    private fun showMonthPicker() {
        if (monthPickerDialog?.isShowing == true) return
        val currentMonth = YearMonth.now()
        val initialMonth = if (selectedMonth.isAfter(currentMonth)) currentMonth else selectedMonth
        val yearPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = currentMonth.year
            value = initialMonth.year
            wrapSelectorWheel = false
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            contentDescription = getString(R.string.ai_report_year_label)
            setFormatter { getString(R.string.ai_report_year_option, it) }
        }
        val monthPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = if (initialMonth.year == currentMonth.year) currentMonth.monthValue else 12
            value = initialMonth.monthValue
            wrapSelectorWheel = false
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            contentDescription = getString(R.string.ai_report_month_label)
            setFormatter { getString(R.string.ai_report_month_option, it) }
        }
        yearPicker.setOnValueChangedListener { _, _, year ->
            monthPicker.maxValue = if (year == currentMonth.year) currentMonth.monthValue else 12
        }

        val padding = (24 * resources.displayMetrics.density).roundToInt()
        val pickers = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(padding, 0, padding, 0)
            addView(yearPicker, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(monthPicker, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        monthPickerDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ai_report_choose_month)
            .setView(pickers)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val month = YearMonth.of(yearPicker.value, monthPicker.value)
                if (month != selectedMonth) {
                    selectedMonth = month
                    updateMonthLabel()
                    regenerate()
                }
            }
            .show()
    }

    private fun showReport(summary: String, tips: List<String>) {
        binding.tvReportSummary.text = formatWonAmounts(summary)
        binding.tipsCard.isVisible = tips.isNotEmpty()

        val rows = listOf(binding.tipOne, binding.tipTwo, binding.tipThree)
        rows.forEachIndexed { index, row ->
            row.root.isVisible = index < tips.size
            row.tipNumber.text = (index + 1).toString()
            row.tipText.text = formatWonAmounts(
                tips.getOrNull(index).orEmpty()
            )
        }
    }

    private fun formatWonAmounts(text: String): String {
        val formatter = java.text.NumberFormat.getIntegerInstance(
            java.util.Locale.KOREA
        )

        return Regex("""(?<![\d.,])(\d[\d,]*)\s*원""").replace(text) { match ->
            val amount = match.groupValues[1]
                .replace(",", "")
                .toLongOrNull()

            if (amount != null) {
                "${formatter.format(amount)}원"
            } else {
                match.value
            }
        }
    }

    private fun readCachedReport(
        owner: String,
        month: YearMonth,
        dataKey: String
    ): Pair<String, List<String>>? {
        return runCatching {
            val saved = reportCache.getString(
                "report_v1_${owner}_$month", null
            ) ?: return@runCatching null

            val data = JSONObject(saved)

            if (data.getString("dataKey") != dataKey) {
                return@runCatching null
            }

            val summary = data.getString("summary").trim()
            val tipsArray = data.getJSONArray("tips")
            require(tipsArray.length() == 3)

            val tips = (0 until tipsArray.length()).map {
                tipsArray.getString(it).trim()
            }
            require(summary.isNotEmpty() && tips.all { it.isNotEmpty() })

            summary to tips
        }.getOrNull()
    }

    private fun saveCachedReport(
        owner: String,
        month: YearMonth,
        dataKey: String,
        summary: String,
        tips: List<String>
    ) {
        val data = JSONObject()
            .put("dataKey", dataKey)
            .put("summary", summary)
            .put("tips", JSONArray(tips))

        reportCache.edit()
            .putString("report_v1_${owner}_$month", data.toString())
            .apply()
    }
    private fun isCurrentRequest(version: Long, month: YearMonth): Boolean =
        version == requestVersion && month == selectedMonth

    private fun setGenerating(generating: Boolean) {
        binding.btnRegenerate.isEnabled = !generating
        binding.btnRegenerate.setText(
            if (generating) R.string.ai_report_busy else R.string.ai_report_retry
        )
    }

    private fun regenerate(force: Boolean = false) {
        val version = ++requestVersion
        reportJob?.cancel()
        reportJob = null
        val owner = uid
        val month = selectedMonth

        if (owner == null) {
            reportOwner = null
            reportMonth = null
            showReport(getString(R.string.ai_report_login), emptyList())
            setGenerating(false)
            return
        }

        var hasPreviousReport = reportOwner == owner && reportMonth == month &&
                binding.tipsCard.isVisible
        if (!hasPreviousReport) {
            reportOwner = null
            reportMonth = null
            showReport(getString(R.string.ai_report_loading), emptyList())
        }
        setGenerating(true)

        reportJob = lifecycleScope.launch {
            try {
                val totals = withContext(Dispatchers.IO) {
                    ExpenseDbHelper(applicationContext).use { db ->
                        db.getExpensesForMonth(month.toString()).groupBy { it.category }
                            .mapValues { (_, expenses) -> expenses.sumOf { it.amount.toLong() } }
                    }
                }
                if (!isCurrentRequest(version, month) || uid != owner) return@launch
                val total = totals.values.sum()
                if (total == 0L) {
                    reportOwner = owner
                    reportMonth = month
                    showReport(getString(R.string.ai_report_empty), emptyList())
                    return@launch
                }

                val dataKey = JSONArray().apply {
                    for ((category, amount) in totals.toSortedMap()) {
                        put(JSONArray().put(category).put(amount))
                    }
                }.toString()

                val cached = withContext(Dispatchers.IO) {
                    readCachedReport(owner, month, dataKey)
                }

                if (!isCurrentRequest(version, month) || uid != owner) {
                    return@launch
                }

                if (cached == null && hasPreviousReport) {
                    hasPreviousReport = false
                    reportOwner = null
                    reportMonth = null
                    showReport(getString(R.string.ai_report_loading), emptyList())
                }

                if (!force && cached != null) {
                    reportOwner = owner
                    reportMonth = month
                    showReport(cached.first, cached.second)
                    return@launch
                }

                val categories = JSONArray()
                for ((category, amount) in totals.entries.sortedByDescending { it.value }) {
                    categories.put(
                        JSONObject().put("category", category).put("won", amount)
                            .put("percent", (100.0 * amount / total).roundToInt())
                    )
                }
                val prompt = """
                    자취생을 위한 월간 가계부 리포트를 한국어 존댓말로 써 주세요.
                    대상 월은 ${month.year}년 ${month.monthValue}월이며, 총 지출은 ${total}원입니다.
                    아래 JSON은 해당 월의 카테고리별 합계(원)와 반올림한 비율입니다.
                    실제 수치를 근거로 두 문장 이내 요약과 서로 다른 실천 가능한 절약 팁 3개를 작성하세요.
                    요약에서는 '이번 달' 대신 대상 연도와 월 또는 '선택한 달'이라고 표현하세요.
                    각 팁은 한 문장으로, 번호와 머리말은 생략하세요.
                    결제 시간, 가게, 구매 횟수, 지난달과의 변화는 추측하지 마세요.
                    데이터: $categories
                """.trimIndent()
                val result = JSONObject(requireNotNull(model.generateContent(prompt).text))
                val summary = result.getString("summary").trim()
                val tipsArray = result.getJSONArray("tips")
                require(tipsArray.length() == 3)
                val tips = (0 until tipsArray.length()).map { tipsArray.getString(it).trim() }
                require(summary.isNotEmpty() && tips.all { it.isNotEmpty() })

                if (isCurrentRequest(version, month) && uid == owner) {
                    runCatching {
                        saveCachedReport(owner, month, dataKey, summary, tips)
                    }

                    reportOwner = owner
                    reportMonth = month
                    showReport(summary, tips)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (isCurrentRequest(version, month) && uid == owner) {
                    if (hasPreviousReport) {
                        Snackbar.make(binding.root, R.string.ai_report_error, Snackbar.LENGTH_LONG).show()
                    } else {
                        reportOwner = null
                        reportMonth = null
                        showReport(getString(R.string.ai_report_error), emptyList())
                    }
                }
            } finally {
                // 이전 달의 요청이 취소되어도 새 요청의 화면과 버튼 상태는 바꾸지 않습니다.
                if (isCurrentRequest(version, month)) {
                    if (uid != owner) {
                        reportOwner = null
                        reportMonth = null
                        showReport(getString(R.string.ai_report_login), emptyList())
                    }
                    reportJob = null
                    setGenerating(false)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selectedMonth", selectedMonth.toString())
        val owner = reportOwner ?: return
        if (owner != uid || reportMonth != selectedMonth) return
        outState.putString("owner", owner)
        outState.putString("month", selectedMonth.toString())
        outState.putString("summary", binding.tvReportSummary.text.toString())
        val rows = listOf(binding.tipOne, binding.tipTwo, binding.tipThree)
        outState.putStringArrayList(
            "tips", ArrayList(rows.filter { it.root.isVisible }.map { it.tipText.text.toString() })
        )
    }

    override fun onDestroy() {
        requestVersion++
        reportJob?.cancel()
        monthPickerDialog?.dismiss()
        super.onDestroy()
    }
}

internal fun getReportFirebaseApp(context: android.content.Context): com.google.firebase.FirebaseApp {
    val appName = "ai-report"
    val existingApp = com.google.firebase.FirebaseApp.getApps(context)
        .firstOrNull { it.name == appName }

    if (existingApp != null) return existingApp

    val options = com.google.firebase.FirebaseOptions.Builder()
        .setProjectId("gen-lang-client-0221506538")
        .setApplicationId("1:873545723575:android:fd286dafa931e80d74e732")
        .setApiKey("AIzaSyBK_IrfQ9N_DalPk5swC4_9Yhi7zi_hh90")
        .build()

    return com.google.firebase.FirebaseApp.initializeApp(context, options, appName)
}
