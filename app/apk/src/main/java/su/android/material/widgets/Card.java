package su.android.material.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import su.android.R;

/**
 * An iOS styled card built on {@link CardView}.
 *
 * <p>Renders crisp rounded corners with an optional stroke, a material
 * ripple foreground clipped to the rounded shape, and animatable corner
 * radius for buttery transitions.</p>
 */
public class Card extends CardView {

    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mStrokeRect = new RectF();

    private float mStrokeWidth = 0f;
    @ColorInt private int mStrokeColor = 0;
    private ColorStateList mRippleColor;

    public Card(@NonNull Context context) {
        this(context, null);
    }

    public Card(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, androidx.cardview.R.attr.cardViewStyle);
    }

    public Card(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setAntiAlias(true);

        final TypedArray a = context.obtainStyledAttributes(attrs, new int[]{
                R.attr.strokeWidth, R.attr.strokeColor});
        mStrokeWidth = a.getDimensionPixelSize(0, 0);
        mStrokeColor = a.getColor(1, 0);
        a.recycle();

        mRippleColor = ColorStateList.valueOf(resolveControlHighlight(context));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setForeground(createRipple());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mStrokeWidth > 0f && mStrokeColor != 0) {
            final float radius = getRadius();
            final float inset = mStrokeWidth / 2f;
            mStrokeRect.set(inset, inset, getWidth() - inset, getHeight() - inset);
            mStrokePaint.setStrokeWidth(mStrokeWidth);
            mStrokePaint.setColor(mStrokeColor);
            final float corner = Math.max(0f, radius - inset);
            canvas.drawRoundRect(mStrokeRect, corner, corner, mStrokePaint);
        }
    }

    @Override
    public void setRadius(float radius) {
        super.setRadius(radius);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mRippleColor != null) {
            setForeground(createRipple());
        }
    }

    /**
     * Sets the width of the card outline. A value of {@code 0} hides the stroke.
     */
    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
        invalidate();
    }

    /**
     * Sets the color of the card outline.
     */
    public void setStrokeColor(@ColorInt int color) {
        mStrokeColor = color;
        invalidate();
    }

    private RippleDrawable createRipple() {
        final GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(getRadius());
        return new RippleDrawable(mRippleColor, null, mask);
    }

    private static int resolveControlHighlight(Context context) {
        final TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.colorControlHighlight, tv, true)) {
            return tv.data;
        }
        return 0x33000000;
    }
}
