package su.android.databinding

import android.annotation.SuppressLint
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Creates and binds the view of a [RvItem] on demand, replacing the old layout/dataBinding
 * inflation.
 */
class ItemLayouts {

    private val factories = SparseArray<(LayoutInflater, ViewGroup) -> View>()
    private val binders = SparseArray<(View, RvItem) -> Unit>()

    operator fun put(
        layoutId: Int,
        create: (LayoutInflater, ViewGroup) -> View,
        bind: (View, RvItem) -> Unit
    ) {
        factories.put(layoutId, create)
        binders.put(layoutId, bind)
    }

    fun createView(layoutId: Int, inflater: LayoutInflater, parent: ViewGroup): View {
        val create = factories.get(layoutId)
            ?: throw IllegalArgumentException("No layout registered for id $layoutId")
        return create(inflater, parent)
    }

    fun bindView(view: View, item: RvItem) {
        binders.get(item.layoutRes)?.invoke(view, item)
    }
}

class RvItemAdapter<T : RvItem>(
    val items: List<T>,
    val layouts: ItemLayouts
) : RecyclerView.Adapter<RvItemAdapter.ViewHolder>() {

    private var recyclerView: RecyclerView? = null
    private val observer by lazy(LazyThreadSafetyMode.NONE) { ListObserver<T>() }

    override fun onAttachedToRecyclerView(rv: RecyclerView) {
        recyclerView = rv
        if (items is ObservableList)
            items.addOnListChangedCallback(observer)
    }

    override fun onDetachedFromRecyclerView(rv: RecyclerView) {
        recyclerView = null
        if (items is ObservableList)
            items.removeOnListChangedCallback(observer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, layoutRes: Int): ViewHolder {
        return ViewHolder(
            layouts.createView(layoutRes, LayoutInflater.from(parent.context), parent)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        layouts.bindView(holder.itemView, item)
        recyclerView?.let {
            if (item is ViewAwareItem)
                item.onBind(holder.itemView, it)
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = items[position].layoutRes

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    inner class ListObserver<T : RvItem> : ObservableList.OnListChangedCallback<ObservableList<T>> {

        @SuppressLint("NotifyDataSetChanged")
        override fun onChanged(sender: ObservableList<T>) {
            notifyDataSetChanged()
        }

        override fun onItemRangeChanged(
            sender: ObservableList<T>,
            positionStart: Int,
            itemCount: Int
        ) {
            notifyItemRangeChanged(positionStart, itemCount)
        }

        override fun onItemRangeInserted(
            sender: ObservableList<T>,
            positionStart: Int,
            itemCount: Int
        ) {
            notifyItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeMoved(
            sender: ObservableList<T>,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int
        ) {
            for (i in 0 until itemCount) {
                notifyItemMoved(fromPosition + i, toPosition + i)
            }
        }

        override fun onItemRangeRemoved(
            sender: ObservableList<T>,
            positionStart: Int,
            itemCount: Int
        ) {
            notifyItemRangeRemoved(positionStart, itemCount)
        }
    }
}

inline fun bindExtra(body: (SparseArray<Any?>) -> Unit) = SparseArray<Any?>().also(body)

fun <T : RvItem> RecyclerView.setAdapter(items: List<T>, layouts: ItemLayouts) {
    val rva = (adapter as? RvItemAdapter<*>)
    if (rva == null || rva.items !== items || rva.layouts !== layouts) {
        adapter = RvItemAdapter(items, layouts)
    }
}
