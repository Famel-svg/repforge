package com.repforge.app.presentation.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.R
import com.repforge.app.domain.logging.formatKg
import com.repforge.app.domain.metrics.WeightRange
import com.repforge.app.domain.metrics.filterWeights
import com.repforge.app.domain.metrics.labelIndices
import com.repforge.app.domain.validation.AppError
import com.repforge.app.domain.validation.parseWeightKg
import com.repforge.app.domain.validation.validateBodyWeight
import com.repforge.app.presentation.components.AreaLineChart
import com.repforge.app.presentation.components.CircleIconButton
import com.repforge.app.presentation.components.HairlineDivider
import com.repforge.app.presentation.components.LargeTitleHeader
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SegmentedPill
import com.repforge.app.presentation.res
import java.text.DateFormat
import java.time.ZoneId
import java.util.Date

@Composable
fun BodyWeightScreen(viewModel: BodyWeightViewModel, onBack: () -> Unit) {
    val weights by viewModel.weights.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var rangeIndex by rememberSaveable { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<AppError?>(null) }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    val ranges = WeightRange.entries
    val rangeLabels = listOf(R.string.t5_range_30d, R.string.t5_range_90d, R.string.t5_range_1y, R.string.range_all).map { stringResource(it) }
    val shown = filterWeights(weights.asReversed(), ranges[rangeIndex], System.currentTimeMillis(), ZoneId.systemDefault())

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LargeTitleHeader(stringResource(R.string.body_weight), action = {
                    CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onBack)
                })
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    TextField(
                        input, { input = it; error = null }, Modifier.weight(1f), label = { Text(stringResource(R.string.weight_kg)) }, singleLine = true,
                        isError = error != null, supportingText = error?.let { { Text(stringResource(it.res())) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            errorIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        )
                    )
                    Button(
                        onClick = {
                            val kg = parseWeightKg(input)
                            error = validateBodyWeight(kg)
                            if (error == null) { viewModel.add(kg!!); input = "" }
                        },
                        Modifier.padding(top = 4.dp).height(56.dp)
                    ) { Text(stringResource(R.string.log)) }
                }
            }
            if (weights.isNotEmpty()) item {
                RfCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SegmentedPill(rangeLabels, rangeIndex, { rangeIndex = it }, Modifier.fillMaxWidth())
                        if (shown.isEmpty()) {
                            Text(stringResource(R.string.t5_weight_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            AreaLineChart(
                                shown.map { it.weightKg.toFloat() }, goal?.toFloat(),
                                labelIndices(shown.size).map { dateFormat.format(Date(shown[it].recordedAt)) }
                            )
                        }
                    }
                }
            }
            if (weights.isEmpty()) item { Text(stringResource(R.string.no_records_yet), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (weights.isNotEmpty()) item {
                RfCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                    androidx.compose.foundation.layout.Column {
                        weights.forEachIndexed { i, w ->
                            if (i > 0) HairlineDivider(inset = 16.dp)
                            Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(formatKg(w.weightKg), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Text(dateFormat.format(Date(w.recordedAt)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(onClick = { viewModel.delete(w.id) }) {
                                    Icon(Icons.Outlined.Delete, stringResource(R.string.remove_record_of, formatKg(w.weightKg)), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
