package su.android.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootContainer) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.rootContainer.updatePadding(
                top = bars.top
            )
            binding.settingsScrollView.updatePadding(
                bottom = bars.bottom
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll()
    }

    override fun onPreBind(binding: FragmentSettingsMd2Binding) = Unit
}
