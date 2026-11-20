package com.example.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.LineItem
import com.example.domain.model.LineItemCategory
import com.example.domain.model.PriceBookItem
import com.example.domain.model.PriceSource
import com.example.domain.model.PricingUnit
import com.example.domain.model.Quote
import com.example.domain.pricing.CurrencyFormatter
import com.example.ui.components.AiPlaceholderNoticeBanner
import com.example.ui.components.GlovePrimaryButton
import com.example.ui.components.GloveSecondaryButton
import com.example.ui.components.MoneyText
import com.example.ui.components.PriceSourceBadge
import com.example.ui.theme.OnSafetyAmberContainer
import com.example.ui.theme.SafetyAmberContainer
import java.util.UUID
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewEditQuoteScreen(
    quote: Quote,
    priceBook: List<PriceBookItem>,
    isAiPlaceholderMode: Boolean,
    onBack: () -> Unit,
    onUpdateQuote: (Quote) -> Unit,
    onDeleteLineItem: (String) -> Unit,
    onSaveLineItem: (LineItem) -> Unit,
    onAddFromPriceBook: (PriceBookItem) -> Unit,
    onOpenPreview: () -> Unit
) {
    var editingItem by remember { mutableStateOf<LineItem?>(null) }
    var showPriceBookPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.review_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = quote.quoteNumber,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.total),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            MoneyText(cents = quote.totalCents, fontSizeSp = 24)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${stringResource(R.string.deposit_due)} (${quote.depositPercent}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            MoneyText(
                                cents = quote.depositCents,
                                fontSizeSp = 17,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    GlovePrimaryButton(
                        text = stringResource(R.string.review_preview_cta),
                        icon = Icons.Filled.Send,
                        onClick = onOpenPreview,
                        modifier = Modifier.testTag("preview_quote_button")
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isAiPlaceholderMode) {
                item(key = "placeholder_notice") {
                    AiPlaceholderNoticeBanner()
                }
            }

            if (quote.estimatedItemCount > 0) {
                item(key = "estimate_warning") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SafetyAmberContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = OnSafetyAmberContainer
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.review_estimate_warning, quote.estimatedItemCount),
                                style = MaterialTheme.typography.labelLarge,
                                color = OnSafetyAmberContainer
                            )
                        }
                    }
                }
            }

            // Customer & Job Site Card
            item(key = "customer_card") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.review_customer_section),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = quote.customerNameSnapshot,
                            onValueChange = { onUpdateQuote(quote.copy(customerNameSnapshot = it)) },
                            label = { Text(stringResource(R.string.review_customer_name)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("review_customer_name_input")
                        )
                        OutlinedTextField(
                            value = quote.customerAddressSnapshot,
                            onValueChange = { onUpdateQuote(quote.copy(customerAddressSnapshot = it)) },
                            label = { Text(stringResource(R.string.review_customer_address)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = quote.customerPhoneSnapshot,
                            onValueChange = { onUpdateQuote(quote.copy(customerPhoneSnapshot = it)) },
                            label = { Text(stringResource(R.string.review_customer_phone)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Line Items Header
            item(key = "line_items_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${stringResource(R.string.review_line_items)} (${quote.lineItems.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.review_swipe_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Swipe-to-delete + Tap-to-edit Line Items
            items(quote.lineItems, key = { it.id }) { lineItem ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDeleteLineItem(lineItem.id)
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.review_delete_action),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.review_delete_action),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("line_item_card_${lineItem.id}")
                            .clickable { editingItem = lineItem },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            width = if (lineItem.requiresPriceReview()) 2.dp else 1.dp,
                            color = if (lineItem.requiresPriceReview()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PriceSourceBadge(source = lineItem.priceSource)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit line item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onDeleteLineItem(lineItem.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("delete_line_item_${lineItem.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete line item",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Text(
                                text = lineItem.name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (lineItem.description.isNotBlank()) {
                                Text(
                                    text = lineItem.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${CurrencyFormatter.formatQuantity(lineItem.quantity)} ${lineItem.unit.labelEn} × ${CurrencyFormatter.formatCents(lineItem.unitPriceCents)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(cents = lineItem.lineTotalCents, fontSizeSp = 18)
                            }
                        }
                    }
                }
            }

            // Add Item Actions
            item(key = "add_item_buttons") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GloveSecondaryButton(
                        text = "Price Book",
                        icon = Icons.Filled.LibraryBooks,
                        onClick = { showPriceBookPicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_from_price_book_button")
                    )
                    GloveSecondaryButton(
                        text = stringResource(R.string.review_add_item),
                        icon = Icons.Filled.Add,
                        onClick = {
                            editingItem = LineItem(
                                id = UUID.randomUUID().toString(),
                                quoteId = quote.id,
                                sortOrder = quote.lineItems.size,
                                name = "",
                                category = LineItemCategory.LABOR,
                                quantity = 1.0,
                                unit = PricingUnit.EACH,
                                unitPriceCents = 0L,
                                priceSource = PriceSource.MANUAL
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_custom_item_button")
                    )
                }
            }

            // Job Notes
            item(key = "notes_field") {
                OutlinedTextField(
                    value = quote.notes,
                    onValueChange = { onUpdateQuote(quote.copy(notes = it)) },
                    label = { Text(stringResource(R.string.review_notes_label)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Subtotal / Tax Breakdown
            item(key = "totals_summary") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.subtotal))
                            MoneyText(cents = quote.subtotalCents, fontSizeSp = 16, bold = false)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${stringResource(R.string.tax)} (${quote.taxRateBps / 100.0}%)")
                            MoneyText(cents = quote.taxCents, fontSizeSp = 16, bold = false)
                        }
                    }
                }
            }
        }
    }

    // Tap-to-edit LineItem Dialog
    editingItem?.let { item ->
        EditLineItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                onSaveLineItem(updated)
                editingItem = null
            }
        )
    }

    // Price Book Picker Dialog
    if (showPriceBookPicker) {
        AlertDialog(
            onDismissRequest = { showPriceBookPicker = false },
            title = { Text("Add from Price Book") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(priceBook, key = { it.id }) { pbItem ->
                        Surface(
                            onClick = {
                                onAddFromPriceBook(pbItem)
                                showPriceBookPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pbItem.localizedName(quote.outputLanguage),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                MoneyText(cents = pbItem.unitPriceCents, fontSizeSp = 15)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPriceBookPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun EditLineItemDialog(
    item: LineItem,
    onDismiss: () -> Unit,
    onSave: (LineItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var description by remember { mutableStateOf(item.description) }
    var qtyText by remember { mutableStateOf(CurrencyFormatter.formatQuantity(item.quantity)) }
    var priceDollarsText by remember {
        mutableStateOf(String.format("%.2f", item.unitPriceCents / 100.0))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Line Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Qty (${item.unit.labelEn})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_item_qty_input")
                    )
                    OutlinedTextField(
                        value = priceDollarsText,
                        onValueChange = { priceDollarsText = it },
                        label = { Text("Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_item_price_input")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = qtyText.toDoubleOrNull()?.coerceAtLeast(0.01) ?: item.quantity
                    val priceCents = ((priceDollarsText.toDoubleOrNull() ?: 0.0) * 100.0)
                        .roundToLong()
                        .coerceAtLeast(0L)
                    onSave(
                        item.copy(
                            name = name.trim().ifBlank { "Custom Item" },
                            description = description.trim(),
                            quantity = qty,
                            unitPriceCents = priceCents,
                            priceSource = PriceSource.MANUAL
                        )
                    )
                },
                modifier = Modifier.testTag("save_line_item_button")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
