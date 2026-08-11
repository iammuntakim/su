package su.android.databinding

/**
 * Marks a getter/setter pair whose property changes are reported through
 * [ObservableHost.notifyPropertyChanged]. Mirrors androidx.databinding.Bindable.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Bindable
