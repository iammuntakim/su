package su.android.ui.install

import su.android.R
import su.android.arch.BaseFragment
import su.android.arch.viewModel
import su.android.databinding.FragmentInstallBinding
import su.android.core.R as CoreR

class InstallFragment : BaseFragment<FragmentInstallBinding>() {

    override val layoutRes = R.layout.FragmentInstall
    override val viewModel by viewModel<InstallViewModel>()

    override fun onStart() {
        super.onStart()
        requireActivity().setTitle(CoreR.string.install)
    }
}
