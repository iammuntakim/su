package top.mauix.core;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

public class RecyclerView extends androidx.recyclerview.widget.RecyclerView {

    public static abstract class OnScrollListener extends androidx.recyclerview.widget.RecyclerView.OnScrollListener {}
    public static abstract class Adapter<VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder> extends androidx.recyclerview.widget.RecyclerView.Adapter<VH> {}
    public static abstract class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    public static abstract class ItemDecoration extends androidx.recyclerview.widget.RecyclerView.ItemDecoration {}
    public interface OnItemTouchListener extends androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {}
    public static abstract class ItemAnimator extends androidx.recyclerview.widget.RecyclerView.ItemAnimator {}
    public static abstract class LayoutManager extends androidx.recyclerview.widget.RecyclerView.LayoutManager {}
    public static class State extends androidx.recyclerview.widget.RecyclerView.State {}

    private float startY = 0f;
    private float lastY = 0f;
    private float overscrollOffset = 0f;
    private boolean isOverscrolling = false;
    private ValueAnimator resetAnimator;
    private int touchSlop;

    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groupBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF groupBounds = new RectF();

    private boolean enableIosBounce = true;
    private boolean enableGroupedStyle = true;
    private boolean enablePressAnimation = true;

    private int dividerInsetLeft = 56;
    private int dividerInsetRight = 0;
    private int dividerHeight = 2;
    private int dividerColor = Color.argb(35, 60, 60, 67);
    private int groupBackgroundColor = Color.argb(255, 255, 255, 255);

    private float groupCornerRadius = 28f;
    private float resistanceFactor = 0.42f;
    private float pressScaleFactor = 0.97f;
    private int bounceDuration = 400;

    public RecyclerView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOverScrollMode(OVER_SCROLL_NEVER);

        dividerPaint.setColor(dividerColor);
        dividerPaint.setStyle(Paint.Style.FILL);

        groupBackgroundPaint.setColor(groupBackgroundColor);
        groupBackgroundPaint.setStyle(Paint.Style.FILL);

