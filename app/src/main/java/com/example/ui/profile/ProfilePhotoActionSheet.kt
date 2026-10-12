package com.example.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralHighlight
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.WalkCrewTheme

/**
 * Action modal (ModalBottomSheet) providing options:
 * - "View photo" (enabled only if a profile picture exists)
 * - "Choose from gallery" (launches Android Photo Picker)
 * - "Remove photo" (destructive style, enabled only if a custom photo is set)
 * - "Cancel"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoActionSheet(
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onViewPhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("profile_photo_action_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 16.dp)
        ) {
            // Drag handle pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.width(36.dp),
                    thickness = 4.dp,
                    color = WalkCrewTheme.extendedColors.divider
                )
            }

            Text(
                text = "Profile Photo",
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = WalkCrewTheme.extendedColors.textPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. "View photo"
            ActionSheetItem(
                icon = Icons.Rounded.Visibility,
                title = "View photo",
                enabled = hasPhoto,
                onClick = {
                    onDismiss()
                    onViewPhoto()
                },
                testTag = "action_view_photo"
            )

            // 2. "Choose from gallery"
            ActionSheetItem(
                icon = Icons.Rounded.PhotoLibrary,
                title = "Choose from gallery",
                enabled = true,
                onClick = {
                    onDismiss()
                    onChooseFromGallery()
                },
                testTag = "action_choose_from_gallery"
            )

            // 3. "Remove photo" (Destructive style)
            ActionSheetItem(
                icon = Icons.Rounded.Delete,
                title = "Remove photo",
                enabled = hasPhoto,
                isDestructive = true,
                onClick = {
                    onDismiss()
                    onRemovePhoto()
                },
                testTag = "action_remove_photo"
            )

            HorizontalDivider(
                color = WalkCrewTheme.extendedColors.divider,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 4. "Cancel"
            ActionSheetItem(
                icon = Icons.Rounded.Close,
                title = "Cancel",
                enabled = true,
                onClick = onDismiss,
                testTag = "action_cancel_photo"
            )
        }
    }
}

@Composable
private fun ActionSheetItem(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    isDestructive: Boolean = false
) {
    val contentColor = when {
        !enabled -> WalkCrewTheme.extendedColors.textSecondary.copy(alpha = 0.4f)
        isDestructive -> CoralHighlight
        else -> WalkCrewTheme.extendedColors.textPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontFamily = PlusJakartaSansFamily,
            fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}
