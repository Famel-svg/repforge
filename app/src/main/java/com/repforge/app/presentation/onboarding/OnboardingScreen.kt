package com.repforge.app.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import kotlinx.coroutines.launch

private data class Page(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

private val PAGES = listOf(
    Page(Icons.Rounded.CloudOff, R.string.onb1_title, R.string.onb1_body),
    Page(Icons.Rounded.TouchApp, R.string.onb2_title, R.string.onb2_body),
    Page(Icons.Rounded.Timeline, R.string.onb3_title, R.string.onb3_body)
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onFinishWithTemplate: () -> Unit = onFinish) {
    val pager = rememberPagerState { PAGES.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.lastIndex
    val cs = MaterialTheme.colorScheme
    val pill = ButtonDefaults.buttonColors()
    Column(Modifier.fillMaxSize().background(cs.background).safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!last) TextButton(onClick = onFinish) { Text(stringResource(R.string.skip), color = cs.onSurfaceVariant) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            val page = PAGES[index]
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(72.dp).background(cs.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(page.icon, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(32.dp))
                Text(stringResource(page.title), style = MaterialTheme.typography.displaySmall, color = cs.onSurface, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(12.dp))
                Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
            }
        }
        val dotsLabel = stringResource(R.string.step_x_of_y, pager.currentPage + 1, PAGES.size)
        Row(
            Modifier.align(Alignment.CenterHorizontally).semantics { contentDescription = dotsLabel },
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(PAGES.size) { i ->
                Box(Modifier.size(8.dp).background(if (i == pager.currentPage) cs.primary else cs.surfaceContainerHighest, CircleShape))
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = CircleShape, colors = pill,
        ) { Text(stringResource(if (last) R.string.get_started else R.string.next), style = MaterialTheme.typography.labelLarge) }
        // Slot is always laid out (invisible before the last page) so pages keep the same content height.
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onFinishWithTemplate, enabled = last,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).alpha(if (last) 1f else 0f).clearAndSetSemantics {}
        ) {
            Text(stringResource(R.string.start_with_template), style = MaterialTheme.typography.labelLarge, color = cs.primary)
        }
    }
}
