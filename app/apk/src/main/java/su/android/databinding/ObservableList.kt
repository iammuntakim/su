package su.android.databinding

/**
 * A list that notifies its listeners when its contents change.
 * Mirrors androidx.databinding.ObservableList.
 */
interface ObservableList<T> : List<T> {

    interface OnListChangedCallback<L> {
        fun onChanged(sender: L)
        fun onItemRangeChanged(sender: L, positionStart: Int, itemCount: Int)
        fun onItemRangeInserted(sender: L, positionStart: Int, itemCount: Int)
        fun onItemRangeMoved(sender: L, fromPosition: Int, toPosition: Int, itemCount: Int)
        fun onItemRangeRemoved(sender: L, positionStart: Int, itemCount: Int)
    }

    fun addOnListChangedCallback(callback: OnListChangedCallback<out ObservableList<T>>)

    fun removeOnListChangedCallback(callback: OnListChangedCallback<out ObservableList<T>>)
}

/**
 * Dispatches change notifications to the registered [ObservableList.OnListChangedCallback]s.
 * Mirrors androidx.databinding.ListChangeRegistry.
 */
class ListChangeRegistry {

    private val listeners =
        mutableListOf<ObservableList.OnListChangedCallback<ObservableList<*>>>()

    @Synchronized
    fun add(listener: ObservableList.OnListChangedCallback<out ObservableList<*>>) {
        if (listeners.none { it === listener }) {
            @Suppress("UNCHECKED_CAST")
            listeners.add(listener as ObservableList.OnListChangedCallback<ObservableList<*>>)
        }
    }

    @Synchronized
    fun remove(listener: ObservableList.OnListChangedCallback<out ObservableList<*>>) {
        listeners.removeAll { it === listener }
    }

    @Synchronized
    fun clear() = listeners.clear()

    fun notifyChanged(sender: ObservableList<*>) =
        notify(sender) { it.onChanged(sender) }

    fun notifyChanged(sender: ObservableList<*>, positionStart: Int, itemCount: Int) =
        notify(sender) { it.onItemRangeChanged(sender, positionStart, itemCount) }

    fun notifyInserted(sender: ObservableList<*>, positionStart: Int, itemCount: Int) =
        notify(sender) { it.onItemRangeInserted(sender, positionStart, itemCount) }

    fun notifyMoved(
        sender: ObservableList<*>,
        fromPosition: Int,
        toPosition: Int,
        itemCount: Int
    ) = notify(sender) { it.onItemRangeMoved(sender, fromPosition, toPosition, itemCount) }

    fun notifyRemoved(sender: ObservableList<*>, positionStart: Int, itemCount: Int) =
        notify(sender) { it.onItemRangeRemoved(sender, positionStart, itemCount) }

    private fun notify(
        sender: ObservableList<*>,
        invoke: (ObservableList.OnListChangedCallback<ObservableList<*>>) -> Unit
    ) {
        val snapshot: List<ObservableList.OnListChangedCallback<ObservableList<*>>>
        synchronized(this) {
            snapshot = listeners.toList()
        }
        snapshot.forEach { invoke(it) }
    }
}
