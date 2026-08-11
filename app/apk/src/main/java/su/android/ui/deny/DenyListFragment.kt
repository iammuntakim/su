package su.android.ui.deny

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.RecyclerView
import su.android.R
import su.android.arch.BaseFragment
import su.android.arch.viewModel
import su.android.core.ktx.hideKeyboard
import su.android.databinding.FragmentDenyBinding
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import su.android.core.R as CoreR

class DenyListFragment : BaseFragment<FragmentDenyBinding>(), MenuProvider {

    override val layoutRes = R.layout.FragmentDeny
    override val viewModel by viewModel<DenyListViewModel>()

    private lateinit var searchView: SearchView

    override fun onStart() {
        super.onStart()
        activity?.setTitle(CoreR.string.denylist)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        binding.appList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) activity?.hideKeyboard()
            }
        })

        binding.appList.apply {
            addEdgeSpacing(top = R.dimen.l_50, bottom = R.dimen.l1)
            addItemSpacing(R.dimen.l1, R.dimen.l_50, R.dimen.l1)
            fixEdgeEffect()
        }
    }

    override fun onPreBind(binding: FragmentDenyBinding) = Unit

    override fun onBackPressed(): Boolean {
        if (::searchView.isInitialized && searchView.isIconfiedByDefault && !searchView.isIconified) {
            searchView.isIconified = true
            return true
        }
        return super.onBackPressed()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.MenuDeny, menu)
        searchView = menu.findItem(R.id.ActionSearch).actionView as SearchView
        searchView.queryHint = searchView.context.getString(CoreR.string.hide_filter_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.query = query ?: ""
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.query = newText ?: ""
                return true
            }
        })
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ActionShowSystem -> {
                val check = !item.isChecked
                viewModel.isShowSystem = check
                item.isChecked = check
                return true
            }
            R.id.ActionShowOS -> {
                val check = !item.isChecked
                viewModel.isShowOS = check
                item.isChecked = check
                return true
            }
        }
        return false
    }

    override fun onPrepareMenu(menu: Menu) {
        val showSystem = menu.findItem(R.id.ActionShowSystem)
        val showOS = menu.findItem(R.id.ActionShowOS)
        if (showSystem != null && showOS != null) {
            showOS.isEnabled = showSystem.isChecked
        }
    }
}
