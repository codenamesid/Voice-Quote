package com.example.ui.quotes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Business
import com.example.domain.model.Quote
import com.example.domain.model.QuoteStatus
import com.example.domain.model.SubscriptionTier
import com.example.ui.components.GlovePrimaryButton
import com.example.ui.components.GloveSecondaryButton
import com.example.ui.components.MoneyText
import com.example.ui.components.QuoteStatusBadge
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateNavy

@Composable
fun QuotesListScreen(
    business: Business?,
    quotes: List<Quote>,
    onOpenReview: (String) -> Unit,
    onOpenPreview: (String) -> Unit,
    onConvertToInvoice: (String) -> Unit,
    onUpgradePro: (SubscriptionTier) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<QuoteStatus?>(null) }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val filteredQuotes = remember(quotes, searchQuery, selectedFilter) {
        quotes.filter { quote ->
            val matchesFilter = selectedFilter == null || quote.status == selectedFilter
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isBlank() ||
                quote.customerNameSnapshot.lowercase().contains(q) ||
                quote.title.lowercase().contains(q) ||
                quote.quoteNumber.lowercase().contains(q)
            matchesFilter && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Text(
                text = stringResource(R.string.quotes_title),
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Free-tier monthly quota banner (5 quotes/mo)
        val remaining = business?.remainingFreeQuotes()
        if (remaining != null) {
            item(key = "quota_banner") {
                Surface(
                    onClick = { showUpgradeDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = SlateNavy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pro_upgrade_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = SafetyAmber,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.quotes_quota_banner, remaining),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.quotes_upgrade_pro),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SafetyAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search input
        item(key = "search_bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.quotes_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quotes_search_input")
            )
        }

        // Status filter chips
        item(key = "filter_chips") {
            val filters = listOf<QuoteStatus?>(
                null,
                QuoteStatus.DRAFT,
                QuoteStatus.SENT,
                QuoteStatus.ACCEPTED,
                QuoteStatus.INVOICED
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { status ->
                    val isSelected = status == selectedFilter
                    Surface(
                        onClick = { selectedFilter = status },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) SafetyAmber else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = status?.name ?: "ALL",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (filteredQuotes.isEmpty()) {
            item(key = "empty_state") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.quotes_empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.quotes_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredQuotes, key = { it.id }) { quote ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quote_card_${quote.id}")
                        .clickable { onOpenPreview(quote.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${quote.quoteNumber} • ${quote.outputLanguage.code.uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            QuoteStatusBadge(status = quote.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quote.customerNameSnapshot.ifBlank { "Walk-up Customer" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = quote.title.ifBlank { "${quote.lineItems.size} line items" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            MoneyText(cents = quote.totalCents, fontSizeSp = 20)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GloveSecondaryButton(
                                text = "Edit",
                                onClick = { onOpenReview(quote.id) },
                                modifier = Modifier.weight(1f)
                            )
                            if (quote.status != QuoteStatus.INVOICED) {
                                GlovePrimaryButton(
                                    text = "Invoice",
                                    icon = Icons.Filled.ReceiptLong,
                                    containerColor = PaidEmerald,
                                    contentColor = Color.White,
                                    onClick = { onConvertToInvoice(quote.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("AI Voice Quote Pro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Unlimited voice quotes, customer e-signature, Stripe deposit links, automatic payment reminders, and custom branding.")
                    GlovePrimaryButton(
                        text = "Pro Monthly — $24.99 / mo",
                        onClick = {
                            onUpgradePro(SubscriptionTier.PRO_MONTHLY)
                            showUpgradeDialog = false
                        }
                    )
                    GloveSecondaryButton(
                        text = "Pro Yearly — $199 / yr (Save 33%)",
                        onClick = {
                            onUpgradePro(SubscriptionTier.PRO_YEARLY)
                            showUpgradeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
