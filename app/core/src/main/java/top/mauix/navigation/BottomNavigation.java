package top.mauix.navigation;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.PathInterpolator;
import android.view.View.BaseSavedState;
import java.util.ArrayList;
import java.util.List;

public class BottomNavigation extends View {

    public enum LabelVisibilityMode { MODE_AUTO, MODE_ICON_TEXT, MODE_ICON_ONLY, MODE_ICON_SELECTED_LABEL }
    public interface OnItemSelectedListener { boolean onNavigationItemSelected(MenuItem item); }
    public interface OnItemReselectedListener { void onNavigationItemReselected(MenuItem item); }

    private static final int[] STATE_SET = { android.R.attr.state_hidden };

    private BottomMenu menu;
    private final List<ItemHolder> items = new ArrayList<>();
    private int selectedIndex = 0;
    private LabelVisibilityMode labelVisibilityMode = LabelVisibilityMode.MODE_AUTO;
    private boolean isHidden;

    private int activeColor = Color.parseColor("#007AFF"), inactiveColor = Color.parseColor("#8E8E93");
    private int badgeBgColor = Color.parseColor("#FF3B30"), blurBgColor = Color.parseColor("#CCF9F9F9");
    private int currentBlurBgColor, currentInactiveColor, currentActiveColor;

    private ValueAnimator themeAnimator;
    private int startBlurBgColor, targetBlurBgColor, startInactiveColor, targetInactiveColor, startActiveColor, targetActiveColor;

    private float iconSizePx, textSizePx, badgeTextSizePx, cornerRadiusPx;
    private Paint bgPaint, textPaint, badgeBgPaint, badgeTextPaint;
    private RectF boundsRect = new RectF(), tempRect = new RectF();

    private OnItemSelectedListener itemSelectedListener;
    private OnItemReselectedListener itemReselectedListener;

