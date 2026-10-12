package com.example.ui.paywall

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.example.ui.util.DevicePreviews
import com.example.ui.util.FontScalePreviews
import com.example.ui.util.ThemePreviews
import com.example.ui.theme.WalkCrewTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.RevenueCatManager
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.CardShape
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PlusJakartaSansFamily
import com.revenuecat.purchases.Package

enum class SubscriptionPlan {
    MONTHLY,
    YEARLY
}

private data class PremiumBenefit(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val Benefits = listOf(
    PremiumBenefit(
        title = "Unlimited crews",
        subtitle = "Create and join as many walking crews as you want (free limit: 2)",
        icon = Icons.Rounded.Groups
    ),
    PremiumBenefit(
        title = "Custom themes",
        subtitle = "Exclusive app color themes and premium avatar rings",
        icon = Icons.Rounded.Palette
    ),
    PremiumBenefit(
        title = "Detailed history",
        subtitle = "Past weekly leaderboards, hall of fame, and lifetime deep-dive trends",
        icon = Icons.Rounded.Timeline
    ),
    PremiumBenefit(
        title = "Ad-free",
        subtitle = "Zero sponsor banners, distraction-free walking camaraderie",
        icon = Icons.Rounded.Block
    )
)

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    triggerReason: String? = null, // e.g. "You've reached the free limit of 2 crews."
    viewModel: PaywallViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isPremium by viewModel.isPremium.collectAsState()
    val isOfferingsLoading by viewModel.isOfferingsLoading.collectAsState()
    val monthlyPkg by viewModel.monthlyPackage.collectAsState()
    val yearlyPkg by viewModel.yearlyPackage.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val isPurchasing by viewModel.isPurchasing.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    val monthlyPrice = monthlyPkg?.product?.price?.formatted ?: "$3.99"
    val yearlyPrice = yearlyPkg?.product?.price?.formatted ?: "$29.99"

    val yearlySubtext = yearlyPkg?.product?.price?.let { priceObj ->
        val micros = priceObj.amountMicros
        if (micros > 0) {
            val monthlyAmount = (micros / 1_000_000.0) / 12.0
            val currencySymbol = priceObj.formatted.replace(Regex("[0-9.,\\s]"), "")
            if (currencySymbol.isNotEmpty()) {
                String.format(java.util.Locale.US, "%s%.2f / month", currencySymbol, monthlyAmount)
            } else {
                String.format(java.util.Locale.US, "$%.2f / month", monthlyAmount)
            }
        } else {
            "$2.50 / month"
        }
    } ?: "$2.50 / month"

    // If purchase completed or restored while on paywall, close after entitlement unlocked
    if (isPremium) {
        onDismiss()
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepForest) // full-screen forest green background
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("paywall_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close / Dismiss button at top right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtle badge
                Box(
                    modifier = Modifier
                        .background(LimeAccent.copy(alpha = 0.15f), ButtonShape)
                        .border(1.dp, LimeAccent.copy(alpha = 0.4f), ButtonShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "WALKCREW PLUS",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = LimeAccent,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.10f), CircleShape)
                        .testTag("paywall_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Headline in lime
            Text(
                text = "Go Premium",
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                color = LimeAccent,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("paywall_headline")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (!triggerReason.isNullOrBlank()) {
                    triggerReason
                } else {
                    "Supercharge your motivation and step with all your circles without limits."
                },
                fontFamily = PlusJakartaSansFamily,
                fontSize = 15.sp,
                color = Color(0xFFBAC7BF),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4 Benefit Rows with check icons
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E4D3B), CardShape),
                shape = CardShape,
                color = Color(0xFF13382A).copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Benefits.forEachIndexed { index, benefit ->
                        BenefitRow(benefit = benefit, testTag = "benefit_row_$index")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Plans section: show loading indicator when fetching offerings from Test Store
            if (isOfferingsLoading && monthlyPkg == null && yearlyPkg == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E4D3B), CardShape),
                    shape = CardShape,
                    color = Color(0xFF13382A).copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = LimeAccent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Loading subscription options...",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Two Plan Cards Side by Side: Monthly and Yearly with "Best value" badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Monthly Plan Card
                    PlanCard(
                        title = "Monthly",
                        price = monthlyPrice,
                        subtext = "Billed monthly",
                        isSelected = selectedPlan == SubscriptionPlan.MONTHLY,
                        badgeText = null,
                        onClick = { viewModel.selectPlan(SubscriptionPlan.MONTHLY) },
                        modifier = Modifier.weight(1f),
                        testTag = "plan_monthly_card"
                    )

                    // Yearly Plan Card (with Best Value badge)
                    PlanCard(
                        title = "Yearly",
                        price = yearlyPrice,
                        subtext = yearlySubtext,
                        isSelected = selectedPlan == SubscriptionPlan.YEARLY,
                        badgeText = "Best value",
                        onClick = { viewModel.selectPlan(SubscriptionPlan.YEARLY) },
                        modifier = Modifier.weight(1f),
                        testTag = "plan_yearly_card"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3-day free trial line
            if (com.revenuecat.purchases.Purchases.isConfigured) {
                Text(
                    text = "3-day free trial, cancel anytime in Google Play",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = LimeAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("free_trial_text")
                )
            } else {
                Text(
                    text = "All WalkCrew features are currently free for all members",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = LimeAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("free_trial_text")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Big lime pill button
            Button(
                onClick = {
                    if (!com.revenuecat.purchases.Purchases.isConfigured) {
                        onDismiss()
                    } else if (activity != null) {
                        viewModel.purchase(
                            activity = activity,
                            onSuccess = { onDismiss() }
                        )
                    }
                },
                enabled = !isPurchasing && !isOfferingsLoading,
                shape = ButtonShape,
                elevation = null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeAccent,
                    contentColor = DeepForest,
                    disabledContainerColor = LimeAccent.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_free_trial_button")
            ) {
                if (isPurchasing || (isOfferingsLoading && com.revenuecat.purchases.Purchases.isConfigured)) {
                    CircularProgressIndicator(
                        color = DeepForest,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (!com.revenuecat.purchases.Purchases.isConfigured) {
                    Text(
                        text = "Continue with Free Plan",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = DeepForest
                    )
                } else {
                    Text(
                        text = "Start free trial / Continue",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = DeepForest
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action message banner (if error or info)
            actionMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Restore purchases button
            TextButton(
                onClick = {
                    viewModel.restorePurchases(
                        onSuccess = { active ->
                            if (active) onDismiss()
                        }
                    )
                },
                modifier = Modifier.testTag("restore_purchases_button")
            ) {
                Text(
                    text = "Restore purchases",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFFBAC7BF)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Terms & Privacy links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terms of Service",
                    fontSize = 12.sp,
                    color = Color(0xFF869B8F),
                    modifier = Modifier
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://walkcrew.app/terms"))
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        .padding(4.dp)
                        .testTag("terms_link")
                )

                Text(
                    text = " • ",
                    fontSize = 12.sp,
                    color = Color(0xFF869B8F)
                )

                Text(
                    text = "Privacy Policy",
                    fontSize = 12.sp,
                    color = Color(0xFF869B8F),
                    modifier = Modifier
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://walkcrew.app/privacy"))
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        .padding(4.dp)
                        .testTag("privacy_link")
                )
            }
        }
    }
}

@Composable
private fun BenefitRow(
    benefit: PremiumBenefit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalAlignment = Alignment.Top
    ) {
        // Lime check circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(LimeAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = DeepForest,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = benefit.title,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = benefit.subtitle,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 13.sp,
                color = Color(0xFFBAC7BF),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtext: String,
    isSelected: Boolean,
    badgeText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "plan_card"
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) LimeAccent else Color(0xFF1E4D3B),
                    shape = CardShape
                ),
            shape = CardShape,
            color = if (isSelected) Color(0xFF164433) else Color(0xFF102F23)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Radio indicator
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) LimeAccent else Color(0xFF5B7A6C),
                            shape = CircleShape
                        )
                        .background(if (isSelected) LimeAccent else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DeepForest)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = title,
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = price,
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = if (isSelected) LimeAccent else Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtext,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 11.sp,
                    color = Color(0xFFBAC7BF)
                )
            }
        }

        // "Best value" lime badge on top of yearly
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(LimeAccent, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText.uppercase(),
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    color = DeepForest,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@DevicePreviews
@FontScalePreviews
@ThemePreviews
@Composable
private fun PaywallScreenPreview() {
    WalkCrewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepForest)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(max = 640.dp)
                ) {
                    Text(
                        text = "WalkCrew Pro",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = LimeAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlock unlimited crews, custom themes, and full stats",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
