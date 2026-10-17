package com.repforge.app.presentation.components

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.presentation.theme.RepForgeTheme

/** Scales to 0.97 while pressed (150ms ease-out); no animation when the system animator scale is 0. Does not consume touches. */
fun Modifier.pressScale(): Modifier = composed {
    val context = LocalContext.current
    val reduced = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, if (reduced) snap() else tween(150), label = "pressScale")
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
}

/** 40dp circular icon button on the card tone with a 48dp touch target; [cd] is the content description. */
@Composable
fun CircleIconButton(icon: ImageVector, cd: String, onClick: () -> Unit, tint: Color? = null, modifier: Modifier = Modifier) {
    Box(
        modifier.size(48.dp).pressScale().clickable(role = Role.Button, onClickLabel = cd, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = cd, tint = tint ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        }
    }
}

/** Soft accent pill button (primaryContainer) with optional leading icon; 48dp touch target. */
@Composable
fun SoftPillButton(text: String, icon: ImageVector? = null, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier.minimumInteractiveComponentSize().pressScale(), shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Compact "- value +" stepper on the High tone; [value] is pre-formatted, tapping it calls [onEdit]. */
@Composable
fun PillStepper(value: String, onDec: () -> Unit, onInc: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.height(48.dp).clip(MaterialTheme.shapes.small).background(cs.surfaceContainerHigh), verticalAlignment = Alignment.CenterVertically) {
        StepperButton(Icons.Outlined.Remove, stringResource(R.string.t1_decrease), onDec)
        Box(
            Modifier.widthIn(min = 48.dp).fillMaxHeight().clickable(role = Role.Button, onClickLabel = stringResource(R.string.t1_edit_value), onClick = onEdit).padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) { Text(value, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, textAlign = TextAlign.Center) }
        StepperButton(Icons.Outlined.Add, stringResource(R.string.t1_increase), onInc)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, cd: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = cd, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

/** Pill segmented control: Highest-tone track, card-tone thumb on the selected option. */
@Composable
fun SegmentedPill(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.height(44.dp).clip(CircleShape).background(cs.surfaceContainerHighest).padding(3.dp).selectableGroup()) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                    .background(if (on) cs.surfaceContainer else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) cs.onSurface else cs.onSurface.copy(alpha = 0.8f), maxLines = 1)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ControlsPreview() = RepForgeTheme(darkTheme = true) {
    androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Outlined.Settings, "Settings", {})
            SoftPillButton("New", Icons.Outlined.Add, {})
        }
        PillStepper("62.5", {}, {}, {})
        var sel by remember { mutableStateOf(1) }
        SegmentedPill(listOf("1M", "3M", "1Y", "All"), sel, { sel = it }, Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun ControlsLightPreview() = RepForgeTheme(darkTheme = false) {
    SegmentedPill(listOf("1M", "3M", "1Y"), 0, {}, Modifier.padding(16.dp).fillMaxWidth())
}
