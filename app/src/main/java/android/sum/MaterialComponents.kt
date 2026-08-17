package android.sum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import android.sum.VectorDrawableFactory

object MaterialComponents {

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun sp(context: Context, value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics)

    fun statusCard(
        context: Context,
        title: String,
        subtitle: String,
        @ColorInt accentColor: Int,
        icon: Drawable? = null
    ): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = dp(context, 20).toFloat()
            cardElevation = 2f * context.resources.displayMetrics.density
            setContentPadding(0, 0, 0, 0)
            useCompatPadding = true
            preventCornerOverlap = false
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = dp(context, 20).toFloat()
            }
            foreground = bg
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 20), dp(context, 18), dp(context, 20), dp(context, 18))
        }

        val iconView = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 48), dp(context, 48))
        }

        val iconBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(accentColor)
        }
        ViewCompat.setBackground(iconView, iconBg)

        if (icon != null) {
            val iv = ShapeableImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(context, 24), dp(context, 24)).apply {
                    gravity = Gravity.CENTER
                }
                setImageDrawable(icon)
                colorFilter = android.graphics.PorterDuffColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
            }
            iconView.addView(iv)
        }

        val textGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(context, 16)
            }
        }

        val titleTv = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#202124"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            typeface = android.graphics.Typeface.create(context, android.graphics.Typeface.BOLD)
        }

        val subtitleTv = TextView(context).apply {
            text = subtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#5F6368"))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, 2) }
        }

        textGroup.addView(titleTv)
        textGroup.addView(subtitleTv)

        val badge = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(accentColor)
            setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(context, 8) }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(accentColor and 0x15FFFFFF or 0x0A000000)
                cornerRadius = dp(context, 12).toFloat()
            }
            isVisible = false
        }

        container.addView(iconView)
        container.addView(textGroup)
        container.addView(badge)

        card.addView(container)

        card.tag = mapOf("title" to titleTv, "subtitle" to subtitleTv, "badge" to badge)
        return card
    }

    fun sectionHeader(context: Context, title: String): TextView {
        return TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#5F6368"))
            typeface = android.graphics.Typeface.create(context, android.graphics.Typeface.BOLD)
            letterSpacing = 0.06f
            setPadding(dp(context, 4), dp(context, 16), dp(context, 4), dp(context, 8))
        }
    }

    fun settingItem(
        context: Context,
        title: String,
        subtitle: String? = null,
        icon: Drawable? = null,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(context, 56)
            setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))

            if (onClick != null) {
                isClickable = true
                isFocusable = true
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { onClick() }
            }
        }

        if (icon != null) {
            val iv = ShapeableImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 24), dp(context, 24)).apply {
                    marginEnd = dp(context, 16)
                }
                setImageDrawable(icon)
                colorFilter = android.graphics.PorterDuffColorFilter(
                    Color.parseColor("#5F6368"),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            container.addView(iv)
        }

        val textGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTv = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#202124"))
        }
        textGroup.addView(titleTv)

        if (subtitle != null) {
            val subTv = TextView(context).apply {
                text = subtitle
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#5F6368"))
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(context, 2) }
            }
            textGroup.addView(subTv)
        }

        container.addView(textGroup)
        return container
    }

    fun switchItem(
        context: Context,
        title: String,
        subtitle: String? = null,
        checked: Boolean = false,
        onCheckedChanged: ((Boolean) -> Unit)? = null
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(context, 56)
            setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12))
        }

        val textGroup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTv = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#202124"))
        }
        textGroup.addView(titleTv)

        if (subtitle != null) {
            val subTv = TextView(context).apply {
                text = subtitle
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#5F6368"))
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(context, 2) }
            }
            textGroup.addView(subTv)
        }

        container.addView(textGroup)

        val switch = com.google.android.material.switchmaterial.SwitchMaterial(context).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> onCheckedChanged?.invoke(isChecked) }
        }
        container.addView(switch)

        return container
    }

    fun infoRow(context: Context, label: String, value: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8))

            addView(TextView(context).apply {
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.parseColor("#5F6368"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(context).apply {
                text = value
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.parseColor("#202124"))
                typeface = android.graphics.Typeface.create(context, android.graphics.Typeface.BOLD)
            })
        }
    }

    fun divider(context: Context): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setPadding(dp(context, 16), 0, dp(context, 16), 0)
            }
            addView(object : android.view.View(context) {
                override fun onDraw(canvas: Canvas) {
                    canvas.drawLine(
                        paddingLeft.toFloat(), 0f,
                        (width - paddingRight).toFloat(), 0f,
                        Paint().apply {
                            color = Color.parseColor("#E0E0E0")
                            strokeWidth = 1f
                        }
                    )
                }
            })
        }
    }

    fun MaterialCardView.setBadge(text: String) {
        val badge = tag as? Map<*, *> ?: return
        val badgeTv = badge["badge"] as? TextView ?: return
        badgeTv.text = text
        badgeTv.isVisible = true
    }
}
