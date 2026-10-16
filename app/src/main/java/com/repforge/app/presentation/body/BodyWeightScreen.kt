package com.repforge.app.presentation.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.repforge.app.R
import com.repforge.app.domain.validation.AppError
import com.repforge.app.presentation.res
import com.repforge.app.domain.logging.formatKg
import com.repforge.app.domain.model.BodyWeight
import com.repforge.app.domain.validation.parseWeightKg
import com.repforge.app.domain.validation.validateBodyWeight
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightScreen(viewModel: BodyWeightViewModel, onBack: () -> Unit) {
    val weights by viewModel.weights.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<AppError?>(null) }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.body_weight)) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        input, { input = it; error = null }, Modifier.weight(1f), label = { Text(stringResource(R.string.weight_kg)) }, singleLine = true,
                        isError = error != null, supportingText = error?.let { { Text(stringResource(it.res())) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Button(
                        onClick = {
                            val kg = parseWeightKg(input)
                            error = validateBodyWeight(kg)
                            if (error == null) { viewModel.add(kg!!); input = "" }
                        },
                        Modifier.padding(top = 8.dp).height(48.dp)
                    ) { Text(stringResource(R.string.log)) }
                }
            }
            if (weights.size >= 2) item { WeightChart(weights.asReversed(), dateFormat) }
            if (weights.isEmpty()) item { Text(stringResource(R.string.no_records_yet), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(weights, key = { it.id }) { w ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatKg(w.weightKg), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(dateFormat.format(Date(w.recordedAt)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { viewModel.delete(w.id) }) { Icon(Icons.Rounded.Delete, stringResource(R.string.remove_record_of, formatKg(w.weightKg))) }
                    }
                }
            }
        }
    }
}

/** [chronological] oldest first, at least two points. */
@Composable
private fun WeightChart(chronological: List<BodyWeight>, dateFormat: DateFormat) {
    val line = MaterialTheme.colorScheme.primary
    val first = chronological.first()
    val last = chronological.last()
    val description = stringResource(
        R.string.body_chart_description,
        formatKg(first.weightKg), dateFormat.format(Date(first.recordedAt)),
        formatKg(last.weightKg), dateFormat.format(Date(last.recordedAt)),
        pluralStringResource(R.plurals.records_count, chronological.size, chronological.size),
        formatKg(chronological.minOf { it.weightKg }), formatKg(chronological.maxOf { it.weightKg })
    )
    Card {
        Canvas(Modifier.fillMaxWidth().height(160.dp).padding(16.dp).semantics { contentDescription = description }) {
            val minKg = chronological.minOf { it.weightKg }
            val spanKg = (chronological.maxOf { it.weightKg } - minKg).coerceAtLeast(0.5)
            val minT = first.recordedAt
            val spanT = (last.recordedAt - minT).coerceAtLeast(1L)
            // Points are placed by time, so uneven logging gaps stay honest.
            val points = chronological.map {
                Offset(size.width * (it.recordedAt - minT) / spanT, size.height * (1f - ((it.weightKg - minKg) / spanKg).toFloat()))
            }
            val path = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            points.forEach { drawCircle(line, radius = 4.dp.toPx(), center = it) }
        }
    }
}
