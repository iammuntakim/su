package su.android.ui.settings

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import su.android.R
import su.android.arch.BaseFragment
import su.android.arch.viewModel
import su.android.databinding.FragmentSettingsBinding
import su.android.databinding.ItemSettingsBinding
import su.android.databinding.ItemSettingsCardBinding
import su.android.core.R as CoreR

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    override val layoutRes = R.layout.fragment_settings
    override val viewModel by viewModel<SettingsViewModel>()
    override val snackbarView: View get() = binding.snackbarContainer

    override fun onStart() {
        super.onStart()
        activity?.title = resources.getString(CoreR.string.settings)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateCards()

        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsScrollView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val tv = TypedValue()
            val actionBarSize = if (requireContext().theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
            } else 0

            val extraBottom = (80 * resources.displayMetrics.density).toInt()

            v.updatePadding(
                top = systemBars.top + actionBarSize,
                bottom = systemBars.bottom + extraBottom
            )

            insets
        }
    }

    private fun populateCards() {
        val rootContainer = binding.settingsContainer
        rootContainer.removeAllViews()

        val cardMargin = resources.getDimensionPixelSize(R.dimen.l1)
        val handler = viewModel as? BaseSettingsItem.Handler

        viewModel.items.forEach { group ->
            if (group is SettingsGroupItem.CardGroup) {
                val cardBinding = DataBindingUtil.inflate<ItemSettingsCardBinding>(
                    layoutInflater,
                    R.layout.item_settings_card,
                    rootContainer,
                    false
                )

                group.children.forEach { childItem ->
                    val itemBinding = DataBindingUtil.inflate<ItemSettingsBinding>(
                        layoutInflater,
                        R.layout.item_settings,
                        cardBinding.cardContainer,
                        false
                    )

                    itemBinding.item = childItem
                    itemBinding.handler = handler
                    itemBinding.lifecycleOwner = viewLifecycleOwner

                    cardBinding.cardContainer.addView(itemBinding.root)
                }

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, cardMargin / 2, 0, cardMargin / 2)
                }

                rootContainer.addView(cardBinding.root, lp)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.items.forEach { group ->
            if (group is SettingsGroupItem.CardGroup) {
                group.children.forEach { it.refresh() }
            } else {
                group.refresh()
            }
        }
    }

    override fun onPreBind(binding: FragmentSettingsBinding) = Unit
}
