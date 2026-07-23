package su.android.ui.home

import su.android.R
import su.android.databinding.RvItem
import su.android.core.R as CoreR

interface Dev {
    val name: String
}

private interface MAImpl : Dev {
    override val name get() = "mikailamin"
}

private interface MuntakimImpl : Dev {
    override val name get() = "iammuntakim"
}

sealed class DeveloperItem : Dev {

    abstract val items: List<IconLink>
    val handle get() = "@${name}"

    object MIKAILAMIN : DeveloperItem(), MAImpl {
        override val items =
            listOf(
                object : IconLink.Github.User() { override val name = "promikailamin" }
            )
    }

    object MUNTAKIM : DeveloperItem(), MuntakimImpl {
        override val items =
            listOf(
                object : IconLink.Github.User(), MuntakimImpl {},
                IconLink.Source
            )
    }
}

sealed class IconLink : RvItem() {

    abstract val icon: Int
    abstract val title: Int
    abstract val link: String

    override val layoutRes get() = R.layout.item_icon_link

    abstract class Github : IconLink() {
        override val icon get() = CoreR.drawable.ic_github
        override val title get() = CoreR.string.github

        abstract class User : Github(), Dev {
            override val link get() = "https://github.com/$name"
        }
    }

    object Source : IconLink() {
        override val icon get() = R.drawable.ic_code
        override val title get() = CoreR.string.github
        override val link get() = "https://github.com/iammuntakim/su"
    }
}
