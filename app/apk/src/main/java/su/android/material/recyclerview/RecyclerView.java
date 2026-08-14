package su.android.material.recyclerview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearSmoothScroller;

/**
 * An iOS feel {@link RecyclerView}.
 *
 * <p>Provides buttery item animations, a slow smooth glide for programmatic
 * scrolling and a subtle overscroll glow so the list feels effortless.</p>
 */
public class RecyclerView extends androidx.recyclerview.widget.RecyclerView {

    public RecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setItemAnimator(new SmoothItemAnimator());
        setEdgeEffectFactory(new IOSEdgeEffectFactory());
    }

    @Override
    public void smoothScrollToPosition(int position) {
        final LinearSmoothScroller scroller = new LinearSmoothScroller(getContext()) {
            @Override
            protected float calculateSpeedPerPixel(@NonNull DisplayMetrics displayMetrics) {
                return 0.04f;
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }

            @Override
            protected int getHorizontalSnapPreference() {
                return SNAP_TO_START;
            }
        };
        scroller.setTargetPosition(position);
        final LayoutManager layoutManager = getLayoutManager();
        if (layoutManager != null) {
            layoutManager.startSmoothScroll(scroller);
        }
    }

    /**
     * Smoothly scrolls by the given distance using the current smooth scroller.
     */
    public void smoothScrollBy(int dx, int dy) {
        smoothScrollBy(dx, dy, new FastOutSlowInInterpolator());
    }

    /**
     * Smoothly scrolls to the very top of the list.
     */
    public void smoothScrollToTop() {
        if (getAdapter() != null && getAdapter().getItemCount() > 0) {
            smoothScrollToPosition(0);
        }
    }

    /**
     * Smoothly scrolls to the very bottom of the list.
     */
    public void smoothScrollToBottom() {
        final int count = getAdapter() != null ? getAdapter().getItemCount() : 0;
        if (count > 0) {
            smoothScrollToPosition(count - 1);
        }
    }

    private static final class SmoothItemAnimator extends DefaultItemAnimator {
        SmoothItemAnimator() {
            setAddDuration(320);
            setRemoveDuration(240);
            setMoveDuration(360);
            setChangeDuration(320);
            setInterpolator(new FastOutSlowInInterpolator());
        }
    }

    private static final class IOSEdgeEffectFactory extends EdgeEffectFactory {
        @NonNull
        @Override
        protected EdgeEffect createEdgeEffect(@NonNull androidx.recyclerview.widget.RecyclerView view, int direction) {
            return new EdgeEffect(view.getContext()) {
                @Override
                public void onPull(float deltaDistance) {
                    super.onPull(deltaDistance * 0.6f);
                }

                @Override
                public void onPull(float deltaDistance, float displacement) {
                    super.onPull(deltaDistance * 0.6f, displacement);
                }
            };
        }
    }
}