    private int touchSlop, pressedIndex = -1;
    private float touchStartX, touchStartY, customWidthPx = -1, customHeightPx = -1;
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    public BottomNavigation(Context c) { this(c, null); }
    public BottomNavigation(Context c, AttributeSet a) { this(c, a, 0); }
    public BottomNavigation(Context c, AttributeSet a, int d) {
        super(c, a, d);
        menu = new BottomMenu(c);
        touchSlop = ViewConfiguration.get(c).getScaledTouchSlop();
        float density = getResources().getDisplayMetrics().density;
        
        iconSizePx = 24 * density; textSizePx = 11 * density;
        badgeTextSizePx = 10 * density; cornerRadiusPx = 20 * density;
        currentBlurBgColor = blurBgColor; currentInactiveColor = inactiveColor; currentActiveColor = activeColor;

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG); bgPaint.setColor(currentBlurBgColor);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG); textPaint.setTextAlign(Paint.Align.CENTER); textPaint.setTextSize(textSizePx);
        badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG); badgeBgPaint.setColor(badgeBgColor);
        badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG); badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER); badgeTextPaint.setTextSize(badgeTextSizePx); badgeTextPaint.setFakeBoldText(true);

        setClickable(true); setFocusable(true);
    }

    private void recreateAnimator(int height) {
        Animator toHidden = ObjectAnimator.ofFloat(this, "translationY", height);
        toHidden.setDuration(175);
        toHidden.setInterpolator(new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f));
        Animator toUnhidden = ObjectAnimator.ofFloat(this, "translationY", 0);
        toUnhidden.setDuration(225);
        toUnhidden.setInterpolator(new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f));

        StateListAnimator animator = new StateListAnimator();
        animator.addState(STATE_SET, toHidden);
        animator.addState(new int[]{}, toUnhidden);
        setStateListAnimator(animator);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isHidden()) {
            mergeDrawableStates(drawableState, STATE_SET);
        }
        return drawableState;
    }

    public boolean isHidden() { return isHidden; }

    public void setHidden(boolean raised) {
        if (isHidden != raised) {
            isHidden = raised;
            refreshDrawableState();
        }
    }

    public Menu getMenu() { return menu; }

    public void inflateMenu(int resId) {
        menu.clear(); items.clear();
        new MenuInflater(getContext()).inflate(resId, menu);
        syncMenuItems();
    }

    public void syncMenuItems() {
        items.clear();
        for (int i = 0; i < menu.size(); i++) items.add(new ItemHolder(menu.getItem(i)));
        requestLayout(); invalidate();
    }

    public void setOnItemSelectedListener(OnItemSelectedListener l) { this.itemSelectedListener = l; }
    public void setOnItemReselectedListener(OnItemReselectedListener l) { this.itemReselectedListener = l; }

    public void setSelectedItemId(int itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).item.getItemId() == itemId) { setSelectedIndex(i); break; }
        }
    }

    public int getSelectedItemId() {
        return (selectedIndex >= 0 && selectedIndex < items.size()) ? items.get(selectedIndex).item.getItemId() : -1;
    }

    public void setLabelVisibilityMode(LabelVisibilityMode mode) {
        this.labelVisibilityMode = mode;
        for (int i = 0; i < items.size(); i++) items.get(i).updateVisibilityProgress(i == selectedIndex, false);
        requestLayout(); invalidate();
    }

    public void setDarkMode(boolean enabled) { setDarkMode(enabled, true); }

    public void setDarkMode(boolean enabled, boolean animate) {
        startBlurBgColor = currentBlurBgColor; startInactiveColor = currentInactiveColor; startActiveColor = currentActiveColor;
        targetBlurBgColor = Color.parseColor(enabled ? "#CC1C1C1E" : "#CCF9F9F9");
        targetInactiveColor = Color.parseColor("#8E8E93");
        targetActiveColor = Color.parseColor(enabled ? "#0A84FF" : "#007AFF");

        if (!animate) {
            currentBlurBgColor = targetBlurBgColor; currentInactiveColor = targetInactiveColor; currentActiveColor = targetActiveColor;
            bgPaint.setColor(currentBlurBgColor); invalidate(); return;
        }

        if (themeAnimator != null && themeAnimator.isRunning()) themeAnimator.cancel();
        themeAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        themeAnimator.setDuration(350);
        themeAnimator.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f));
        themeAnimator.addUpdateListener(a -> {
            float f = (Float) a.getAnimatedValue();
            currentBlurBgColor = (Integer) argbEvaluator.evaluate(f, startBlurBgColor, targetBlurBgColor);
            currentInactiveColor = (Integer) argbEvaluator.evaluate(f, startInactiveColor, targetInactiveColor);
            currentActiveColor = (Integer) argbEvaluator.evaluate(f, startActiveColor, targetActiveColor);
            bgPaint.setColor(currentBlurBgColor); invalidate();
        });
        themeAnimator.start();
    }

    public void setActiveColor(int color) { this.activeColor = color; this.currentActiveColor = color; invalidate(); }
    public void setInactiveColor(int color) { this.inactiveColor = color; this.currentInactiveColor = color; invalidate(); }
    public void setBadgeBackgroundColor(int color) { this.badgeBgColor = color; badgeBgPaint.setColor(badgeBgColor); invalidate(); }
    public void setCornerRadiusDp(float dp) { this.cornerRadiusPx = dp * getResources().getDisplayMetrics().density; invalidate(); }

    public void setCustomSizeDp(float widthDp, float heightDp) {
        float density = getResources().getDisplayMetrics().density;
        this.customWidthPx = widthDp > 0 ? widthDp * density : -1;
        this.customHeightPx = heightDp > 0 ? heightDp * density : -1;
        requestLayout(); invalidate();
    }

    public void setBadge(int itemId, String text) {
        for (ItemHolder holder : items) {
            if (holder.item.getItemId() == itemId) { holder.badgeText = text; invalidate(); break; }
        }
    }

    private void setSelectedIndex(int index) {
        if (index < 0 || index >= items.size()) return;
        if (selectedIndex == index) {
            if (itemReselectedListener != null) itemReselectedListener.onNavigationItemReselected(items.get(index).item);
            return;
        }

        int prev = selectedIndex; selectedIndex = index;
        if (itemSelectedListener != null && !itemSelectedListener.onNavigationItemSelected(items.get(index).item)) {
            selectedIndex = prev; return;
        }

        for (int i = 0; i < items.size(); i++) items.get(i).item.setChecked(i == selectedIndex);
        if (prev >= 0 && prev < items.size()) items.get(prev).animateState(false);
        if (selectedIndex >= 0 && selectedIndex < items.size()) items.get(selectedIndex).animateState(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wSize = MeasureSpec.getSize(widthMeasureSpec), hSize = MeasureSpec.getSize(heightMeasureSpec);
        int defH = (int) (60 * getResources().getDisplayMetrics().density);
        int targetW = (customWidthPx > 0) ? (int) customWidthPx : wSize;
        int targetH = (customHeightPx > 0) ? (int) customHeightPx : defH;
        setMeasuredDimension(
            (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) ? wSize : Math.min(targetW, wSize),
            (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) ? hSize : targetH
        );
        recreateAnimator(getMeasuredHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh); boundsRect.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRoundRect(boundsRect, cornerRadiusPx, cornerRadiusPx, bgPaint);
        if (items.isEmpty()) return;

        float itemWidth = (float) getWidth() / items.size();
        int height = getHeight();

        for (int i = 0; i < items.size(); i++) {
            ItemHolder holder = items.get(i);
            float centerX = (i * itemWidth) + (itemWidth / 2.0f);

            canvas.save();
            canvas.scale(holder.scaleProgress, holder.scaleProgress, centerX, height / 2.0f);

            int color = (Integer) argbEvaluator.evaluate(holder.progress, currentInactiveColor, currentActiveColor);
            float labelAlpha = holder.labelProgress;
            float iconY = (height / 2.0f) - (((textSizePx / 2.0f) + (4 * getResources().getDisplayMetrics().density)) * labelAlpha);

            Drawable icon = holder.item.getIcon();
            if (icon != null) {
                int left = (int) (centerX - (iconSizePx / 2.0f)), top = (int) (iconY - (iconSizePx / 2.0f));
                icon.setBounds(left, top, (int) (left + iconSizePx), (int) (top + iconSizePx));
                icon.setTint(color); icon.draw(canvas);
            }

            if (labelAlpha > 0.01f && holder.item.getTitle() != null) {
                textPaint.setColor(color); textPaint.setAlpha((int) (255 * labelAlpha));
                float textY = iconY + (iconSizePx / 2.0f) + (4 * getResources().getDisplayMetrics().density) + textSizePx;
                canvas.drawText(holder.item.getTitle().toString(), centerX, textY, textPaint);
            }

            if (holder.badgeText != null && !holder.badgeText.isEmpty()) {
                drawBadge(canvas, holder.badgeText, centerX + (iconSizePx / 2.0f), iconY - (iconSizePx / 2.0f));
            }
            canvas.restore();
        }
    }

    private boolean shouldShowLabel(boolean isSelected) {
        switch (labelVisibilityMode) {
            case MODE_ICON_ONLY: return false;
            case MODE_ICON_TEXT: return true;
            case MODE_ICON_SELECTED_LABEL: return isSelected;
            default: return items.size() <= 3 || isSelected;
        }
    }

    private void drawBadge(Canvas canvas, String text, float badgeX, float badgeY) {
        float padding = 4 * getResources().getDisplayMetrics().density;
        float textWidth = badgeTextPaint.measureText(text);
        float h = badgeTextSizePx + padding;
        float w = Math.max(h, textWidth + (padding * 2));

        tempRect.set(badgeX, badgeY, badgeX + w, badgeY + h);
        canvas.drawRoundRect(tempRect, h / 2.0f, h / 2.0f, badgeBgPaint);
        canvas.drawText(text, tempRect.centerX(), tempRect.centerY() - ((badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2.0f), badgeTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || items.isEmpty()) return false;
        float x = event.getX(), y = event.getY();
        int clickedIndex = (int) (x / ((float) getWidth() / items.size()));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = x; touchStartY = y; pressedIndex = clickedIndex;
                if (pressedIndex >= 0 && pressedIndex < items.size()) items.get(pressedIndex).animatePress(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - touchStartX) > touchSlop || Math.abs(y - touchStartY) > touchSlop) {
                    if (pressedIndex >= 0 && pressedIndex < items.size()) items.get(pressedIndex).animatePress(false);
                    pressedIndex = -1;
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (pressedIndex >= 0 && pressedIndex < items.size()) {
                    items.get(pressedIndex).animatePress(false);
                    setSelectedIndex(pressedIndex);
                }
                pressedIndex = -1; return true;
            case MotionEvent.ACTION_CANCEL:
                if (pressedIndex >= 0 && pressedIndex < items.size()) items.get(pressedIndex).animatePress(false);
                pressedIndex = -1; return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.isHidden = isHidden();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setHidden(ss.isHidden);
    }

    static class SavedState extends BaseSavedState {
        public boolean isHidden;

        public SavedState(Parcel source) {
            super(source);
            isHidden = source.readByte() != 0;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte(isHidden ? (byte) 1 : (byte) 0);
        }

        public static final Creator<SavedState> CREATOR = new Creator<>() {
            @Override
            public SavedState createFromParcel(Parcel source) { return new SavedState(source); }
            @Override
            public SavedState[] newArray(int size) { return new SavedState[size]; }
        };
    }

    private class ItemHolder {
        MenuItem item;
        float progress, labelProgress, scaleProgress = 1.0f;
        String badgeText;
        ValueAnimator selectAnim, labelAnim, pressAnim;

        ItemHolder(MenuItem item) {
            this.item = item;
            boolean isSel = item.isChecked();
            this.progress = isSel ? 1.0f : 0.0f;
            this.labelProgress = shouldShowLabel(isSel) ? 1.0f : 0.0f;
        }

        void updateVisibilityProgress(boolean isSelected, boolean animate) {
            float target = shouldShowLabel(isSelected) ? 1.0f : 0.0f;
            if (!animate) { labelProgress = target; invalidate(); return; }

            if (labelAnim != null && labelAnim.isRunning()) labelAnim.cancel();
            labelAnim = ValueAnimator.ofFloat(labelProgress, target);
            labelAnim.setDuration(320);
            labelAnim.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f));
            labelAnim.addUpdateListener(a -> { labelProgress = (Float) a.getAnimatedValue(); invalidate(); });
            labelAnim.start();
        }

        void animateState(boolean select) {
            if (selectAnim != null && selectAnim.isRunning()) selectAnim.cancel();
            selectAnim = ValueAnimator.ofFloat(progress, select ? 1.0f : 0.0f);
            selectAnim.setDuration(320);
            selectAnim.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f));
            selectAnim.addUpdateListener(a -> { progress = (Float) a.getAnimatedValue(); invalidate(); });
            selectAnim.start();
            updateVisibilityProgress(select, true);
        }

        void animatePress(boolean pressed) {
            if (pressAnim != null && pressAnim.isRunning()) pressAnim.cancel();
            pressAnim = ValueAnimator.ofFloat(scaleProgress, pressed ? 0.92f : 1.0f);
            pressAnim.setDuration(pressed ? 120 : 220);
            pressAnim.setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f));
            pressAnim.addUpdateListener(a -> { scaleProgress = (Float) a.getAnimatedValue(); invalidate(); });
            pressAnim.start();
        }
    }

    private static class BottomMenu implements Menu {
        private final Context context;
        private final List<MenuItem> menuItems = new ArrayList<>();

        BottomMenu(Context context) { this.context = context; }

        @Override public MenuItem add(CharSequence title) { return add(0, 0, 0, title); }
        @Override public MenuItem add(int titleRes) { return add(0, 0, 0, context.getString(titleRes)); }
        @Override public MenuItem add(int g, int i, int o, int titleRes) { return add(g, i, o, context.getString(titleRes)); }
        @Override public MenuItem add(int g, int id, int o, CharSequence title) {
            MenuItem item = new BottomMenuItem(context, id, g, o, title);
            menuItems.add(item); return item;
        }

        @Override public void clear() { menuItems.clear(); }
        @Override public MenuItem findItem(int id) {
            for (MenuItem item : menuItems) if (item.getItemId() == id) return item;
            return null;
        }
        @Override public MenuItem getItem(int index) { return menuItems.get(index); }
        @Override public int size() { return menuItems.size(); }

        @Override public int addIntentOptions(int g, int i, int o, android.content.ComponentName c, android.content.Intent[] s, android.content.Intent in, int f, MenuItem[] out) { return 0; }
        @Override public android.view.SubMenu addSubMenu(CharSequence title) { return null; }
        @Override public android.view.SubMenu addSubMenu(int titleRes) { return null; }
        @Override public android.view.SubMenu addSubMenu(int g, int i, int o, CharSequence t) { return null; }
        @Override public android.view.SubMenu addSubMenu(int g, int i, int o, int t) { return null; }
        @Override public void close() {}
        @Override public boolean hasVisibleItems() { return !menuItems.isEmpty(); }
        @Override public boolean isShortcutKey(int k, android.view.KeyEvent e) { return false; }
        @Override public boolean performIdentifierAction(int id, int f) { return false; }
        @Override public boolean performShortcut(int k, android.view.KeyEvent e, int f) { return false; }
        @Override public void removeGroup(int g) {}
        @Override public void removeItem(int id) { MenuItem item = findItem(id); if (item != null) menuItems.remove(item); }
        @Override public void setGroupCheckable(int g, boolean c, boolean e) {}
        @Override public void setGroupEnabled(int g, boolean e) {}
        @Override public void setGroupVisible(int g, boolean v) {}
        @Override public void setQwertyMode(boolean q) {}
    }

    private static class BottomMenuItem implements MenuItem {
        private final Context context;
        private final int itemId, groupId, order;
        private CharSequence title;
        private Drawable icon;
        private boolean isChecked = false, isEnabled = true, isVisible = true;

        BottomMenuItem(Context context, int itemId, int groupId, int order, CharSequence title) {
            this.context = context; this.itemId = itemId; this.groupId = groupId; this.order = order; this.title = title;
        }

        @Override public int getItemId() { return itemId; }
        @Override public int getGroupId() { return groupId; }
        @Override public int getOrder() { return order; }
        @Override public MenuItem setTitle(CharSequence title) { this.title = title; return this; }
        @Override public MenuItem setTitle(int title) { this.title = context.getString(title); return this; }
        @Override public CharSequence getTitle() { return title; }
        @Override public MenuItem setIcon(Drawable icon) { this.icon = icon; return this; }
        @Override public MenuItem setIcon(int iconRes) { this.icon = context.getDrawable(iconRes); return this; }
        @Override public Drawable getIcon() { return icon; }
        @Override public MenuItem setChecked(boolean checked) { this.isChecked = checked; return this; }
        @Override public boolean isChecked() { return isChecked; }
        @Override public MenuItem setEnabled(boolean enabled) { this.isEnabled = enabled; return this; }
        @Override public boolean isEnabled() { return isEnabled; }
        @Override public MenuItem setVisible(boolean visible) { this.isVisible = visible; return this; }
        @Override public boolean isVisible() { return isVisible; }

        @Override public CharSequence getTitleCondensed() { return title; }
        @Override public MenuItem setTitleCondensed(CharSequence t) { return this; }
        @Override public MenuItem setIntent(android.content.Intent intent) { return this; }
        @Override public android.content.Intent getIntent() { return null; }
        @Override public MenuItem setShortcut(char n, char a) { return this; }
        @Override public MenuItem setNumericShortcut(char n) { return this; }
        @Override public char getNumericShortcut() { return 0; }
        @Override public MenuItem setAlphabeticShortcut(char a) { return this; }
        @Override public char getAlphabeticShortcut() { return 0; }
        @Override public MenuItem setCheckable(boolean checkable) { return this; }
        @Override public boolean isCheckable() { return true; }
        @Override public boolean hasSubMenu() { return false; }
        @Override public android.view.SubMenu getSubMenu() { return null; }
        @Override public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener l) { return this; }
        @Override public android.view.ContextMenu.ContextMenuInfo getMenuInfo() { return null; }
        @Override public void setShowAsAction(int actionEnum) {}
        @Override public MenuItem setShowAsActionFlags(int actionEnum) { return this; }
        @Override public MenuItem setActionView(View view) { return this; }
        @Override public MenuItem setActionView(int resId) { return this; }
        @Override public View getActionView() { return null; }
        @Override public MenuItem setActionProvider(android.view.ActionProvider actionProvider) { return this; }
        @Override public android.view.ActionProvider getActionProvider() { return null; }
        @Override public boolean expandActionView() { return false; }
        @Override public boolean collapseActionView() { return false; }
        @Override public boolean isActionViewExpanded() { return false; }
        @Override public MenuItem setOnActionExpandListener(OnActionExpandListener listener) { return this; }
    }
}