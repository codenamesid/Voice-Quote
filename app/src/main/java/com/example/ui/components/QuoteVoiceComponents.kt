package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.InvoiceStatus
import com.example.domain.model.PriceSource
import com.example.domain.model.QuoteStatus
import com.example.domain.model.SupportedLanguage
import com.example.domain.pricing.CurrencyFormatter
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSafetyAmberContainer
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.PaidEmeraldContainer
import com.example.ui.theme.SafetyAmberContainer
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.VerifiedBlueContainer

/**
 * Glove-friendly primary action button with guaranteed >= 56.dp touch height.
 */
@Composable
fun GlovePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 16.sp
        )
    }
}

@Composable
fun GloveSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 15.sp
        )
    }
}

@Composable
fun LanguageToggleBar(
    selected: SupportedLanguage,
    onSelect: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "lang_toggle"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SupportedLanguage.entries.forEach { lang ->
            val isSelected = lang == selected
            Surface(
                onClick = { onSelect(lang) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("${testTagPrefix}_${lang.code}"),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else SlateBorder
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (lang == SupportedLanguage.EN) "EN • English" else "ES • Español",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AiPlaceholderNoticeBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_placeholder_notice"),
        shape = RoundedCornerShape(12.dp),
        color = SafetyAmberContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OnSafetyAmberContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.ai_placeholder_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSafetyAmberContainer,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun PriceSourceBadge(source: PriceSource, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (source) {
        PriceSource.PRICE_BOOK -> Triple(
            stringResource(R.string.review_price_book_badge),
            VerifiedBlueContainer,
            VerifiedBlue
        )
        PriceSource.AI_ESTIMATE -> Triple(
            stringResource(R.string.review_estimate_badge),
            SafetyAmberContainer,
            OnSafetyAmberContainer
        )
        PriceSource.MANUAL -> Triple(
            stringResource(R.string.review_manual_badge),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun QuoteStatusBadge(status: QuoteStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        QuoteStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        QuoteStatus.SENT, QuoteStatus.VIEWED -> VerifiedBlueContainer to VerifiedBlue
        QuoteStatus.ACCEPTED, QuoteStatus.INVOICED -> PaidEmeraldContainer to PaidEmerald
        QuoteStatus.DECLINED, QuoteStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun InvoiceStatusBadge(status: InvoiceStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        InvoiceStatus.PAID -> PaidEmeraldContainer to PaidEmerald
        InvoiceStatus.PARTIALLY_PAID -> SafetyAmberContainer to OnSafetyAmberContainer
        InvoiceStatus.SENT, InvoiceStatus.DRAFT -> VerifiedBlueContainer to VerifiedBlue
        InvoiceStatus.OVERDUE, InvoiceStatus.VOID -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = status.name.replace("_", " "),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSizeSp: Int = 18,
    bold: Boolean = true
) {
    Text(
        text = CurrencyFormatter.formatCents(cents),
        modifier = modifier,
        color = color,
        fontFamily = JetBrainsMonoFamily,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = fontSizeSp.sp
    )
}
