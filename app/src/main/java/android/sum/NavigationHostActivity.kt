package android.sum

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import android.sum.R
import android.sum.HomeDashboardFragment
import android.sum.ModuleManagerFragment
import android.sum.SuperuserFragment
import android.sum.AppSettingsFragment
import android.sum.MaterialComponents
import android.sum.VectorDrawableFactory

class NavigationHostActivity : AppCompatActivity() {

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var bottomNav: com.google.android.material.bottomnavigation.BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val root = buildLayout()
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, 0)
            bottomNav.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_vault -> VaultFragment()
                R.id.nav_shield -> ShieldFragment()
                R.id.nav_preferences -> PreferencesFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_dashboard
        }
    }

    private fun buildLayout(): LinearLayout {
        val ctx = this

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        val fragmentContainer = FrameLayout(ctx).apply {
            id = R.id.fragment_container
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }

        bottomNav = com.google.android.material.bottomnavigation.BottomNavigationView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.WHITE)
            elevation = 12f * ctx.resources.displayMetrics.density
            labelVisibilityMode = NavigationBarView.LABELVisibilityMode_SELECTED
            itemIconTintList = MaterialColors.getColorStateList(
                this,
                com.google.android.material.R.attr.colorPrimary,
                Color.parseColor("#1A73E8")
            )
            itemTextColor = MaterialColors.getColorStateList(
                this,
                com.google.android.material.R.attr.colorOnSurface,
                Color.parseColor("#5F6368")
            )

            menu.add(0, R.id.nav_dashboard, 0, "Dashboard").apply {
                icon = DrawableFactory.shieldIcon(context)
            }
            menu.add(0, R.id.nav_vault, 1, "Vault").apply {
                icon = DrawableFactory.extensionIcon(context)
            }
            menu.add(0, R.id.nav_shield, 2, "Shield").apply {
                icon = DrawableFactory.superuserIcon(context)
            }
            menu.add(0, R.id.nav_preferences, 3, "Settings").apply {
                icon = DrawableFactory.settingsGear(context)
            }
        }

        root.addView(fragmentContainer)
        root.addView(bottomNav)

        return root
    }
}
