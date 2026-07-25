package su.android.ui.module

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import su.android.R
import su.android.arch.BaseFragment
import su.android.arch.viewModel
import su.android.core.utils.MediaStoreUtils.displayName
import su.android.databinding.FragmentModuleMd2Binding
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addInvalidateItemDecorationsObserver
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import su.android.core.R as CoreR

class ModuleFragment : BaseFragment<FragmentModuleMd2Binding>() {

    override val layoutRes = R.layout.fragment_module_md2
    override val viewModel by viewModel<ModuleViewModel>()

    override fun onStart() {
        super.onStart()
        activity?.title = resources.getString(CoreR.string.modules)
        viewModel.data.observe(this) {
            it ?: return@observe
            val displayName = runCatching { it.displayName }.getOrNull() ?: return@observe
            viewModel.requestInstallLocalModule(it, displayName)
            viewModel.data.value = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.moduleList.apply {
            addEdgeSpacing(top = R.dimen.l_50, bottom = R.dimen.l_50)
            addItemSpacing(R.dimen.l1, R.dimen.l_50, R.dimen.l1)
            fixEdgeEffect()
            post { addInvalidateItemDecorationsObserver() }
        }

        val fab = view.findViewById<View>(R.id.floatingActionButton) ?: view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(com.google.android.material.R.id.floatingActionButton)
        
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomMargin = resources.getDimensionPixelSize(R.dimen.l2) + navBarHeight
            
            binding.moduleList.updatePadding(bottom = bottomMargin + 120)
            
            view.findViewById<View>(android.R.id.content)?.let {
            }
            
            insets
        }
    }

    override fun onPreBind(binding: FragmentModuleMd2Binding) = Unit

}
