package su.android.ui.superuser

import android.graphics.drawable.Drawable
import su.android.databinding.Bindable
import su.android.BR
import su.android.R
import su.android.core.Config
import su.android.core.model.policy.Policy
import su.android.databinding.DiffItem
import su.android.databinding.ItemWrapper
import su.android.databinding.ObservableRvItem
import su.android.databinding.set
import su.android.R as CoreR

class PolicyRvItem(
    private val viewModel: SuperuserViewModel,
    override val item: Policy,
    val packageName: String,
    private val isSharedUid: Boolean,
    val icon: Drawable,
    val appName: String
) : ObservableRvItem(), DiffItem<PolicyRvItem>, ItemWrapper<Policy> {

    override val layoutRes = R.layout.item_policy

    val title get() = if (isSharedUid) "[SharedUID] $appName" else appName

    private inline fun <reified T> setImpl(new: T, old: T, setter: (T) -> Unit) {
        if (old != new) {
            setter(new)
        }
    }

    @get:Bindable
    var isExpanded = false
        set(value) = set(value, field, { field = it }, BR.expanded)

    val showSlider = Config.suRestrict || item.policy == Policy.RESTRICT

    @get:Bindable
    var isEnabled
        get() = item.policy >= Policy.ALLOW
        set(value) = setImpl(value, isEnabled) {
            notifyPropertyChanged(BR.enabled)
            viewModel.updatePolicy(this, if (it) Policy.ALLOW else Policy.DENY)
        }

    @get:Bindable
    var sliderValue
        get() = item.policy
        set(value) = setImpl(value, sliderValue) {
            notifyPropertyChanged(BR.sliderValue)
            notifyPropertyChanged(BR.enabled)
            viewModel.updatePolicy(this, it)
        }

    val sliderValueToPolicyString: (Float) -> Int = { value ->
        when (value.toInt()) {
            1 -> CoreR.string.deny
            2 -> CoreR.string.restrict
            3 -> CoreR.string.grant
            else -> CoreR.string.deny
        }
    }

    @get:Bindable
    var shouldNotify
        get() = item.notification
        private set(value) = setImpl(value, shouldNotify) {
            item.notification = it
            viewModel.updateNotify(this)
        }

    @get:Bindable
    var shouldLog
        get() = item.logging
        private set(value) = setImpl(value, shouldLog) {
            item.logging = it
            viewModel.updateLogging(this)
        }

    fun toggleExpand() {
        isExpanded = !isExpanded
    }

    fun toggleNotify() {
        shouldNotify = !shouldNotify
    }

    fun toggleLog() {
        shouldLog = !shouldLog
    }

    fun revoke() {
        viewModel.deletePressed(this)
    }

    override fun itemSameAs(other: PolicyRvItem) = packageName == other.packageName

    override fun contentSameAs(other: PolicyRvItem) = item.policy == other.item.policy

}
