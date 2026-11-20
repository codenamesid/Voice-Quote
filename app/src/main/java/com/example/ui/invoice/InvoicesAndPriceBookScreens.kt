package com.example.ui.invoice

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Business
import com.example.domain.model.Invoice
import com.example.domain.model.InvoiceStatus
import com.example.domain.model.LineItemCategory
import com.example.domain.model.PriceBookItem
import com.example.domain.model.PricingUnit
import com.example.domain.model.SupportedLanguage
import com.example.domain.pricing.CurrencyFormatter
import com.example.pdf.QuoteShareHelper
import com.example.ui.components.GlovePrimaryButton
import com.example.ui.components.GloveSecondaryButton
import com.example.ui.components.InvoiceStatusBadge
import com.example.ui.components.MoneyText
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateNavy
import java.io.File
import kotlin.math.roundToLong

@Composable
fun InvoicesListScreen(
    invoices: List<Invoice>,
    onOpenInvoice: (String) -> Unit
) {
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
                text = stringResource(R.string.invoices_title),
                style = MaterialTheme.typography.headlineMedium
            )
        }

        if (invoices.isEmpty()) {
            item(key = "empty") {
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
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.invoices_empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.invoices_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(invoices, key = { it.id }) { invoice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invoice_card_${invoice.id}")
                        .clickable { onOpenInvoice(invoice.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
                            Text(
                                text = invoice.invoiceNumber,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            InvoiceStatusBadge(status = invoice.status)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = invoice.customerNameSnapshot.ifBlank { "Customer" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = invoice.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.invoice_balance_due),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    cents = invoice.balanceDueCents,
                                    fontSizeSp = 20,
                                    color = if (invoice.balanceDueCents == 0L) PaidEmerald else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    business: Business,
    invoice: Invoice,
    onBack: () -> Unit,
    onToggleAutoReminders: (Boolean) -> Unit,
    onSendReminder: () -> Unit,
    onMarkPaid: () -> Unit,
    onExportInvoicePdf: ((File) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val isEs = invoice.outputLanguage == SupportedLanguage.ES

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(invoice.invoiceNumber, style = MaterialTheme.typography.titleLarge)
                        InvoiceStatusBadge(status = invoice.status)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance Hero Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SlateNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = invoice.customerNameSnapshot.ifBlank { "Customer" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.invoice_balance_due),
                        style = MaterialTheme.typography.labelMedium,
                        color = SafetyAmber
                    )
                    MoneyText(
                        cents = invoice.balanceDueCents,
                        color = Color.White,
                        fontSizeSp = 32
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Invoice Total", color = Color(0xFFD1D5DB))
                        MoneyText(cents = invoice.totalCents, color = Color.White, fontSizeSp = 15, bold = false)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.invoice_deposit_paid), color = Color(0xFFD1D5DB))
                        MoneyText(cents = invoice.amountPaidCents, color = PaidEmerald, fontSizeSp = 15)
                    }
                }
            }

            // Line Items Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Items", style = MaterialTheme.typography.titleMedium)
                    invoice.lineItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${CurrencyFormatter.formatQuantity(item.quantity)} ${item.unit.labelEn} × ${CurrencyFormatter.formatCents(item.unitPriceCents)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            MoneyText(cents = item.lineTotalCents, fontSizeSp = 16)
                        }
                    }
                }
            }

            // Automatic Payment Reminders Card
            Card(
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.invoice_auto_reminders),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Reminders sent: ${invoice.reminderCount} • Every 3 days until paid",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = invoice.autoRemindersEnabled,
                            onCheckedChange = onToggleAutoReminders,
                            modifier = Modifier.testTag("invoice_auto_reminder_switch")
                        )
                    }

                    if (invoice.status != InvoiceStatus.PAID) {
                        GloveSecondaryButton(
                            text = stringResource(R.string.invoice_send_reminder),
                            icon = Icons.Filled.NotificationsActive,
                            onClick = {
                                onSendReminder()
                                val msg = if (isEs) {
                                    "Recordatorio amable de ${business.name}: tu factura ${invoice.invoiceNumber} tiene un saldo pendiente de ${CurrencyFormatter.formatCents(invoice.balanceDueCents)}. Paga aquí: ${invoice.stripePaymentLinkUrl.orEmpty()}"
                                } else {
                                    "Friendly reminder from ${business.name}: Invoice ${invoice.invoiceNumber} has a balance due of ${CurrencyFormatter.formatCents(invoice.balanceDueCents)}. Pay online here: ${invoice.stripePaymentLinkUrl.orEmpty()}"
                                }
                                QuoteShareHelper.launchSmsShare(context, invoice.customerPhoneSnapshot, msg)
                            },
                            modifier = Modifier.testTag("send_reminder_button")
                        )
                    }
                }
            }

            if (invoice.status != InvoiceStatus.PAID) {
                GlovePrimaryButton(
                    text = stringResource(R.string.invoice_record_payment),
                    icon = Icons.Filled.CheckCircle,
                    containerColor = PaidEmerald,
                    contentColor = Color.White,
                    onClick = onMarkPaid,
                    modifier = Modifier.testTag("mark_invoice_paid_button")
                )
            }

            GloveSecondaryButton(
                text = "Export Invoice PDF",
                icon = Icons.Filled.PictureAsPdf,
                onClick = {
                    onExportInvoicePdf { file ->
                        QuoteShareHelper.launchPdfShare(
                            context,
                            file,
                            "Invoice ${invoice.invoiceNumber} from ${business.name}"
                        )
                    }
                },
                modifier = Modifier.testTag("export_invoice_pdf_button")
            )
        }
    }
}

