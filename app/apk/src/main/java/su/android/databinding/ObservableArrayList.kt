package su.android.databinding

import java.util.ArrayList

/**
 * An [ArrayList] that notifies its listeners whenever its contents change.
 * Mirrors androidx.databinding.ObservableArrayList.
 */
class ObservableArrayList<T> : ArrayList<T>(), ObservableList<T> {

    private val listeners = ListChangeRegistry()

    override fun addOnListChangedCallback(listener: OnListChangedCallback<out ObservableList<T>>) {
        listeners.add(listener)
    }

    override fun removeOnListChangedCallback(listener: OnListChangedCallback<out ObservableList<T>>) {
        listeners.remove(listener)
    }

    override fun add(element: T): Boolean {
        super.add(element)
        listeners.notifyInserted(this, size - 1, 1)
        return true
    }

    override fun add(index: Int, element: T) {
        super.add(index, element)
        listeners.notifyInserted(this, index, 1)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val count = elements.size
        val start = size
        val result = super.addAll(elements)
        if (result)
            listeners.notifyInserted(this, start, count)
        return result
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val count = elements.size
        val result = super.addAll(index, elements)
        if (result)
            listeners.notifyInserted(this, index, count)
        return result
    }

    override fun clear() {
        val count = size
        super.clear()
        if (count > 0)
            listeners.notifyRemoved(this, 0, count)
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        if (index >= 0) {
            removeAt(index)
            return true
        }
        return false
    }

    override fun removeAt(index: Int): T {
        val result = super.removeAt(index)
        listeners.notifyRemoved(this, index, 1)
        return result
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var index = size - 1
        var changed = false
        while (index >= 0) {
            if (elements.contains(get(index))) {
                removeAt(index)
                changed = true
            }
            index--
        }
        return changed
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        var index = size - 1
        var changed = false
        while (index >= 0) {
            if (!elements.contains(get(index))) {
                removeAt(index)
                changed = true
            }
            index--
        }
        return changed
    }

    override fun set(index: Int, element: T): T {
        val result = super.set(index, element)
        listeners.notifyChanged(this, index, 1)
        return result
    }
}
