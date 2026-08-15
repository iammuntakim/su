package su.android.databinding;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.InverseMethod;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.topjohnwu.widget.IndeterminateCheckBox;

import java.util.List;
import java.util.Objects;

import kotlin.jvm.functions.Function1;

import su.android.core.model.policy.Policy;
import su.android.utils.TextHolder;

public class DataBindingAdapters {

    @BindingAdapter("gone")
    public static void setGone(View view, boolean gone) {
        view.setVisibility(gone ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("goneUnless")
    public static void setGoneUnless(View view, boolean goneUnless) {
        setGone(view, !goneUnless);
    }

    @BindingAdapter("invisible")
    public static void setInvisible(View view, boolean invisible) {
        view.setVisibility(invisible ? View.INVISIBLE : View.VISIBLE);
    }

    @BindingAdapter("invisibleUnless")
    public static void setInvisibleUnless(View view, boolean invisibleUnless) {
        setInvisible(view, !invisibleUnless);
    }

    @BindingAdapter("markdownText")
    public static void setMarkdownText(TextView view, Spanned markdown) {
        view.setMovementMethod(LinkMovementMethod.getInstance());
        view.setText(markdown);
    }

    @BindingAdapter("srcCompat")
    public static void setImageResource(ImageView view, @DrawableRes int resId) {
        view.setImageResource(resId);
    }

    @BindingAdapter("srcCompat")
    public static void setImageResource(ImageView view, Drawable drawable) {
        view.setImageDrawable(drawable);
    }

    @BindingAdapter("android:text")
    public static void setText(TextView view, TextHolder text) {
        view.setText(text == null ? null : text.getText(view.getResources()));
    }

    @BindingAdapter("onTouch")
    public static void setOnTouchListener(View view, View.OnTouchListener listener) {
        view.setOnTouchListener(listener);
    }

    @BindingAdapter("scrollToLast")
    public static void setScrollToLast(RecyclerView view, boolean shouldScrollToLast) {
        DataBindingAdaptersKt.setScrollToLast(view, shouldScrollToLast);
    }

    @BindingAdapter("isEnabled")
    public static void setEnabled(View view, boolean isEnabled) {
        view.setEnabled(isEnabled);
    }

    @BindingAdapter("isSelected")
    public static void isSelected(View view, boolean isSelected) {
        view.setSelected(isSelected);
    }

    @BindingAdapter("nestedScrollingEnabled")
    public static void setNestedScrolling(RecyclerView view, boolean enabled) {
        view.setNestedScrollingEnabled(enabled);
    }

    @BindingAdapter("strikeThrough")
    public static void setStrikeThroughEnabled(TextView view, boolean useStrikeThrough) {
        int flags = view.getPaintFlags();
        view.setPaintFlags(useStrikeThrough ? flags | Paint.STRIKE_THRU_TEXT_FLAG
                : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    @BindingAdapter("labelFormatter")
    public static void setLabelFormatter(Slider slider, Function1<Float, Integer> formatter) {
        slider.setLabelFormatter(value -> slider.getResources().getString(formatter.invoke(value)));
    }

    @BindingAdapter("state")
    public static void setState(IndeterminateCheckBox view, Boolean state) {
        if (!Objects.equals(view.getState(), state)) {
            view.setState(state);
        }
    }

    @InverseBindingAdapter(attribute = "state")
    public static Boolean getState(IndeterminateCheckBox view) {
        return view.getState();
    }

    @BindingAdapter("stateAttrChanged")
    public static void setListeners(IndeterminateCheckBox view, InverseBindingListener attrChange) {
        view.setOnStateChangedListener((checkbox, state) -> attrChange.onChange());
    }

    @InverseBindingAdapter(attribute = "android:value")
    public static float getValueBinding(Slider view) {
        return view.getValue();
    }

    @BindingAdapter("android:valueAttrChanged")
    public static void setListener(Slider view, InverseBindingListener attrChange) {
        view.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(Slider slider) {
                attrChange.onChange();
            }
        });
    }

    @InverseBindingAdapter(attribute = "android:selection")
    public static int getSelection(Spinner view) {
        return view.getSelectedItemPosition();
    }

    @BindingAdapter("android:selectionAttrChanged")
    public static void setSelectionListener(Spinner view, InverseBindingListener attrChange) {
        view.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
                attrChange.onChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                attrChange.onChange();
            }
        });
    }

    @InverseMethod("sliderValueToPolicy")
    public static float policyToSliderValue(int policy) {
        if (policy == Policy.RESTRICT) {
            return 2f;
        }
        if (policy == Policy.ALLOW) {
            return 3f;
        }
        return 1f;
    }

    public static int sliderValueToPolicy(float value) {
        if (value == 2f) {
            return Policy.RESTRICT;
        }
        if (value == 3f) {
            return Policy.ALLOW;
        }
        return Policy.DENY;
    }

    @BindingAdapter(value = {"items", "extraBindings"}, requireAll = false)
    public static void setAdapter(RecyclerView view, List items, SparseArray extraBindings) {
        RvItemAdapterKt.setAdapter(view, items, extraBindings);
    }

    @BindingAdapter(value = {"items", "layout"})
    public static void setAdapter(Spinner view, Object[] items, int layoutRes) {
        view.setAdapter(new ArrayAdapter<>(view.getContext(), layoutRes, items));
    }

    @BindingAdapter("icon")
    public static void setIconRes(Button view, @DrawableRes int res) {
        ((MaterialButton) view).setIconResource(res);
    }

    @BindingAdapter("icon")
    public static void setIcon(Button view, Drawable drawable) {
        ((MaterialButton) view).setIcon(drawable);
    }
}
