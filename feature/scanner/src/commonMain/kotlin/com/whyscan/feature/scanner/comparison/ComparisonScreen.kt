package com.whyscan.feature.scanner.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whyscan.core.designsystem.Spacing
import com.whyscan.core.domain.scan.EngineMetrics
import com.whyscan.feature.scanner.resources.Res
import com.whyscan.feature.scanner.resources.action_stop
import com.whyscan.feature.scanner.resources.comparison_counts
import com.whyscan.feature.scanner.resources.comparison_detections
import com.whyscan.feature.scanner.resources.comparison_failures
import com.whyscan.feature.scanner.resources.comparison_frames
import com.whyscan.feature.scanner.resources.comparison_frames_per_detection
import com.whyscan.feature.scanner.resources.comparison_hint
import com.whyscan.feature.scanner.resources.comparison_latencies
import com.whyscan.feature.scanner.resources.comparison_leader
import com.whyscan.feature.scanner.resources.comparison_millis
import com.whyscan.feature.scanner.resources.comparison_needs_two_engines
import com.whyscan.feature.scanner.resources.comparison_no_data
import com.whyscan.feature.scanner.resources.comparison_participants
import com.whyscan.feature.scanner.resources.comparison_reset
import com.whyscan.feature.scanner.resources.comparison_start
import com.whyscan.feature.scanner.resources.comparison_title
import com.whyscan.feature.scanner.resources.comparison_unique_values
import com.whyscan.feature.scanner.resources.session_error
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComparisonScreen(viewModel: ComparisonViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ComparisonContent(state = state, onAction = viewModel::onAction)
}

@Composable
fun ComparisonContent(
    state: ComparisonState,
    onAction: (ComparisonAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { Header(state, onAction) }

        if (state.hasResults) {
            items(state.entries, key = { it.engineId.id }) { metrics ->
                MetricsCard(metrics, isLeader = metrics.engineId == state.leader?.engineId)
            }
        }
    }
}

@Composable
private fun Header(state: ComparisonState, onAction: (ComparisonAction) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.comparison_title),
                style = MaterialTheme.typography.titleMedium,
            )

            if (state.notEnoughEngines) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.comparison_needs_two_engines,
                        state.participants.size,
                        state.participants.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(
                        Res.string.comparison_participants,
                        state.participants.joinToString { it.id },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.error?.let {
                Text(
                    text = stringResource(Res.string.session_error, it.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Los tres controles son iconos y no palabras. Son los mandos de siempre —arrancar,
            // parar, volver a empezar— y con la palabra ocupaban la fila entera: en un teléfono
            // estrecho o con el cuerpo de letra subido, "Reiniciar" se salía por la derecha. Lo que
            // dicen sigue estando: es su descripción hablada, que es la misma cadena de antes.
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = { onAction(ComparisonAction.Start) },
                    enabled = !state.isRunning && !state.notEnoughEngines,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(Res.string.comparison_start),
                    )
                }
                OutlinedIconButton(
                    onClick = { onAction(ComparisonAction.Stop) },
                    enabled = state.isRunning,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = stringResource(Res.string.action_stop),
                    )
                }
                OutlinedIconButton(onClick = { onAction(ComparisonAction.Reset) }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(Res.string.comparison_reset),
                    )
                }
            }

            if (!state.hasResults && !state.notEnoughEngines) {
                Text(
                    text = stringResource(Res.string.comparison_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun MetricsCard(metrics: EngineMetrics, isLeader: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = if (isLeader) {
                    stringResource(Res.string.comparison_leader, metrics.engineId.id)
                } else {
                    metrics.engineId.id
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (isLeader) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                // Dos contadores no caben en un solo plural: `pluralStringResource` elige la
                // forma con **una** cantidad, así que "1 códigos distintos · 3 lecturas" no tenía
                // arreglo mientras fuera una sola cadena. Se pluralizan por separado y
                // `comparison_counts` pasa a ser la plantilla que los une — el separador sigue
                // siendo traducible, que es lo que se perdería juntándolos en Kotlin.
                text = stringResource(
                    Res.string.comparison_counts,
                    pluralStringResource(
                        Res.plurals.comparison_unique_values,
                        metrics.uniqueValues,
                        metrics.uniqueValues,
                    ),
                    pluralStringResource(
                        Res.plurals.comparison_detections,
                        metrics.detections,
                        metrics.detections,
                    ),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    Res.string.comparison_latencies,
                    metrics.firstDetectionLatencyMillis.asMillis(),
                    metrics.averageLatencyMillis.asMillis(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = buildString {
                    append(
                        pluralStringResource(
                            Res.plurals.comparison_frames,
                            metrics.framesAnalyzed,
                            metrics.framesAnalyzed,
                        ),
                    )
                    metrics.framesPerDetection?.let {
                        append(stringResource(Res.string.comparison_frames_per_detection, it))
                    }
                    if (metrics.transientFailures > 0) {
                        append(
                            pluralStringResource(
                                Res.plurals.comparison_failures,
                                metrics.transientFailures,
                                metrics.transientFailures,
                            ),
                        )
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Una latencia ausente se muestra como raya, no como cero: no medida no es lo mismo que rápida. */
@Composable
private fun Long?.asMillis(): String = this
    ?.let { stringResource(Res.string.comparison_millis, it) }
    ?: stringResource(Res.string.comparison_no_data)
