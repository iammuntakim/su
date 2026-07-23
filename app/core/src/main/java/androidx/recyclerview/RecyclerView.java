package androidx.recyclerview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

public class RecyclerView extends androidx.recyclerview.widget.RecyclerView {

    private final Path clipPath = new Path();
    private final RectF rectBounds = new RectF();
    private float cornerRadius = 64f;

    public RecyclerView(Context context) {
        super(context);
        init();
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.WHITE);
        if (getParent() instanceof android.view.View) {
            ((android.view.View) getParent()).setBackgroundColor(Color.parseColor("#E0E0E0"));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getParent() instanceof android.view.View) {
            ((android.view.View) getParent()).setBackgroundColor(Color.parseColor("#E0E0E0"));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rectBounds.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(rectBounds, cornerRadius, cornerRadius, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas) {
        int count = canvas.save();
        canvas.clipPath(clipPath);
        super.draw(canvas);
        canvas.restoreToCount(count);
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
        invalidate();
    }
}
