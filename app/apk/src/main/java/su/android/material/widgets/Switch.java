package su.android.material.widgets;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.CompoundButton;

import androidx.annotation.ColorInt;
import androidx.core.view.ViewCompat;

/**
 * An iOS styled switch built on top of the framework {@link CompoundButton}.
 *
 * <p>The thumb glides along a pill shaped track with a springy, overshooting
 * animation (exactly like iOS), supports drag-to-toggle and springs back to
 * the nearest end when released.</p>
 */
public class Switch extends CompoundButton {

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;

    private static final float SPRING_DAMPING_RATIO = 0.62f;
    private static final float SPRING_STIFFNESS = 160f;
    private static final int SPRING_DURATION = 420;

    @ColorInt private static final int TRACK_ON_COLOR_LIGHT = 0xFF34C759;
    @ColorInt private static final int TRACK_ON_COLOR_DARK = 0xFF30D158;
    @ColorInt private static final int TRACK_OFF_COLOR_LIGHT = 0xFFE9E9EB;
    @ColorInt private static final int TRACK_OFF_COLOR_DARK = 0xFF39393D;
    @ColorInt private static final int THUMB_COLOR = 0xFFFFFFFF;

    private final float mDensity;
    private final float mTrackWidth;
    private final float mTrackHeight;
    private final float mThumbDiameter;
    private final float mThumbInset;
    private final float mTrackRadius;

    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mTrackRect = new RectF();

    @ColorInt private final int mTrackOnColor;
    @ColorInt private final int mTrackOffColor;

    private float mPosition = 0f;
    private ValueAnimator mPositionAnimator;

    private int mTouchMode = TOUCH_MODE_IDLE;
    private float mTouchX;
    private float mTouchY;
    private final int mTouchSlop;
    private final int mMinFlingVelocity;
    private VelocityTracker mVelocityTracker;

    private boolean mPressedState = false;
    private boolean mInitialized = false;

    public Switch(Context context) {
        this(context, null);
    }

    public Switch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Switch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Configuration config = context.getResources().getConfiguration();
        final boolean night =
                (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        mTrackOnColor = night ? TRACK_ON_COLOR_DARK : TRACK_ON_COLOR_LIGHT;
        mTrackOffColor = night ? TRACK_OFF_COLOR_DARK : TRACK_OFF_COLOR_LIGHT;

        mDensity = getResources().getDisplayMetrics().density;
        mTrackWidth = 51 * mDensity;
        mTrackHeight = 31 * mDensity;
        mThumbDiameter = 27 * mDensity;
        mThumbInset = (mTrackHeight - mThumbDiameter) / 2f;
        mTrackRadius = mTrackHeight / 2f;

        mThumbPaint.setColor(THUMB_COLOR);
        mShadowPaint.setColor(0x33000000);

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();

        mInitialized = true;
        mPosition = isChecked() ? 1f : 0f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = (int) mTrackWidth + getPaddingLeft() + getPaddingRight();
        final int height = (int) mTrackHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(
                resolveSizeAndState(width, widthMeasureSpec, 0),
                resolveSizeAndState(height, heightMeasureSpec, 0));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;

        final float left = cx - mTrackWidth / 2f;
        final float top = cy - mTrackHeight / 2f;
        mTrackRect.set(left, top, left + mTrackWidth, top + mTrackHeight);

        final int trackColor = lerpColor(mTrackOffColor, mTrackOnColor, mPosition);
        mTrackPaint.setColor(isEnabled() ? trackColor
                : blendAlpha(trackColor, 0x38));
        canvas.drawRoundRect(mTrackRect, mTrackRadius, mTrackRadius, mTrackPaint);

        final float scale = mPressedState ? 1.12f : 1f;
        final float travel = mTrackWidth - mThumbDiameter - mThumbInset * 2f;
        final float position = ViewCompat.isLayoutRtl(this) ? 1f - mPosition : mPosition;
        final float thumbLeft = mTrackRect.left + mThumbInset + position * travel;
        final float thumbCenterX = thumbLeft + mThumbDiameter / 2f;
        final float thumbRadius = mThumbDiameter / 2f * scale;

        mShadowPaint.setColor(isEnabled() ? 0x33000000 : 0x11000000);
        canvas.drawCircle(thumbCenterX, cy + 0.5f * mDensity, thumbRadius, mShadowPaint);
        mThumbPaint.setColor(isEnabled() ? THUMB_COLOR : 0xFFF0F0F0);
        canvas.drawCircle(thumbCenterX, cy, thumbRadius, mThumbPaint);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (!mInitialized) {
            mPosition = checked ? 1f : 0f;
            return;
        }
        final float target = checked ? 1f : 0f;
        if (getWindowToken() != null && isLaidOut()) {
            animatePosition(target);
        } else {
            cancelPositionAnimator();
            mPosition = target;
            invalidate();
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        cancelPositionAnimator();
        mPosition = isChecked() ? 1f : 0f;
        invalidate();
    }

    @Override
    public boolean performClick() {
        if (isEnabled()) {
            toggle();
        }
        return super.performClick();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (isEnabled() && hitThumb(ev.getX(), ev.getY())) {
                    mTouchMode = TOUCH_MODE_DOWN;
                    mTouchX = ev.getX();
                    mTouchY = ev.getY();
                    mPressedState = true;
                    invalidate();
                    return true;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mTouchMode == TOUCH_MODE_DOWN) {
                    final float dx = Math.abs(ev.getX() - mTouchX);
                    final float dy = Math.abs(ev.getY() - mTouchY);
                    if (dx > mTouchSlop || dy > mTouchSlop) {
                        mTouchMode = TOUCH_MODE_DRAGGING;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        mTouchX = ev.getX();
                        mTouchY = ev.getY();
                        cancelPositionAnimator();
                    }
                } else if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    final float dx = ev.getX() - mTouchX;
                    final float travel = mTrackWidth - mThumbDiameter - mThumbInset * 2f;
                    float dPos = travel > 0 ? dx / travel : 0f;
                    if (ViewCompat.isLayoutRtl(this)) {
                        dPos = -dPos;
                    }
                    final float newPos = clamp(mPosition + dPos, 0f, 1f);
                    if (newPos != mPosition) {
                        mTouchX = ev.getX();
                        mPosition = newPos;
                        invalidate();
                    }
                    return true;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    stopDrag(ev);
                    recycleVelocityTracker();
                    return true;
                }
                if (mTouchMode == TOUCH_MODE_DOWN) {
                    mTouchMode = TOUCH_MODE_IDLE;
                    mPressedState = false;
                    invalidate();
                    recycleVelocityTracker();
                    if (ev.getAction() == MotionEvent.ACTION_UP && isEnabled()) {
                        playSoundEffect(SoundEffectConstants.CLICK);
                        toggle();
                    }
                    return true;
                }
                recycleVelocityTracker();
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    private void stopDrag(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_IDLE;
        mPressedState = false;
        invalidate();

        boolean newState;
        if (ev.getAction() == MotionEvent.ACTION_UP && isEnabled()) {
            mVelocityTracker.computeCurrentVelocity(1000);
            final float xvel = mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) > mMinFlingVelocity) {
                newState = ViewCompat.isLayoutRtl(this) ? xvel < 0 : xvel > 0;
            } else {
                newState = mPosition > 0.5f;
            }
        } else {
            newState = isChecked();
        }

