package su.android.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import su.android.R
import su.android.arch.BaseFragment
import su.android.arch.viewModel
import su.android.databinding.FragmentSettingsMd2Binding
import su.android.core.R as CoreR

class SettingsFragment : BaseFragment<FragmentSettingsMd2Binding>() {

    override val layoutRes = R.layout.fragment_settings_md2
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

        binding.settingsList.clipToPadding = false
        binding.settingsList.apply {
            addEdgeSpacing(top = R.dimen.l_50, bottom = R.dimen.l_50)
            addItemSpacing(R.dimen.l1, R.dimen.l_50, R.dimen.l1)
            fixEdgeEffect()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val extraBottom = (50 * resources.displayMetrics.density).toInt()

            binding.root.updatePadding(
                top = systemBars.top
            )

            binding.settingsList.updatePadding(
                bottom = extraBottom + navBars.bottom
            )

            insets
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.items.forEach { it.refresh() }
    }

    override fun onPreBind(binding: FragmentSettingsMd2Binding) = Unit
}