@Composable
fun PriceBookScreen(
    business: Business?,
    items: List<PriceBookItem>,
    onSaveItem: (
        existingId: String?,
        nameEn: String,
        nameEs: String,
        category: LineItemCategory,
        unit: PricingUnit,
        unitPriceCents: Long
    ) -> Unit,
    onEditShopProfile: () -> Unit
) {
    var editingItem by remember { mutableStateOf<PriceBookItem?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.nav_price_book),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${business?.trade?.displayNameEn ?: "Trade"} • ${items.size} saved rates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onEditShopProfile,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("edit_business_profile_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Edit Business Profile")
                }
            }
        }

        item(key = "add_button") {
            GlovePrimaryButton(
                text = "Add Price Book Item",
                icon = Icons.Filled.Add,
                onClick = { addingNew = true },
                modifier = Modifier.testTag("add_price_book_item_button")
            )
        }

        items(items, key = { it.id }) { pbItem ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editingItem = pbItem },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pbItem.name, style = MaterialTheme.typography.titleMedium)
                        if (pbItem.nameEs.isNotBlank()) {
                            Text(
                                pbItem.nameEs,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${pbItem.category.labelEn} • per ${pbItem.unit.labelEn}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    MoneyText(cents = pbItem.unitPriceCents, fontSizeSp = 18)
                }
            }
        }
    }

    if (addingNew || editingItem != null) {
        val current = editingItem
        var nameEn by remember(current) { mutableStateOf(current?.name.orEmpty()) }
        var nameEs by remember(current) { mutableStateOf(current?.nameEs.orEmpty()) }
        var priceText by remember(current) {
            mutableStateOf(
                if (current != null) String.format("%.2f", current.unitPriceCents / 100.0) else ""
            )
        }

        AlertDialog(
            onDismissRequest = {
                addingNew = false
                editingItem = null
            },
            title = { Text(if (current == null) "New Price Book Item" else "Edit Price Book Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("Name (English)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nameEs,
                        onValueChange = { nameEs = it },
                        label = { Text("Nombre (Español)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Unit Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cents = ((priceText.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong()
                        if (nameEn.isNotBlank()) {
                            onSaveItem(
                                current?.id,
                                nameEn,
                                nameEs,
                                current?.category ?: LineItemCategory.LABOR,
                                current?.unit ?: PricingUnit.EACH,
                                cents
                            )
                        }
                        addingNew = false
                        editingItem = null
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addingNew = false
                        editingItem = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
