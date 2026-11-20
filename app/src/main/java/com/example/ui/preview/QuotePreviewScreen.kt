package com.example.ui.preview

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.Business
import com.example.domain.model.Quote
import com.example.domain.model.QuoteStatus
import com.example.domain.model.SupportedLanguage
import com.example.domain.pricing.CurrencyFormatter
import com.example.pdf.QuoteShareHelper
import com.example.ui.components.GlovePrimaryButton
import com.example.ui.components.GloveSecondaryButton
import com.example.ui.components.LanguageToggleBar
import com.example.ui.components.MoneyText
import com.example.ui.components.QuoteStatusBadge
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.PaidEmeraldContainer
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SlateNavy
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotePreviewScreen(
    business: Business,
    quote: Quote,
    onBack: () -> Unit,
    onSwitchLanguage: (SupportedLanguage) -> Unit,
    onExportPdf: ((File) -> Unit) -> Unit,
    onMarkSent: () -> Unit,
    onSimulateESign: (signerName: String, payDeposit: Boolean) -> Unit,
    onConvertToInvoice: () -> Unit
) {
    val context = LocalContext.current
    val isEs = quote.outputLanguage == SupportedLanguage.ES
    var showESignDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.preview_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        QuoteStatusBadge(status = quote.status)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            // Independent Customer Output Language Selector (EN / ES regardless of spoken input!)
            Text(
                text = stringResource(R.string.preview_output_language),
                style = MaterialTheme.typography.titleMedium
            )
            LanguageToggleBar(
                selected = quote.outputLanguage,
                onSelect = onSwitchLanguage,
                testTagPrefix = "preview_lang"
            )

            // Branded Document Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("branded_quote_preview_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Branded Dark Slate Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateNavy)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                business.logoLocalUri?.let { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Company Logo",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column {
                                    Text(
                                        text = business.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    if (business.licenseNumber.isNotBlank()) {
                                        Text(
                                            text = (if (isEs) "Licencia #: " else "Lic #: ") + business.licenseNumber,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }
                                    if (business.phone.isNotBlank()) {
                                        Text(
                                            text = business.phone,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SafetyAmber
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = if (isEs) "COTIZACIÓN" else "QUOTE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SlateInk
                                    )
                                    Text(
                                        text = quote.quoteNumber,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SlateInk
                                    )
                                }
                            }
                        }
                    }

                    // Safety Amber Accent Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(SafetyAmber)
                    )

                    // Document Body
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isEs) "PREPARADO PARA" else "PREPARED FOR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = quote.customerNameSnapshot.ifBlank {
                                if (isEs) "Cliente" else "Valued Customer"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = SlateInk
                        )
                        if (quote.customerAddressSnapshot.isNotBlank()) {
                            Text(
                                text = quote.customerAddressSnapshot,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (quote.title.isNotBlank()) {
                            Text(
                                text = "${if (isEs) "Proyecto" else "Job"}: ${quote.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateInk
                            )
                        }

                        HorizontalDivider()

                        // Line Items Table
                        quote.lineItems.forEach { item ->
                            val unitLbl = if (isEs) item.unit.labelEs else item.unit.labelEn
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SlateInk
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatQuantity(item.quantity)} $unitLbl × ${CurrencyFormatter.formatCents(item.unitPriceCents)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                MoneyText(cents = item.lineTotalCents, color = SlateInk, fontSizeSp = 16)
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isEs) "Subtotal" else "Subtotal", color = SlateInk)
                            MoneyText(cents = quote.subtotalCents, color = SlateInk, fontSizeSp = 15, bold = false)
                        }
                        if (quote.taxRateBps > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${if (isEs) "Impuesto" else "Tax"} (${quote.taxRateBps / 100.0}%)",
                                    color = SlateInk
                                )
                                MoneyText(cents = quote.taxCents, color = SlateInk, fontSizeSp = 15, bold = false)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.titleLarge,
                                color = SlateInk
                            )
                            MoneyText(cents = quote.totalCents, color = SlateInk, fontSizeSp = 22)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEs) "Depósito al aprobar (${quote.depositPercent}%)" else "Deposit due on approval (${quote.depositPercent}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SafetyAmber
                            )
                            MoneyText(cents = quote.depositCents, color = SafetyAmber, fontSizeSp = 16)
                        }

                        if (quote.notes.isNotBlank()) {
                            HorizontalDivider()
                            Text(
                                text = "${if (isEs) "Notas" else "Notes"}: ${quote.notes}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (quote.termsSnapshot.isNotBlank()) {
                            Text(
                                text = quote.termsSnapshot,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!quote.signerName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PaidEmeraldContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = PaidEmerald
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isEs) "Firmado por ${quote.signerName}" else "E-Signed by ${quote.signerName}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = PaidEmerald
                                        )
                                        if (quote.depositPaidCents > 0L) {
                                            Text(
                                                text = (if (isEs) "Depósito recibido: " else "Deposit received: ") +
                                                    CurrencyFormatter.formatCents(quote.depositPaidCents),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = PaidEmerald
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Send & Export Actions (Glove-friendly 56dp+)
            GlovePrimaryButton(
                text = stringResource(R.string.preview_generate_pdf),
                icon = Icons.Filled.PictureAsPdf,
                onClick = {
                    onExportPdf { pdfFile ->
                        val summary = QuoteShareHelper.buildShareSummary(business, quote)
                        QuoteShareHelper.launchPdfShare(context, pdfFile, summary)
                    }
                },
                modifier = Modifier.testTag("export_pdf_button")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GloveSecondaryButton(
                    text = stringResource(R.string.preview_send_sms),
                    icon = Icons.Filled.Sms,
                    onClick = {
                        val summary = QuoteShareHelper.buildShareSummary(business, quote)
                        onMarkSent()
                        QuoteShareHelper.launchSmsShare(context, quote.customerPhoneSnapshot, summary)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("send_sms_button")
                )
                GloveSecondaryButton(
                    text = stringResource(R.string.preview_send_email),
                    icon = Icons.Filled.Email,
                    onClick = {
                        val summary = QuoteShareHelper.buildShareSummary(business, quote)
                        val subject = "${quote.quoteNumber} — ${business.name}"
                        onMarkSent()
                        QuoteShareHelper.launchEmailShare(
                            context,
                            quote.customerEmailSnapshot,
                            subject,
                            summary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("send_email_button")
                )
            }

            GloveSecondaryButton(
                text = "${stringResource(R.string.preview_share_link)} (${quote.shareUrl.orEmpty().removePrefix("https://")})",
                icon = Icons.Filled.Link,
                onClick = {
                    val summary = QuoteShareHelper.buildShareSummary(business, quote)
                    onMarkSent()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, summary)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(Intent.createChooser(shareIntent, "Share Quote Link")) }
                },
                modifier = Modifier.testTag("share_link_button")
            )

            if (quote.status != QuoteStatus.ACCEPTED && quote.status != QuoteStatus.INVOICED) {
                GloveSecondaryButton(
                    text = stringResource(R.string.preview_mark_accepted),
                    icon = Icons.Filled.Draw,
                    onClick = { showESignDialog = true },
                    modifier = Modifier.testTag("simulate_esign_button")
                )
            }

            GlovePrimaryButton(
                text = stringResource(R.string.preview_convert_invoice),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                containerColor = PaidEmerald,
                contentColor = Color.White,
                onClick = onConvertToInvoice,
                modifier = Modifier.testTag("convert_to_invoice_button")
            )
            Spacer(Modifier.height(72.dp))
        }
    }

    if (showESignDialog) {
        var signerName by remember { mutableStateOf(quote.customerNameSnapshot.ifBlank { "Customer" }) }
        var collectDeposit by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showESignDialog = false },
            title = { Text("Customer E-Sign & Deposit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Preview the customer approval step locally (hosted web page & Stripe Connect activate when Firebase Functions are deployed)."
                    )
                    OutlinedTextField(
                        value = signerName,
                        onValueChange = { signerName = it },
                        label = { Text("Customer Signature Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = collectDeposit,
                            onCheckedChange = { collectDeposit = it }
                        )
                        Text(
                            text = "Mark ${CurrencyFormatter.formatCents(quote.depositCents)} deposit paid"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showESignDialog = false
                        onSimulateESign(signerName, collectDeposit)
                    },
                    modifier = Modifier.testTag("confirm_esign_button")
                ) {
                    Text("Approve & Sign")
                }
            },
            dismissButton = {
                TextButton(onClick = { showESignDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
