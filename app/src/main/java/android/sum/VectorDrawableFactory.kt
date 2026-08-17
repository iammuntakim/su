package android.sum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RoundRectShape
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import android.sum.R

object VectorDrawableFactory {

    @ColorInt private const val PRIMARY = 0xFF1A73E8.toInt()
    @ColorInt private const val PRIMARY_DARK = 0xFF1558B0.toInt()
    @ColorInt private const val SURFACE = 0xFFFFFFFF.toInt()
    @ColorInt private const val SURFACE_VARIANT = 0xFFF1F3F4.toInt()
    @ColorInt private const val ON_PRIMARY = 0xFFFFFFFF.toInt()
    @ColorInt private const val ERROR = 0xFFD93025.toInt()
    @ColorInt private const val SUCCESS = 0xFF1E8E3E.toInt()
    @ColorInt private const val WARNING = 0xFFF9AB00.toInt()
    @ColorInt private const val TEAL = 0xFF009688.toInt()
    @ColorInt private const val PURPLE = 0xFF7C4DFF.toInt()
    @ColorInt private const val AMBER = 0xFFFFA000.toInt()

    fun shieldIcon(context: Context, @ColorInt tint: Int = ON_PRIMARY): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.FILL
            }
            private val path = Path()

            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                val cx = w / 2f
                val topPad = h * 0.08f
                val botPad = h * 0.12f

                path.reset()
                path.moveTo(cx, topPad)
                path.lineTo(w * 0.82f, h * 0.28f)
                path.lineTo(w * 0.82f, h * 0.58f)
                path.quadTo(w * 0.82f, h * 0.82f, cx, h - botPad)
                path.quadTo(w * 0.18f, h * 0.82f, w * 0.18f, h * 0.58f)
                path.lineTo(w * 0.18f, h * 0.28f)
                path.close()

                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            override fun getOpacity() = PixelFormat.TRANSLUCENT
        }
    }

    fun extensionIcon(context: Context, @ColorInt tint: Int = ON_PRIMARY): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.FILL
            }
            private val path = Path()

            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                val s = minOf(w, h) * 0.38f
                val cx = w / 2f
                val cy = h / 2f
                val r = s / 2f
                val notch = s * 0.15f

                path.reset()
                path.moveTo(cx - r + notch, cy - r)
                path.lineTo(cx + r - notch, cy - r)
                path.lineTo(cx + r, cy - r + notch)
                path.lineTo(cx + r, cy + r - notch)
                path.lineTo(cx + r - notch, cy + r)
                path.lineTo(cx - r + notch, cy + r)
                path.lineTo(cx - r, cy + r - notch)
                path.lineTo(cx - r, cy - r + notch)
                path.close()

                canvas.drawPath(path, fillPaint)

                val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                val is = s * 0.45f
                val ir = is / 2f
                canvas.drawRoundRect(
                    cx - ir, cy - ir * 0.6f, cx + ir, cy + ir * 0.6f,
                    ir * 0.15f, ir * 0.15f, inner
                )
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha; fillPaint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf; fillPaint.colorFilter = cf }
            override fun getOpacity() = PixelFormat.TRANSLUCENT
        }
    }

    fun superuserIcon(context: Context, @ColorInt tint: Int = ON_PRIMARY): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                textSize = 0f
                textAlign = Paint.Align.CENTER
            }

            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                paint.textSize = minOf(w, h) * 0.55f
                paint.textAlign = Paint.Align.CENTER
                val fm = paint.fontMetrics
                val x = w / 2f
                val y = h / 2f - (fm.ascent + fm.descent) / 2f
                canvas.drawText("#", x, y, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            override fun getOpacity() = PixelFormat.TRANSLUCENT
        }
    }

    fun settingsGear(context: Context, @ColorInt tint: Int = ON_PRIMARY): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.FILL
            }
            private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.TRANSPARENT
                style = Paint.Style.FILL
            }

            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                val cx = w / 2f
                val cy = h / 2f
                val outerR = minOf(w, h) * 0.42f
                val innerR = outerR * 0.55f
                val teeth = 8
                val toothAngle = (2 * Math.PI / teeth).toFloat()
                val toothDepth = outerR * 0.22f

                val path = Path()
                for (i in 0 until teeth) {
                    val angle = i * toothAngle - Math.PI.toFloat() / 2f
                    val cos1 = kotlin.math.cos(angle - toothAngle * 0.15f)
                    val sin1 = kotlin.math.sin(angle - toothAngle * 0.15f)
                    val cos2 = kotlin.math.cos(angle + toothAngle * 0.15f)
                    val sin2 = kotlin.math.sin(angle + toothAngle * 0.15f)
                    val cosM = kotlin.math.cos(angle)
                    val sinM = kotlin.math.sin(angle)

                    if (i == 0) {
                        path.moveTo(cx + cos1 * outerR, cy + sin1 * outerR)
                    }
                    path.lineTo(cx + cosM * (outerR + toothDepth), cy + sinM * (outerR + toothDepth))
                    path.lineTo(cx + cos2 * outerR, cy + sin2 * outerR)

                    val nextAngle = (i + 1) * toothAngle - Math.PI.toFloat() / 2f
                    val cosN = kotlin.math.cos(nextAngle - toothAngle * 0.15f)
                    val sinN = kotlin.math.sin(nextAngle - toothAngle * 0.15f)
                    path.arcTo(
                        RectF(cx - outerR, cy - outerR, cx + outerR, cy + outerR),
                        Math.toDegrees((angle + toothAngle * 0.15f).toDouble()).toFloat(),
                        Math.toDegrees(((nextAngle - toothAngle * 0.15f) - (angle + toothAngle * 0.15f)).toDouble()).toFloat(),
                        false
                    )
                }
                path.close()
                canvas.drawPath(path, paint)

                canvas.drawCircle(cx, cy, innerR, clearPaint)
                canvas.drawCircle(cx, cy, innerR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = paint.color
                    style = Paint.Style.STROKE
                    strokeWidth = minOf(w, h) * 0.06f
                })
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            override fun getOpacity() = PixelFormat.TRANSLUCENT
        }
    }

    fun circleDrawable(@ColorInt color: Int, sizeDp: Int = 40): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(sizeDp * 3, sizeDp * 3)
            setColor(color)
        }
    }

    fun roundedRect(@ColorInt color: Int, radiusDp: Int = 16): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radiusDp * 3f
        }
    }

    fun rippleOverlay(content: Drawable, @ColorInt rippleColor: Int = 0x20000000): RippleDrawable {
        val mask = ShapeDrawable(OvalShape())
        return RippleDrawable(
            ContextCompat.getColorStateList(
                Context(content.context),
                android.R.color.black
            ).takeIf { false } ?: android.content.res.ColorStateList.valueOf(rippleColor),
            content,
            mask
        )
    }

    fun statusBadge(@ColorInt color: Int): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    fun cardBackground(context: Context, @ColorInt color: Int = SURFACE): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 28f
        }
    }

    fun chipBackground(context: Context, @ColorInt color: Int = SURFACE_VARIANT): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 40f
            setStroke(2 * context.resources.displayMetrics.density.toInt(), SURFACE_VARIANT)
        }
    }

    fun dividerDrawable(): ShapeDrawable {
        return ShapeDrawable().apply {
            intrinsicHeight = 1
            paint.color = 0xFFDADCE0.toInt()
        }
    }

    fun navBarBackground(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(SURFACE)
            cornerRadius = 28f
        }
    }

    fun icLogo(context: Context): Drawable {
        return LayerDrawable(arrayOf(
            circleDrawable(TEAL, 48),
            object : Drawable() {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 0f
                }
                override fun draw(canvas: Canvas) {
                    val s = minOf(bounds.width(), bounds.height())
                    paint.textSize = s * 0.42f
                    paint.textAlign = Paint.Align.CENTER
                    val fm = paint.fontMetrics
                    canvas.drawText(
                        "S",
                        bounds.width() / 2f,
                        bounds.height() / 2f - (fm.ascent + fm.descent) / 2f,
                        paint
                    )
                }
                override fun setAlpha(alpha: Int) { paint.alpha = alpha }
                override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
                override fun getOpacity() = PixelFormat.TRANSLUCENT
            }
        ))
    }

    fun notificationIcon(): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
            }
            private val path = Path()

            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                val cx = w / 2f

                path.reset()
                path.moveTo(cx - w * 0.35f, h * 0.7f)
                path.lineTo(cx - w * 0.35f, h * 0.45f)
                path.quadTo(cx - w * 0.35f, h * 0.15f, cx, h * 0.15f)
                path.quadTo(cx + w * 0.35f, h * 0.15f, cx + w * 0.35f, h * 0.45f)
                path.lineTo(cx + w * 0.35f, h * 0.7f)
                path.close()
                canvas.drawPath(path, paint)

                canvas.drawCircle(cx, h * 0.82f, w * 0.08f, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            override fun getOpacity() = PixelFormat.TRANSLUCENT
        }
    }
}
