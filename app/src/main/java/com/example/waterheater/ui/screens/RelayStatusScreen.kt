package com.example.waterheater.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterheater.ui.theme.*

@Composable
fun RelayStatusDialog(
    currentMode: String,
    onSelectMode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚡ Power-Restore Behavior",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Controls what happens to the water heater relay after a mains power outage:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                RelayModeOptionCard(
                    title = "Power OFF (Recommended)",
                    subtitle = "Relay stays OFF after blackout. Prevents unattended high-power heating.",
                    icon = Icons.Default.Shield,
                    iconTint = EmeraldSuccess,
                    isSelected = currentMode == "power_off",
                    onClick = { onSelectMode("power_off") }
                )

                RelayModeOptionCard(
                    title = "Power ON",
                    subtitle = "Relay automatically turns ON when power is restored.",
                    icon = Icons.Default.FlashOn,
                    iconTint = AmberFirePrimary,
                    isSelected = currentMode == "power_on",
                    onClick = { onSelectMode("power_on") }
                )

                RelayModeOptionCard(
                    title = "Remember Last State",
                    subtitle = "Restores whatever state (ON or OFF) was active before power failed.",
                    icon = Icons.Default.History,
                    iconTint = CyanWaterPrimary,
                    isSelected = currentMode == "last",
                    onClick = { onSelectMode("last") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun RelayModeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AmberFirePrimary else CardBorder
    val bgColor = if (isSelected) SurfaceVariantCard else SurfaceCard

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AmberFirePrimary,
                unselectedColor = TextMuted
            )
        )
    }
}
