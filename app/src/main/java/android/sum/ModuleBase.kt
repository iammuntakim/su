package android.sum

abstract class ModuleBase : Comparable<LocalModule> {
    abstract var id: String
        protected set
    abstract var name: String
        protected set
    abstract var version: String
        protected set
    abstract var versionCode: Int
        protected set

    override operator fun compareTo(other: LocalModule) = id.compareTo(other.id)
}
