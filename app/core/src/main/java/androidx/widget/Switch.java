package androidx.widget;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.widget.CompoundButton;

public class Switch extends android.widget.Switch {

    protected Paint trackPaint;
    protected Paint thumbPaint;
    protected Paint shadowPaint;

    protected RectF trackRect;
    protected RectF thumbRect;

    protected int offColor = Color.parseColor("#E5E5EA");
    protected int onColor = Color.parseColor("#007AFF");
    protected int thumbColor = Color.WHITE;
    protected int shadowColor = Color.parseColor("#26000000");

    protected float widthScale = 0.95f;
    protected float thumbPaddingRatio = 0.06f;
    protected float shadowOffsetYRatio = 0.8f;
    protected long animationDuration = 300;
    protected float overshootTension = 0.8f;

    protected boolean isChecked = false;
    protected float progress = 0.0f;

    protected ValueAnimator animator;
    protected ArgbEvaluator argbEvaluator;
    protected OnCheckedChangeListener listener;

    protected float touchStartX;
    protected float touchStartY;
    protected float lastTouchX;
    protected boolean isDragging = false;
    protected int touchSlop;

    public Switch(Context context) {
        super(context);
        init(context);
    }

    public Switch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Switch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.FILL);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(thumbColor);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(shadowColor);

        trackRect = new RectF();
        thumbRect = new RectF();

        argbEvaluator = new ArgbEvaluator();
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        setClickable(true);
        setFocusable(true);

        setTextOn("");
        setTextOff("");
        setShowText(false);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int baseWidth = (int) (62 * getResources().getDisplayMetrics().density);
        int defaultWidth = (int) (baseWidth * widthScale);
        int defaultHeight = (int) (32 * getResources().getDisplayMetrics().density);

        int width = (widthMode == MeasureSpec.EXACTLY) ? widthSize : defaultWidth;
        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize : defaultHeight;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        trackRect.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float radius = height / 2.0f;

        int currentColor = (Integer) argbEvaluator.evaluate(progress, offColor, onColor);
        trackPaint.setColor(currentColor);

        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        float padding = height * thumbPaddingRatio;
        float thumbDiameter = height - (padding * 2);
        float startX = padding;
        float endX = width - padding - thumbDiameter;

        float currentThumbLeft = startX + (endX - startX) * progress;

        thumbRect.set(currentThumbLeft, padding, currentThumbLeft + thumbDiameter, padding + thumbDiameter);

        float thumbRadius = thumbDiameter / 2.0f;
        float cx = thumbRect.centerX();
        float cy = thumbRect.centerY();

        canvas.drawCircle(cx, cy + (padding * shadowOffsetYRatio), thumbRadius, shadowPaint);
        canvas.drawCircle(cx, cy, thumbRadius, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        float width = getWidth();
        float height = getHeight();
        float padding = height * thumbPaddingRatio;
        float thumbDiameter = height - (padding * 2);
        float totalTravel = width - (padding * 2) - thumbDiameter;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = x;
                touchStartY = y;
                lastTouchX = x;
                isDragging = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = x - touchStartX;
                float dy = y - touchStartY;

                if (!isDragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    isDragging = true;
                }

                if (isDragging) {
                    float deltaX = x - lastTouchX;
                    if (totalTravel > 0) {
                        progress += deltaX / totalTravel;
                        if (progress < 0.0f) progress = 0.0f;
                        if (progress > 1.0f) progress = 1.0f;
                    }
                    invalidate();
                }
                lastTouchX = x;
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    boolean newChecked = progress >= 0.5f;
                    animateToState(newChecked);
                } else {
                    float clickDx = Math.abs(x - touchStartX);
                    float clickDy = Math.abs(y - touchStartY);
                    if (clickDx < touchSlop && clickDy < touchSlop) {
                        toggle();
                    } else {
                        animateToState(isChecked);
                    }
                }
                isDragging = false;
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void toggle() {
        setChecked(!isChecked, true);
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        setChecked(checked, false);
    }

    public void setChecked(boolean checked, boolean animate) {
        if (this.isChecked == checked && progress == (checked ? 1.0f : 0.0f)) {
            return;
        }

        boolean changed = this.isChecked != checked;
        this.isChecked = checked;
        super.setChecked(checked);

        if (changed && listener != null) {
            listener.onCheckedChanged(this, this.isChecked);
        }

        if (animate) {
            animateToState(checked);
        } else {
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            progress = checked ? 1.0f : 0.0f;
            invalidate();
        }
    }

    protected void animateToState(boolean checked) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            super.setChecked(checked);
            if (listener != null) {
                listener.onCheckedChanged(this, this.isChecked);
            }
        }

        float targetProgress = checked ? 1.0f : 0.0f;

        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(progress, targetProgress);
        animator.setDuration(animationDuration);
        animator.setInterpolator(new OvershootInterpolator(overshootTension));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
        super.setOnCheckedChangeListener(listener);
    }

    public void setOffColor(int color) {
        this.offColor = color;
        invalidate();
    }

    public void setOnColor(int color) {
        this.onColor = color;
        invalidate();
    }

    public void setThumbColor(int color) {
        this.thumbColor = color;
        thumbPaint.setColor(color);
        invalidate();
    }

    public void setShadowColor(int color) {
        this.shadowColor = color;
        shadowPaint.setColor(color);
        invalidate();
    }

    public void setWidthScale(float scale) {
        this.widthScale = scale;
        requestLayout();
    }

    public void setThumbPaddingRatio(float ratio) {
        this.thumbPaddingRatio = ratio;
        invalidate();
    }

    public void setAnimationDuration(long durationMs) {
        this.animationDuration = durationMs;
    }

    public void setOvershootTension(float tension) {
        this.overshootTension = tension;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.checked = isChecked;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setChecked(ss.checked, false);
    }

    private static class SavedState extends BaseSavedState {
        boolean checked;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            checked = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (checked ? 1 : 0));
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