        setLayoutManager(new LinearLayoutManager(context));
        setItemAnimator(new IosItemAnimator());
        addItemDecoration(new IosDividerItemDecoration());
        addOnItemTouchListener(new IosItemTouchListener());
    }

    public void setEnableIosBounce(boolean enable) {
        this.enableIosBounce = enable;
    }

    public boolean isEnableIosBounce() {
        return enableIosBounce;
    }

    public void setEnableGroupedStyle(boolean enable) {
        this.enableGroupedStyle = enable;
        invalidate();
    }

    public boolean isEnableGroupedStyle() {
        return enableGroupedStyle;
    }

    public void setEnablePressAnimation(boolean enable) {
        this.enablePressAnimation = enable;
    }

    public boolean isEnablePressAnimation() {
        return enablePressAnimation;
    }

    public void setDividerColor(int color) {
        this.dividerColor = color;
        this.dividerPaint.setColor(color);
        invalidate();
    }

    public int getDividerColor() {
        return dividerColor;
    }

    public void setDividerInsets(int left, int right) {
        this.dividerInsetLeft = left;
        this.dividerInsetRight = right;
        invalidate();
    }

    public int getDividerInsetLeft() {
        return dividerInsetLeft;
    }

    public int getDividerInsetRight() {
        return dividerInsetRight;
    }

    public void setDividerHeight(int height) {
        this.dividerHeight = height;
        invalidate();
    }

    public int getDividerHeight() {
        return dividerHeight;
    }

    public void setGroupBackgroundColor(int color) {
        this.groupBackgroundColor = color;
        this.groupBackgroundPaint.setColor(color);
        invalidate();
    }

    public int getGroupBackgroundColor() {
        return groupBackgroundColor;
    }

    public void setGroupCornerRadius(float radius) {
        this.groupCornerRadius = radius;
        invalidate();
    }

    public float getGroupCornerRadius() {
        return groupCornerRadius;
    }

    public void setResistanceFactor(float factor) {
        this.resistanceFactor = factor;
    }

    public float getResistanceFactor() {
        return resistanceFactor;
    }

    public void setBounceDuration(int duration) {
        this.bounceDuration = duration;
    }

    public int getBounceDuration() {
        return bounceDuration;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!enableIosBounce) {
            return super.onTouchEvent(e);
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startY = e.getY();
                lastY = e.getY();
                if (resetAnimator != null && resetAnimator.isRunning()) {
                    resetAnimator.cancel();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float currentY = e.getY();
                float deltaY = currentY - lastY;
                lastY = currentY;

                boolean canScrollUp = canScrollVertically(-1);
                boolean canScrollDown = canScrollVertically(1);

                if ((!canScrollUp && deltaY > 0) || (!canScrollDown && deltaY < 0) || isOverscrolling) {
                    isOverscrolling = true;
                    float damping = Math.max(0.08f, 1.0f - (Math.abs(overscrollOffset) / (getHeight() * 1.4f)));
                    overscrollOffset += deltaY * resistanceFactor * damping;
                    setTranslationY(overscrollOffset);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isOverscrolling) {
                    animateBounceBack();
                    isOverscrolling = false;
                    return true;
                }
                break;
        }

        return super.onTouchEvent(e);
    }

    private void animateBounceBack() {
        if (resetAnimator != null && resetAnimator.isRunning()) {
            resetAnimator.cancel();
        }

        resetAnimator = ValueAnimator.ofFloat(overscrollOffset, 0f);
        resetAnimator.setDuration(bounceDuration);
        resetAnimator.setInterpolator(new DecelerateInterpolator(2.0f));
        resetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                overscrollOffset = ((Float) animation.getAnimatedValue()).floatValue();
                setTranslationY(overscrollOffset);
            }
        });
        resetAnimator.start();
    }

    @Override
    public void onDraw(Canvas c) {
        if (enableGroupedStyle && getChildCount() > 0) {
            drawGroupedBackground(c);
        }
        super.onDraw(c);
    }

    private void drawGroupedBackground(Canvas c) {
        int firstVisible = getChildCount() > 0 ? 0 : -1;
        int lastVisible = getChildCount() - 1;

        if (firstVisible >= 0 && lastVisible >= 0) {
            View topChild = getChildAt(firstVisible);
            View bottomChild = getChildAt(lastVisible);

            if (topChild != null && bottomChild != null) {
                groupBounds.set(
                    getPaddingLeft(),
                    topChild.getTop(),
                    getWidth() - getPaddingRight(),
                    bottomChild.getBottom()
                );
                c.drawRoundRect(groupBounds, groupCornerRadius, groupCornerRadius, groupBackgroundPaint);
            }
        }
    }

    private class IosDividerItemDecoration extends androidx.recyclerview.widget.RecyclerView.ItemDecoration {

        @Override
        public void onDrawOver(@NonNull Canvas c, @NonNull androidx.recyclerview.widget.RecyclerView parent, @NonNull androidx.recyclerview.widget.RecyclerView.State state) {
            int childCount = parent.getChildCount();
            int width = parent.getWidth();

            for (int i = 0; i < childCount - 1; i++) {
                View child = parent.getChildAt(i);
                int position = parent.getChildAdapterPosition(child);
                if (position == NO_POSITION) {
                    continue;
                }

                int top = child.getBottom();
                int bottom = top + dividerHeight;
                int left = parent.getPaddingLeft() + dividerInsetLeft;
                int right = width - parent.getPaddingRight() - dividerInsetRight;

                c.drawRect(left, top, right, bottom, dividerPaint);
            }
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull androidx.recyclerview.widget.RecyclerView parent, @NonNull androidx.recyclerview.widget.RecyclerView.State state) {
            outRect.set(0, 0, 0, dividerHeight);
        }
    }

    private class IosItemTouchListener implements androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {

        private View currentPressedView = null;

        @Override
        public boolean onInterceptTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull MotionEvent e) {
            if (!enablePressAnimation) {
                return false;
            }

            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null) {
                    currentPressedView = child;
                    animateScale(child, pressScaleFactor);
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (currentPressedView != null) {
                    animateScale(currentPressedView, 1.0f);
                    currentPressedView = null;
                }
            }
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull MotionEvent e) {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (currentPressedView != null) {
                    animateScale(currentPressedView, 1.0f);
                    currentPressedView = null;
                }
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (disallowIntercept && currentPressedView != null) {
                animateScale(currentPressedView, 1.0f);
                currentPressedView = null;
            }
        }

        private void animateScale(View view, float scale) {
            view.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(120)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
        }
    }

    public static class IosItemAnimator extends DefaultItemAnimator {

        @Override
        public boolean animateAdd(final androidx.recyclerview.widget.RecyclerView.ViewHolder holder) {
            dispatchAddStarting(holder);
            holder.itemView.setAlpha(0f);
            holder.itemView.setScaleX(0.90f);
            holder.itemView.setScaleY(0.90f);
            holder.itemView.animate()
                .alpha(1f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.05f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dispatchAddFinished(holder);
                    }
                })
                .start();
            return false;
        }

        @Override
        public boolean animateRemove(final androidx.recyclerview.widget.RecyclerView.ViewHolder holder) {
            dispatchRemoveStarting(holder);
            holder.itemView.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(240)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dispatchRemoveFinished(holder);
                    }
                })
                .start();
            return false;
        }
    }
}
