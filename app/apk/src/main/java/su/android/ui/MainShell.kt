package su.android.ui

import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import su.android.R
import su.android.core.Info
import su.android.core.model.module.LocalModule
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MainShell(activity: MainActivity) {
    var frame by remember { mutableStateOf<FrameLayout?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = activity.titleText,
                navigationIcon = {
                    if (activity.showBack) {
                        IconButton(onClick = { activity.onBackPressed() }) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!activity.bottomBarHidden) {
                val superuserEnabled = Info.showSuperUser
                val moduleEnabled = Info.env.isActive && LocalModule.loaded()
                NavigationBar {
                    MiuixNavItem(
                        painter = painterResource(R.drawable.ic_home_filled),
                        label = stringResource(R.string.section_home),
                        selected = activity.selectedTabId == R.id.home_fragment,
                        onClick = { activity.navigateToTab(R.id.home_fragment) },
                    )
                    MiuixNavItem(
                        painter = painterResource(R.drawable.ic_superuser_filled),
                        label = stringResource(R.string.superuser),
                        selected = activity.selectedTabId == R.id.superuser_fragment,
                        onClick = { activity.navigateToTab(R.id.superuser_fragment) },
                        enabled = superuserEnabled,
                    )
                    MiuixNavItem(
                        painter = painterResource(R.drawable.ic_module_filled),
                        label = stringResource(R.string.modules),
                        selected = activity.selectedTabId == R.id.modules_fragment,
                        onClick = { activity.navigateToTab(R.id.modules_fragment) },
                        enabled = moduleEnabled,
                    )
                    MiuixNavItem(
                        painter = painterResource(R.drawable.ic_settings_filled),
                        label = stringResource(R.string.settings),
                        selected = activity.selectedTabId == R.id.settings_fragment,
                        onClick = { activity.navigateToTab(R.id.settings_fragment) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (activity.showFab) {
                FloatingActionButton(onClick = activity::scrollHomeToTop) {
                    Icon(
                        painterResource(R.drawable.ic_scroll_top),
                        contentDescription = stringResource(R.string.scroll_to_top),
                    )
                }
            }
        },
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { ctx ->
                frame ?: FrameLayout(ctx).apply {
                    id = R.id.main_nav_host
                }.also {
                    frame = it
                    activity.contentRoot = it
                }
            },
        )
    }

    LaunchedEffect(frame) {
        frame?.doOnAttach {
            activity.attachNavHost()
        }
    }
}

@Composable
private fun RowScope.MiuixNavItem(
    painter: Painter,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val tint = if (selected) {
        MiuixTheme.colorScheme.onSurfaceContainer
    } else {
        MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.5f)
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(tint),
        )
        Spacer(Modifier.height(3.dp))
        BasicText(
            text = label,
            style = TextStyle(
                color = tint,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}