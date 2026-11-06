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
import kotlin.math.min

class ExpenseDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class ChartItem(
        val amount: Int,
        val color: Int
    )

    private var items: List<ChartItem> = emptyList()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val strokeWidth = resources.getDimension(
        R.dimen.statistics_chart_stroke_width
    )

    private val labelTextSize = resources.getDimension(
        R.dimen.statistics_chart_center_label_size
    )

    private val amountTextSize = resources.getDimension(
        R.dimen.statistics_chart_center_amount_size
    )

    private val labelColor = ContextCompat.getColor(
        context, R.color.text_secondary
    )

    private val amountColor = ContextCompat.getColor(
        context, R.color.text_primary
    )

    private val emptyColor = ContextCompat.getColor(
        context, R.color.bg_muted
    )

    fun setChartData(data: List<ChartItem>) {
        items = data.filter { it.amount > 0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - strokeWidth / 2f

        if (radius <= 0f) return

        val bounds = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        paint.apply {
            style = Paint.Style.STROKE
            strokeWidth = this@ExpenseDonutChartView.strokeWidth
            strokeCap = Paint.Cap.BUTT
        }

        val total = items.sumOf { it.amount.toLong() }

        if (total == 0L) {
            paint.color = emptyColor

            canvas.drawArc(
                bounds,
                0f,
                360f,
                false,
                paint
            )
        } else {
            var startAngle = -90f

            items.forEachIndexed { index, item ->
                val sweepAngle = if (index == items.lastIndex) {
                    270f - startAngle
                } else {
                    item.amount.toFloat() / total.toFloat() * 360f
                }

                paint.color = item.color

                canvas.drawArc(
                    bounds,
                    startAngle,
                    sweepAngle,
                    false,
                    paint
                )

                startAngle += sweepAngle
            }
        }

        paint.apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
            color = labelColor
            textSize = labelTextSize
        }

        val labelBaseline = centerY -
                resources.getDimension(R.dimen.spacing_sm)

        canvas.drawText(
            "총 지출",
            centerX,
            labelBaseline,
            paint
        )

        paint.apply {
            color = amountColor
            textSize = amountTextSize
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val amountBaseline = centerY +
                resources.getDimension(R.dimen.spacing_xl)

        val formattedAmount = NumberFormat.getNumberInstance(
            Locale.KOREA
        ).format(total)

        canvas.drawText(
            "${formattedAmount}원",
            centerX,
            amountBaseline,
            paint
        )
    }
}
