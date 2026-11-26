package com.app.jjanchall.ui.setting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.app.jjanchall.R
import com.app.jjanchall.data.local.ExpenseDbHelper
import com.app.jjanchall.databinding.ActivityAiReportBinding
import com.app.jjanchall.util.applySystemBarsPadding
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AiReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiReportBinding
    private val month = YearMonth.now()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid
    private var reportOwner: String? = null

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
        binding.tvReportMonth.text = getString(R.string.ai_report_month, month.year, month.monthValue)

        val previous = savedInstanceState?.takeIf {
            uid != null && it.getString("owner") == uid && it.getString("month") == month.toString()
        }
        if (previous != null) {
            reportOwner = uid
            showReport(previous.getString("summary").orEmpty(), previous.getStringArrayList("tips").orEmpty())
        } else {
            regenerate()
        }
        binding.btnRegenerate.setOnClickListener { regenerate() }
    }

    private fun showReport(summary: String, tips: List<String>) {
        binding.tvReportSummary.text = summary
        binding.tipsCard.isVisible = tips.isNotEmpty()
        val rows = listOf(binding.tipOne, binding.tipTwo, binding.tipThree)
        rows.forEachIndexed { index, row ->
            row.root.isVisible = index < tips.size
            row.tipNumber.text = (index + 1).toString()
            row.tipText.text = tips.getOrNull(index).orEmpty()
        }
    }

    private fun regenerate() {
        val owner = uid
        if (owner == null) {
            reportOwner = null
            showReport(getString(R.string.ai_report_login), emptyList())
            return
        }
        if (!binding.btnRegenerate.isEnabled) return
        if (reportOwner != owner || !binding.tipsCard.isVisible) {
            reportOwner = null
            showReport(getString(R.string.ai_report_loading), emptyList())
        }
        binding.btnRegenerate.isEnabled = false
        binding.btnRegenerate.setText(R.string.ai_report_busy)
        lifecycleScope.launch {
            try {
                val totals = withContext(Dispatchers.IO) {
                    ExpenseDbHelper(applicationContext).use { db ->
                        db.getExpensesForMonth(month.toString()).groupBy { it.category }
                            .mapValues { (_, expenses) -> expenses.sumOf { it.amount.toLong() } }
                    }
                }
                if (uid != owner) return@launch
                val total = totals.values.sum()
                if (total == 0L) {
                    reportOwner = owner
                    showReport(getString(R.string.ai_report_empty), emptyList())
                    return@launch
                }
                val categories = JSONArray()
                for ((category, amount) in totals.entries.sortedByDescending { it.value }) {
                    categories.put(JSONObject().put("category", category).put("won", amount)
                        .put("percent", (100.0 * amount / total).roundToInt()))
                }
                val prompt = """
                    자취생을 위한 월간 가계부 리포트를 한국어 존댓말로 써 주세요.
                    아래 JSON은 카테고리별 월 합계(원)와 반올림한 비율입니다.
                    실제 수치를 근거로 두 문장 이내 요약과 서로 다른 실천 가능한 절약 팁 3개를 작성하세요.
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
                if (uid == owner) {
                    reportOwner = owner
                    showReport(summary, tips)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (uid == owner && reportOwner == owner && binding.tipsCard.isVisible) {
                    Snackbar.make(binding.root, R.string.ai_report_error, Snackbar.LENGTH_LONG).show()
                } else if (uid == owner) {
                    reportOwner = null
                    showReport("${error.javaClass.simpleName}: ${error.message}", emptyList())
                }
            } finally {
                if (uid != owner) {
                    reportOwner = null
                    showReport(getString(R.string.ai_report_login), emptyList())
                }
                binding.btnRegenerate.setText(R.string.ai_report_retry)
                binding.btnRegenerate.isEnabled = true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val owner = reportOwner ?: return
        outState.putString("owner", owner)
        outState.putString("month", month.toString())
        outState.putString("summary", binding.tvReportSummary.text.toString())
        val rows = listOf(binding.tipOne, binding.tipTwo, binding.tipThree)
        outState.putStringArrayList("tips", ArrayList(rows.filter { it.root.isVisible }.map { it.tipText.text.toString() }))
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