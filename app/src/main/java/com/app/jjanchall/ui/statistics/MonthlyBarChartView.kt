package com.app.jjanchall.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.app.jjanchall.R
import java.util.Locale
import kotlin.math.max

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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textColor = ContextCompat.getColor(
        context, R.color.text_primary
    )

    private val secondaryTextColor = ContextCompat.getColor(
        context, R.color.text_secondary
    )

    private val lineColor = ContextCompat.getColor(
        context, R.color.border_default
    )

    private val selectedBarColor = ContextCompat.getColor(
        context, R.color.bg_brand
    )

    private val normalBarColor = ContextCompat.getColor(
        context, R.color.bg_brand_subtle
    )

    private val horizontalPadding = resources.getDimension(
        R.dimen.statistics_chart_inner_padding
    )

    private val verticalPadding = resources.getDimension(
        R.dimen.statistics_chart_vertical_padding
    )

    private val barRadius = resources.getDimension(
        R.dimen.statistics_chart_bar_radius
    )

    private val labelTextSize = resources.getDimension(
        R.dimen.statistics_chart_label_text_size
    )

    private val valueTextSize = resources.getDimension(
        R.dimen.statistics_chart_value_text_size
    )

    private val lineWidth = resources.getDimension(
        R.dimen.statistics_divider_height
    )

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

        val chartLeft = horizontalPadding
        val chartRight = width.toFloat() - horizontalPadding
        val chartTop = verticalPadding
        val chartBottom = height.toFloat() - verticalPadding

        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0f || chartHeight <= 0f) return

        paint.apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = lineWidth
        }

        canvas.drawLine(
            chartLeft,
            chartBottom,
            chartRight,
            chartBottom,
            paint
        )

        val maxAmount = max(1L, items.maxOf { it.amount })
        val slotWidth = chartWidth / items.size
        val barWidth = slotWidth * 0.48f

        items.forEachIndexed { index, item ->
            val centerX = chartLeft + slotWidth * (index + 0.5f)

            val barHeight = chartHeight *
                    (item.amount.toFloat() / maxAmount.toFloat())

            if (item.amount > 0L) {
                val barTop = chartBottom - barHeight

                paint.apply {
                    style = Paint.Style.FILL
                    color = if (index == items.lastIndex) {
                        selectedBarColor
                    } else {
                        normalBarColor
                    }
                }

                canvas.drawRoundRect(
                    RectF(
                        centerX - barWidth / 2f,
                        barTop,
                        centerX + barWidth / 2f,
                        chartBottom
                    ),
                    barRadius,
                    barRadius,
                    paint
                )
            }

            paint.apply {
                style = Paint.Style.FILL
                color = textColor
                textSize = valueTextSize
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT
            }

            val amountLabel = formatShortAmount(item.amount)

            val amountY = if (item.amount > 0L) {
                chartBottom - barHeight -
                        resources.getDimension(R.dimen.spacing_sm)
            } else {
                chartBottom -
                        resources.getDimension(R.dimen.spacing_md)
            }

            canvas.drawText(
                amountLabel,
                centerX,
                amountY,
                paint
            )

            paint.apply {
                color = secondaryTextColor
                textSize = labelTextSize
                textAlign = Paint.Align.CENTER
            }

            val monthY = chartBottom +
                    resources.getDimension(R.dimen.spacing_xl)

            canvas.drawText(
                item.month,
                centerX,
                monthY,
                paint
            )
        }
    }

    private fun drawEmptyMessage(canvas: Canvas) {
        paint.apply {
            style = Paint.Style.FILL
            color = secondaryTextColor
            textSize = labelTextSize
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            "월별 지출 데이터가 없습니다.",
            width / 2f,
            height / 2f,
            paint
        )
    }

    private fun formatShortAmount(amount: Long): String {
        if (amount == 0L) return "0"

        return if (amount >= 10000L) {
            val man = amount / 10000.0

            if (amount % 10000L == 0L) {
                "${amount / 10000L}만"
            } else {
                String.format(Locale.KOREA, "%.1f만", man)
            }
        } else {
            NumberFormatHelper.format(amount)
        }
    }

    private object NumberFormatHelper {
        fun format(amount: Long): String {
            return java.text.NumberFormat.getNumberInstance(
                Locale.KOREA
            ).format(amount)
        }
    }
}
