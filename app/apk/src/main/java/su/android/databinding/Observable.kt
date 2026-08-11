package su.android.databinding

/**
 * A data holder that can be observed for property changes.
 */
interface Observable {

    fun addOnPropertyChangedCallback(callback: OnPropertyChangedCallback)

    fun removeOnPropertyChangedCallback(callback: OnPropertyChangedCallback)

    abstract class OnPropertyChangedCallback {
        abstract fun onPropertyChanged(sender: Observable?, propertyId: Int)
    }
}

/**
 * Tracks the observers of an [Observable] and notifies them when a property changes.
 */
class PropertyChangeRegistry {

    private val listeners = mutableListOf<Observable.OnPropertyChangedCallback>()

    @Synchronized
    fun add(listener: Observable.OnPropertyChangedCallback) {
        if (listeners.none { it === listener }) {
            listeners.add(listener)
        }
    }

    @Synchronized
    fun remove(listener: Observable.OnPropertyChangedCallback) {
        listeners.removeAll { it === listener }
    }

    @Synchronized
    fun clear() = listeners.clear()

    fun notifyCallbacks(sender: Observable?, propertyId: Int, payload: Any? = null) {
        val snapshot: List<Observable.OnPropertyChangedCallback>
        synchronized(this) {
            snapshot = listeners.toList()
        }
        snapshot.forEach { it.onPropertyChanged(sender, propertyId) }
    }
}
