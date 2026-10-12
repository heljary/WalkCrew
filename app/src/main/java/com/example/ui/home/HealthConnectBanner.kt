package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HealthConnectState
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CardShape
import com.example.ui.theme.WalkCrewTheme

@Composable
fun HealthConnectBanner(
    state: HealthConnectState,
    onRequestPermission: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onDismissRationale: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (state) {
        is HealthConnectState.NeedsPermission -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
                    .testTag("health_connect_needs_permission_card"),
                shape = CardShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Connect Health Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WalkCrewTheme.extendedColors.textPrimary
                            )
                            Text(
                                text = "Read today's steps directly from Health Connect",
                                style = MaterialTheme.typography.bodySmall,
                                color = WalkCrewTheme.extendedColors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Allow Step Access",
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "allow_health_connect_button"
                    )
                }
            }
        }

        is HealthConnectState.PermissionDenied -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .border(1.dp, WalkCrewTheme.extendedColors.highlight.copy(alpha = 0.4f), CardShape)
                    .testTag("health_connect_denied_card"),
                shape = CardShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(WalkCrewTheme.extendedColors.highlight.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = WalkCrewTheme.extendedColors.highlight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Step Access Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WalkCrewTheme.extendedColors.textPrimary
                            )
                            Text(
                                text = "WalkCrew needs step permissions to calculate progress and compete with your crew.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WalkCrewTheme.extendedColors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Retry Permission",
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "retry_health_connect_button"
                    )
                }
            }
        }

        is HealthConnectState.NotInstalled -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape)
                    .testTag("health_connect_not_installed_card"),
                shape = CardShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Install Health Connect",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WalkCrewTheme.extendedColors.textPrimary
                            )
                            Text(
                                text = "Health Connect powers real-time step reading from your device sensors and wearables.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WalkCrewTheme.extendedColors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Install from Google Play",
                        onClick = onInstallHealthConnect,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "install_health_connect_button"
                    )
                }
            }
        }

        is HealthConnectState.AvailableAndGranted -> {
            // Subtle, sleek sync indicator badge
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Synced",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Health Connect Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = WalkCrewTheme.extendedColors.textSecondary
                    )
                }
            }
        }

        else -> {
            // Loading or Not supported: graceful no-op
        }
    }
}
