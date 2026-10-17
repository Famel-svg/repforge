package com.repforge.app.presentation.workout

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.presentation.components.SoftPillButton

/** True when the system animator scale is 0 (reduced motion); entrance animations then snap. */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** Floating rest timer: huge mm:ss (tap to pause/resume), progress, +15s and a filled Skip. */
@Composable
internal fun RestTimerCard(
    visible: Boolean,
    remaining: Int,
    total: Int,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onHeight: (Int) -> Unit = {}
) {
    val reduced = rememberReducedMotion()
    AnimatedVisibility(
        visible, modifier,
        enter = if (reduced) EnterTransition.None else fadeIn() + slideInVertically { it / 2 },
        exit = if (reduced) ExitTransition.None else fadeOut() + slideOutVertically { it / 2 }
    ) {
        val cs = MaterialTheme.colorScheme
        val time = "%02d:%02d".format(remaining / 60, remaining % 60)
        val description = stringResource(R.string.t4_rest_toggle_cd, time)
        val compact = LocalConfiguration.current.screenHeightDp < 480
        // The outer modifier (alignment + margins) is already applied by AnimatedVisibility; only measure here.
        Surface(Modifier.fillMaxWidth().onSizeChanged { onHeight(it.height) }, shape = MaterialTheme.shapes.large, color = cs.surfaceContainerHigh) {
            if (compact) Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rest), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    Text(
                        time, Modifier.weight(1f).heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically)
                            .clickable(role = Role.Button, onClick = onToggle).semantics { contentDescription = description },
                        style = MaterialTheme.typography.headlineMedium, color = cs.onSurface
                    )
                    SoftPillButton(stringResource(R.string.t4_rest_add), onClick = onAdd)
                    Button(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp), shape = CircleShape) {
                        Text(stringResource(R.string.t4_rest_skip), style = MaterialTheme.typography.labelLarge)
                    }
                }
                ProgressBar((remaining / total.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f))
            } else Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.rest), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(
                    time, Modifier.clickable(role = Role.Button, onClick = onToggle).semantics { contentDescription = description },
                    style = MaterialTheme.typography.displayLarge, color = cs.onSurface, textAlign = TextAlign.Center
                )
                ProgressBar((remaining / total.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SoftPillButton(stringResource(R.string.t4_rest_add), onClick = onAdd)
                    Button(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp), shape = CircleShape) {
                        Text(stringResource(R.string.t4_rest_skip), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** Thin rounded progress bar (3dp by default) in the accent colour over a Highest-tone track. */
@Composable
internal fun ProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 3.dp) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(cs.surfaceContainerHighest)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(cs.primary))
    }
}
