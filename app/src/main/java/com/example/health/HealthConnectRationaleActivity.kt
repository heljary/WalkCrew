package com.example.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CardShape
import com.example.ui.theme.Spacing
import com.example.ui.theme.WalkCrewTheme

class HealthConnectRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WalkCrewTheme {
                HealthConnectRationaleScreen(
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun HealthConnectRationaleScreen(
    onDismiss: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenHorizontal)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Icon Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Health Connect & WalkCrew",
                style = MaterialTheme.typography.headlineMedium,
                color = WalkCrewTheme.extendedColors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "WalkCrew uses your step data via Android Health Connect to empower your fitness journey with your crew.",
                style = MaterialTheme.typography.bodyMedium,
                color = WalkCrewTheme.extendedColors.textSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Explanation cards
            RationalePointCard(
                icon = Icons.Rounded.DirectionsWalk,
                title = "Daily Goal Tracking",
                description = "We read today's step count to update your 240dp StepRing and motivate you toward your 8,000 steps goal."
            )

            Spacer(modifier = Modifier.height(16.dp))

            RationalePointCard(
                icon = Icons.Rounded.Shield,
                title = "Weekly Trends & Analysis",
                description = "We access the past 7 days of step totals to compute your distance, active minutes, and weekly bar chart."
            )

            Spacer(modifier = Modifier.height(16.dp))

            RationalePointCard(
                icon = Icons.Rounded.Lock,
                title = "Privacy First",
                description = "Your health data is never sold, shared with advertisers, or used for third-party tracking. You can revoke access anytime in Health Connect settings."
            )

            Spacer(modifier = Modifier.height(36.dp))

            PrimaryButton(
                text = "Got It, Continue",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RationalePointCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WalkCrewTheme.extendedColors.divider, CardShape),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkCrewTheme.extendedColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = WalkCrewTheme.extendedColors.textSecondary
                )
            }
        }
    }
}
