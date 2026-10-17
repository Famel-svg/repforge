package com.repforge.app.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.presentation.components.HairlineDivider
import com.repforge.app.presentation.components.pressScale

internal enum class Destination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home("home", R.string.t2_tab_home, Icons.Outlined.Home),
    Plan("plan", R.string.t2_tab_plan, Icons.Outlined.CalendarMonth),
    Progress("progress", R.string.t2_tab_progress, Icons.Outlined.Timeline),
    Library("library", R.string.t2_tab_library, Icons.AutoMirrored.Outlined.MenuBook)
}

/** Five slots: Home, Plan, center Start/Resume, Progress, Library. [currentRoute] selects a tab (none for Settings). */
@Composable
internal fun RepForgeBottomBar(currentRoute: String?, resuming: Boolean, onTab: (Destination) -> Unit, onStart: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val accent = if (resuming) cs.secondary else cs.primary
    val onAccent = if (resuming) cs.onSecondary else cs.onPrimary
    Column(Modifier.background(cs.surfaceContainerLow).navigationBarsPadding()) {
        HairlineDivider()
        Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
            val tab = @Composable { d: Destination, m: Modifier -> TabItem(d, currentRoute == d.route, m) { onTab(d) } }
            tab(Destination.Home, Modifier.weight(1f))
            tab(Destination.Plan, Modifier.weight(1f))
            Box(Modifier.weight(1f).height(80.dp)) {
                Column(
                    Modifier.align(Alignment.Center).offset(y = (-2).dp).pressScale()
                        .selectable(selected = false, role = Role.Button, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onStart() }),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(56.dp)
                            .drawBehind { drawCircle(accent.copy(alpha = 0.25f), radius = size.minDimension / 2 + 5.dp.toPx()) }
                            .background(accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(if (resuming) Icons.Rounded.PlayArrow else Icons.Rounded.FitnessCenter, contentDescription = null, tint = onAccent, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.height(8.dp)) // clears the 5dp halo ring
                    Text(stringResource(if (resuming) R.string.t2_tab_resume else R.string.t2_tab_start), style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
            tab(Destination.Progress, Modifier.weight(1f))
            tab(Destination.Library, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(d: Destination, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.height(80.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(d.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(stringResource(d.label), style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}
