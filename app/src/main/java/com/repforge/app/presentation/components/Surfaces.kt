package com.repforge.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repforge.app.presentation.theme.RepForgeTheme

/** Screen large title with optional muted subtitle and a trailing action slot (e.g. a CircleIconButton). */
@Composable
fun LargeTitleHeader(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

/** Borderless, shadowless card on the surfaceContainer tone; clickable (with press scale) when [onClick] is set. */
@Composable
fun RfCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val color = MaterialTheme.colorScheme.surfaceContainer
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.pressScale(), shape = shape, color = color) { Box(Modifier.padding(padding)) { content() } }
    } else {
        Surface(modifier = modifier, shape = shape, color = color) { Box(Modifier.padding(padding)) { content() } }
    }
}

/** Muted section header text (bodySmall) with an optional trailing slot such as a SoftPillButton. */
@Composable
fun SectionHeader(text: String, trailing: (@Composable () -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing?.invoke()
    }
}

/** 0.5dp outlineVariant divider; [inset] is the start padding (use 16 + icon + 12 to clear a leading icon). */
@Composable
fun HairlineDivider(inset: Dp = 0.dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(start = inset).height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/** Visual tone of a [Tag]. */
enum class TagTone { Neutral, Accent, Goal }

/** Small rounded label: Neutral (grey), Accent (soft accent pill) or Goal (yellow). */
@Composable
fun Tag(text: String, tone: TagTone = TagTone.Neutral, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        TagTone.Neutral -> cs.surfaceContainerHigh to cs.onSurfaceVariant
        TagTone.Accent -> cs.primaryContainer to cs.onPrimaryContainer
        TagTone.Goal -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
    Surface(modifier, shape = MaterialTheme.shapes.small, color = bg) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = fg, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** 40dp accent-filled rounded square (radius 10) with a contrasting glyph; decorative. */
@Composable
fun GlyphSquare(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp)) }
}

/** Centered icon, title, body and optional action slot for empty lists. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) Box(Modifier.padding(top = 8.dp)) { action() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SurfacesPreview() = RepForgeTheme(darkTheme = true) {
    Column(Modifier.padding(PaddingValues(bottom = 16.dp)), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LargeTitleHeader("RepForge", "Saturday", action = { CircleIconButton(Icons.Outlined.Settings, "Settings", {}) })
        RfCard(Modifier.padding(horizontal = 16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) { GlyphSquare(Icons.Outlined.FitnessCenter); Tag("Push", TagTone.Accent); Tag("Goal", TagTone.Goal) }
                HairlineDivider(inset = 52.dp)
                SectionHeader("Routines", trailing = { SoftPillButton("New", null, {}) })
            }
        }
        EmptyState(Icons.Outlined.FitnessCenter, "Nothing here", "Add your first routine.")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun SurfacesLightPreview() = RepForgeTheme(darkTheme = false) {
    RfCard(Modifier.padding(16.dp)) { Tag("Rest", TagTone.Neutral) }
}
