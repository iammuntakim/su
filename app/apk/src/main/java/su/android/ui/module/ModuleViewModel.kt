package su.android.ui.module

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import su.android.databinding.Bindable
import su.android.BR
import su.android.MainDirections
import su.android.R
import su.android.arch.AsyncLoadViewModel
import su.android.core.Const
import su.android.core.Info
import su.android.core.base.ContentResultCallback
import su.android.core.model.module.LocalModule
import su.android.core.model.module.OnlineModule
import su.android.databinding.MergeObservableList
import su.android.databinding.RvItem
import su.android.databinding.bindExtra
import su.android.databinding.filterList
import su.android.databinding.set
import su.android.dialog.LocalModuleInstallDialog
import su.android.dialog.OnlineModuleInstallDialog
import su.android.events.GetContentEvent
import su.android.events.SnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import su.android.R as CoreR

class ModuleViewModel : AsyncLoadViewModel() {

    val bottomBarBarrierIds = intArrayOf(R.id.module_update, R.id.module_remove)

    private val itemsInstalled = filterList<LocalModuleRvItem>(viewModelScope)

    private var allModules: List<LocalModuleRvItem> = emptyList()

    val items = MergeObservableList<RvItem>().insertList(itemsInstalled)

    var searchQuery: String = ""
        set(value) {
            if (field == value) return
            field = value
            itemsInstalled.filter(::matchesQuery)
        }

    private fun matchesQuery(item: LocalModuleRvItem): Boolean {
        val s = searchQuery
        if (s.isEmpty()) return true
        return item.item.name.contains(s, true) ||
            item.item.author.contains(s, true) ||
            item.item.description.contains(s, true)
    }
    val extraBindings = bindExtra {
        it.put(BR.viewModel, this)
    }

    val data get() = uri

    @get:Bindable
    var loading = true
        private set(value) = set(value, field, { field = it }, BR.loading)

    override suspend fun doLoadWork() {
        loading = true
        val moduleLoaded = Info.env.isActive &&
                withContext(Dispatchers.IO) { LocalModule.loaded() }
        if (moduleLoaded) {
            loadInstalled()
        }
        loading = false
        loadUpdateInfo()
    }

    override fun onNetworkChanged(network: Boolean) = startLoading()

    private suspend fun loadInstalled() {
        val installed = withContext(Dispatchers.Default) {
            LocalModule.installed().map { LocalModuleRvItem(it) }
        }
        allModules = installed
        itemsInstalled.set(installed)
    }

    private suspend fun loadUpdateInfo() {
        withContext(Dispatchers.IO) {
            allModules.forEach {
                if (it.item.fetch())
                    it.fetchedUpdateInfo()
            }
        }
    }

    fun downloadPressed(item: OnlineModule?) =
        if (item != null && Info.isConnected.value == true) {
            withExternalRW { OnlineModuleInstallDialog(item).show() }
        } else {
            SnackbarEvent(CoreR.string.no_connection).publish()
        }

    fun installPressed() = withExternalRW {
        GetContentEvent("application/zip", UriCallback()).publish()
    }

    fun requestInstallLocalModule(uri: Uri, displayName: String) {
        LocalModuleInstallDialog(this, uri, displayName).show()
    }

    @Parcelize
    class UriCallback : ContentResultCallback {
        override fun onActivityResult(result: Uri) {
            uri.value = result
        }
    }

    fun runAction(id: String, name: String) {
        MainDirections.actionActionFragment(id, name).navigate()
    }

    companion object {
        private val uri = MutableLiveData<Uri?>()
    }
}