        if (newState != isChecked()) {
            playSoundEffect(SoundEffectConstants.CLICK);
        }
        setChecked(newState);
    }

    private boolean hitThumb(float x, float y) {
        final float travel = mTrackWidth - mThumbDiameter - mThumbInset * 2f;
        final float position = ViewCompat.isLayoutRtl(this) ? 1f - mPosition : mPosition;
        final float thumbLeft = getWidth() / 2f - mTrackWidth / 2f + mThumbInset + position * travel;
        final float thumbRight = thumbLeft + mThumbDiameter;
        final float trackTop = getHeight() / 2f - mTrackHeight / 2f;
        final float trackBottom = trackTop + mTrackHeight;
        return x >= thumbLeft - mTouchSlop && x <= thumbRight + mTouchSlop
                && y >= trackTop - mTouchSlop && y <= trackBottom + mTouchSlop;
    }

    private void animatePosition(float to) {
        cancelPositionAnimator();
        final float from = mPosition;
        if (from == to) {
            return;
        }
        mPositionAnimator = ValueAnimator.ofFloat(0f, 1f);
        mPositionAnimator.setDuration(SPRING_DURATION);
        mPositionAnimator.setInterpolator(new SpringInterpolator(SPRING_DAMPING_RATIO, SPRING_STIFFNESS));
        mPositionAnimator.addUpdateListener(animation -> {
            final float fraction = (float) animation.getAnimatedValue();
            mPosition = clamp(from + (to - from) * fraction, 0f, 1f);
            invalidate();
        });
        mPositionAnimator.start();
    }

    private void cancelPositionAnimator() {
        if (mPositionAnimator != null) {
            mPositionAnimator.cancel();
            mPositionAnimator = null;
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int lerpColor(@ColorInt int from, @ColorInt int to, float fraction) {
        final float f = clamp(fraction, 0f, 1f);
        return Color.argb(
                (int) (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * f),
                (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * f),
                (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * f),
                (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * f));
    }

    private static int blendAlpha(@ColorInt int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /** A damped spring interpolator that slightly overshoots then settles. */
    private static final class SpringInterpolator implements TimeInterpolator {
        private final double mOmegaDamped;

        SpringInterpolator(float dampingRatio, float stiffness) {
            final double omega0 = Math.sqrt(stiffness);
            mOmegaDamped = omega0 * Math.sqrt(1.0 - dampingRatio * dampingRatio);
            mDecay = dampingRatio * omega0;
        }

        private final double mDecay;

        @Override
        public float getInterpolation(float input) {
            final double t = input;
            final double value = 1.0 - Math.exp(-mDecay * t) * Math.cos(mOmegaDamped * t);
            return (float) Math.max(0.0, value);
        }
    }
}
