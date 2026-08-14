package su.android.databinding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState

@Composable
fun <T> ObservableHost.observeProp(propId: Int, read: () -> T): State<T> {
    return produceState(read()) {
        val callback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (propertyId == propId || propertyId == 0) {
                    value = read()
                }
            }
        }
        this@observeProp.addOnPropertyChangedCallback(callback)
        awaitDispose { this@observeProp.removeOnPropertyChangedCallback(callback) }
    }
}
