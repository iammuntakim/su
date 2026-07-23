package androidx.recyclerview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class RecyclerView extends android.view.ViewGroup {

    private final Path clipPath = new Path();
    private final Path borderPath = new Path();
    private final Path shadowPath = new Path();
    private final RectF rectBounds = new RectF();
    private final RectF tempRect = new RectF();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float cornerRadius = 64f;
    private float borderWidth = 0f;
    private int borderColor = Color.TRANSPARENT;
    private float shadowRadius = 0f;
    private int shadowColor = Color.argb(60, 0, 0, 0);
    private float shadowDx = 0f;
    private float shadowDy = 0f;

    private boolean isClipToPaddingEnabled = true;
    private boolean isCustomDrawingEnabled = true;

    private int customBackgroundColor = Color.WHITE;
    private int parentBackgroundColor = Color.parseColor("#E0E0E0");

    public RecyclerView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setBackgroundColor(customBackgroundColor);
        
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(borderWidth);

        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(customBackgroundColor);

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(shadowColor);

        setWillNotDraw(false);
        applyParentBackground();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyParentBackground();
    }

    private void applyParentBackground() {
        ViewParent parent = getParent();
        if (parent instanceof View) {
            ((View) parent).setBackgroundColor(parentBackgroundColor);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int parentLeft = getPaddingLeft();
        int parentTop = getPaddingTop();

        int currentTop = parentTop;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();

                child.layout(parentLeft, currentTop, parentLeft + childWidth, currentTop + childHeight);
                currentTop += childHeight;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        int maxWidth = 0;
        int totalHeight = 0;
        int childState = 0;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                totalHeight += child.getMeasuredHeight();
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
        }

        maxWidth += getPaddingLeft() + getPaddingRight();
        totalHeight += getPaddingTop() + getPaddingBottom();

        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        totalHeight = Math.max(totalHeight, getSuggestedMinimumHeight());

        setMeasuredDimension(
                resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(totalHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT)
        );
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePaths(w, h);
    }

    private void updatePaths(int w, int h) {
        rectBounds.set(0, 0, w, h);
        
        clipPath.reset();
        clipPath.addRoundRect(rectBounds, cornerRadius, cornerRadius, Path.Direction.CW);

        if (borderWidth > 0) {
            float inset = borderWidth / 2f;
            tempRect.set(inset, inset, w - inset, h - inset);
            borderPath.reset();
            borderPath.addRoundRect(tempRect, Math.max(0, cornerRadius - inset), Math.max(0, cornerRadius - inset), Path.Direction.CW);
        }

        if (shadowRadius > 0) {
            shadowPath.reset();
            tempRect.set(rectBounds);
            tempRect.offset(shadowDx, shadowDy);
            shadowPath.addRoundRect(tempRect, cornerRadius, cornerRadius, Path.Direction.CW);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isCustomDrawingEnabled) {
            super.draw(canvas);
            return;
        }

        if (shadowRadius > 0) {
            drawShadow(canvas);
        }

        int count = canvas.save();
        canvas.clipPath(clipPath);

        canvas.drawPath(clipPath, backgroundPaint);

        super.draw(canvas);

        if (borderWidth > 0 && borderColor != Color.TRANSPARENT) {
            canvas.drawPath(borderPath, borderPaint);
        }

        canvas.restoreToCount(count);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int count = canvas.save();
        if (isClipToPaddingEnabled) {
            canvas.clipRect(
                    getPaddingLeft(),
                    getPaddingTop(),
                    getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom()
            );
        }
        boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(count);
        return result;
    }

    private void drawShadow(Canvas canvas) {
        shadowPaint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
        canvas.drawPath(shadowPath, shadowPaint);
    }

    public void setCornerRadius(float radius) {
        if (this.cornerRadius != radius) {
            this.cornerRadius = radius;
            updatePaths(getWidth(), getHeight());
            invalidate();
        }
    }

    public float getCornerRadius() {
        return this.cornerRadius;
    }

    public void setBorderWidth(float width) {
        if (this.borderWidth != width) {
            this.borderWidth = width;
            borderPaint.setStrokeWidth(width);
            updatePaths(getWidth(), getHeight());
            invalidate();
        }
    }

    public float getBorderWidth() {
        return this.borderWidth;
    }

    public void setBorderColor(int color) {
        if (this.borderColor != color) {
            this.borderColor = color;
            borderPaint.setColor(color);
            invalidate();
        }
    }

    public int getBorderColor() {
        return this.borderColor;
    }

    public void setShadowConfiguration(float radius, float dx, float dy, int color) {
        this.shadowRadius = radius;
        this.shadowDx = dx;
        this.shadowDy = dy;
        this.shadowColor = color;
        updatePaths(getWidth(), getHeight());
        invalidate();
    }

    public void setCustomBackgroundColor(int color) {
        this.customBackgroundColor = color;
        backgroundPaint.setColor(color);
        invalidate();
    }

    public int getCustomBackgroundColor() {
        return this.customBackgroundColor;
    }

    public void setParentBackgroundColor(int color) {
        this.parentBackgroundColor = color;
        applyParentBackground();
    }

    public int getParentBackgroundColor() {
        return this.parentBackgroundColor;
    }

    public void setCustomClipToPadding(boolean enabled) {
        this.isClipToPaddingEnabled = enabled;
        invalidate();
    }

    public boolean isCustomClipToPadding() {
        return this.isClipToPaddingEnabled;
    }

    public void setCustomDrawingEnabled(boolean enabled) {
        this.isCustomDrawingEnabled = enabled;
        invalidate();
    }

    public boolean isCustomDrawingEnabled() {
        return this.isCustomDrawingEnabled;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }
}
