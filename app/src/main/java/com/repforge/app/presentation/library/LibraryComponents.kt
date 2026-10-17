package com.repforge.app.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.presentation.components.pressScale

internal val MUSCLES = listOf(
    "Peito", "Costas", "Ombros", "Bíceps", "Tríceps", "Antebraço", "Abdômen", "Quadríceps",
    "Posterior de coxa", "Glúteos", "Panturrilha", "Trapézio", "Lombar", "Cardio", "Corpo inteiro"
)
internal val EQUIPMENT = listOf("Barra", "Halteres", "Máquina", "Polia", "Peso corporal", "Kettlebell", "Elástico", "Smith", "Barra W", "Outros")
internal val GUTTER = PaddingValues(horizontal = 16.dp)

/** Filled (High tone) search field, radius 12, leading icon, no outline. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun RfSearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    TextField(
        value, onChange, modifier.fillMaxWidth().padding(GUTTER),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (value.isNotEmpty()) {
            { IconButton({ onChange("") }) { Icon(Icons.Outlined.Close, stringResource(R.string.t3_library_search)) } }
        } else null,
        singleLine = true, shape = MaterialTheme.shapes.small,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = cs.surfaceContainerHigh, unfocusedContainerColor = cs.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
        )
    )
}

/** Horizontally scrolling pill chips; the selected one is accent-filled. Tapping it again clears the filter. */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun PillChipRow(options: List<String>, selected: String?, onSelect: (String?) -> Unit, label: @Composable (String) -> String) {
    val cs = MaterialTheme.colorScheme
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = GUTTER) {
        items(options) { option ->
            val on = selected == option
            Surface(
                selected = on, onClick = { onSelect(if (on) null else option) },
                modifier = Modifier.minimumInteractiveComponentSize().pressScale(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (on) cs.primary else cs.surfaceContainerHigh,
                contentColor = if (on) cs.onPrimary else cs.onSurface
            ) {
                Text(label(option), Modifier.heightIn(min = 20.dp).padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
    }
}
