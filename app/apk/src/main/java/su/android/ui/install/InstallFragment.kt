package su.android.ui.install

import su.android.R
import su.android.arch.BaseFragment
import su.android.arch.viewModel
import su.android.databinding.FragmentInstallBinding
import su.android.R as CoreR

class InstallFragment : BaseFragment<FragmentInstallBinding>() {

    override val layoutRes = R.layout.fragment_install
    override val viewModel by viewModel<InstallViewModel>()

    override fun onStart() {
        super.onStart()
        requireActivity().setTitle(CoreR.string.install)
    }
}
