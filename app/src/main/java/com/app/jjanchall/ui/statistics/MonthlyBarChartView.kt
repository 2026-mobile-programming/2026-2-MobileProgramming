package com.app.jjanchall.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.app.jjanchall.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/** HOME-02 월별 지출 Canvas 막대그래프 */
class MonthlyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class MonthlyItem(
        val month: String,
        val amount: Long
    )

    private var items: List<MonthlyItem> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = resources.displayMetrics.density
    }

    private val barRect = RectF()
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    fun setChartData(data: List<MonthlyItem>) {
        items = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (items.isEmpty()) {
            drawEmptyMessage(canvas)
            return
        }

        val chartLeft = 8f * density
        val chartRight = width - 8f * density
        val chartTop = 34f * density
        val chartBottom = height - 34f * density

        val chartHeight = chartBottom - chartTop

        val maxAmount = max(
            1L,
            items.maxOf { it.amount }
        )

        linePaint.color = ContextCompat.getColor(
            context,
            R.color.border_default
        )

        canvas.drawLine(
            chartLeft,
            chartBottom,
            chartRight,
            chartBottom,
            linePaint
        )

        val slotWidth = (chartRight - chartLeft) / items.size
        val barWidth = slotWidth * 0.48f

        items.forEachIndexed { index, item ->

            val centerX =
                chartLeft + slotWidth * (index + 0.5f)

            val amount = item.amount.coerceAtLeast(0L)

            val barHeight =
                amount.toFloat() / maxAmount * chartHeight

            val barTop = chartBottom - barHeight

            // 선택한 월(마지막 막대)은 진한 초록색
            barPaint.color = ContextCompat.getColor(
                context,
                if (index == items.lastIndex) {
                    R.color.bg_brand
                } else {
                    R.color.bg_brand_subtle
                }
            )

            barRect.set(
                centerX - barWidth / 2f,
                barTop,
                centerX + barWidth / 2f,
                chartBottom
            )

            val cornerRadius = 6f * density

            canvas.drawRoundRect(
                barRect,
                cornerRadius,
                cornerRadius,
                barPaint
            )

            textPaint.color = ContextCompat.getColor(
                context,
                R.color.text_primary
            )
            textPaint.textSize = 10f * scaledDensity

            canvas.drawText(
                formatCompactAmount(amount),
                centerX,
                (barTop - 8f * density).coerceAtLeast(16f * density),
                textPaint
            )

            // 막대 아래 월 표시
            textPaint.color = ContextCompat.getColor(
                context,
                R.color.text_secondary
            )
            textPaint.textSize = 12f * scaledDensity

            canvas.drawText(
                item.month,
                centerX,
                chartBottom + 22f * density,
                textPaint
            )
        }
    }
    private fun formatCompactAmount(amount: Long): String {
        return if (amount >= 10000L) {
            String.format(
                Locale.KOREA,
                "%.1f만",
                amount / 10000.0
            )
        } else {
            numberFormat.format(amount)
        }
    }

    private fun drawEmptyMessage(canvas: Canvas) {
        textPaint.color = ContextCompat.getColor(
            context,
            R.color.text_secondary
        )
        textPaint.textSize = 14f * scaledDensity

        canvas.drawText(
            "월별 지출 데이터가 없습니다.",
            width / 2f,
            height / 2f,
            textPaint
        )
    }
}
