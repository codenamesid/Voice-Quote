package com.example.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.Business
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TradeType
import com.example.domain.pricing.CurrencyFormatter
import com.example.domain.pricing.TradePriceBookSeeder
import com.example.ui.components.GlovePrimaryButton
import com.example.ui.components.GloveSecondaryButton
import com.example.ui.components.LanguageToggleBar
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateInk
import com.example.ui.theme.SlateNavy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    existingBusiness: Business?,
    onComplete: (
        businessName: String,
        trade: TradeType,
        phone: String,
        licenseNumber: String,
        taxRatePercent: Double,
        depositPercent: Int,
        defaultLanguage: SupportedLanguage,
        logoLocalUri: String?
    ) -> Unit
) {
    var businessName by rememberSaveable { mutableStateOf(existingBusiness?.name.orEmpty()) }
    var selectedTrade by rememberSaveable { mutableStateOf(existingBusiness?.trade ?: TradeType.PLUMBING) }
    var phone by rememberSaveable { mutableStateOf(existingBusiness?.phone.orEmpty()) }
    var licenseNumber by rememberSaveable { mutableStateOf(existingBusiness?.licenseNumber.orEmpty()) }
    var taxRateText by rememberSaveable {
        mutableStateOf(
            if (existingBusiness != null && existingBusiness.defaultTaxRateBps > 0) {
                (existingBusiness.defaultTaxRateBps / 100.0).toString()
            } else {
                "8.25"
            }
        )
    }
    var depositText by rememberSaveable {
        mutableStateOf((existingBusiness?.defaultDepositPercent ?: 30).toString())
    }
    var defaultLanguage by rememberSaveable {
        mutableStateOf(existingBusiness?.defaultLanguage ?: SupportedLanguage.EN)
    }
    var logoUri by rememberSaveable { mutableStateOf(existingBusiness?.logoLocalUri) }

    // Zero-permission Android Photo Picker for logo upload (Google Play compliant)
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            logoUri = uri.toString()
        }
    }

    val previewSeededItems = remember(selectedTrade) {
        TradePriceBookSeeder.seedForTrade("preview", selectedTrade)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // High-contrast industrial header banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(SlateInk, SlateNavy))
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    text = "QUOTEVOICE",
                    style = MaterialTheme.typography.labelMedium,
                    color = SafetyAmber
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD1D5DB)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Default Language Toggle
            Text(
                text = "Default Language / Idioma",
                style = MaterialTheme.typography.titleMedium
            )
            LanguageToggleBar(
                selected = defaultLanguage,
                onSelect = { defaultLanguage = it },
                testTagPrefix = "onboarding_lang"
            )

            // Business Name + Logo
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (logoUri != null) {
                            AsyncImage(
                                model = logoUri,
                                contentDescription = "Business Logo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        GloveSecondaryButton(
                            text = stringResource(
                                if (logoUri == null) R.string.onboarding_pick_logo else R.string.onboarding_change_logo
                            ),
                            icon = Icons.Filled.PhotoLibrary,
                            onClick = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_pick_logo_button")
                        )
                    }

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text(stringResource(R.string.onboarding_business_name)) },
                        placeholder = { Text("e.g. Rivera Plumbing & Drain") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_business_name_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.onboarding_phone)) },
                        placeholder = { Text("512-555-0190") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_phone_input")
                    )

                    OutlinedTextField(
                        value = licenseNumber,
                        onValueChange = { licenseNumber = it },
                        label = { Text(stringResource(R.string.onboarding_license_number)) },
                        placeholder = { Text("TX-MPL-44821") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_license_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text(stringResource(R.string.onboarding_tax_rate)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = depositText,
                            onValueChange = { depositText = it },
                            label = { Text(stringResource(R.string.onboarding_deposit_percent)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Trade Picker (Seeds Starter Price Book)
            Text(
                text = stringResource(R.string.onboarding_pick_trade),
                style = MaterialTheme.typography.titleLarge
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TradeType.entries.forEach { trade ->
                    val isSelected = trade == selectedTrade
                    val tradeIcon = when (trade) {
                        TradeType.PLUMBING -> Icons.Filled.Plumbing
                        TradeType.ELECTRICAL -> Icons.Filled.ElectricBolt
                        TradeType.HANDYMAN -> Icons.Filled.Handyman
                        TradeType.PAINTING -> Icons.Filled.FormatPaint
                        TradeType.LANDSCAPING -> Icons.Filled.Grass
                        TradeType.GENERAL -> Icons.Filled.Build
                    }
                    Surface(
                        onClick = { selectedTrade = trade },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("trade_chip_${trade.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) SafetyAmber else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tradeIcon,
                                contentDescription = null,
                                tint = if (isSelected) SafetyAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (defaultLanguage == SupportedLanguage.ES) trade.displayNameEs else trade.displayNameEn,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Preview of Seeded Price Book
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.onboarding_seeded_items_note,
                                previewSeededItems.size,
                                if (defaultLanguage == SupportedLanguage.ES) selectedTrade.displayNameEs else selectedTrade.displayNameEn
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    previewSeededItems.take(4).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.localizedName(defaultLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${CurrencyFormatter.formatCents(item.unitPriceCents)}/${item.unit.labelEn}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            GlovePrimaryButton(
                text = stringResource(R.string.onboarding_finish_cta),
                onClick = {
                    val safeName = businessName.trim().ifBlank { "My Trade Shop" }
                    val tax = taxRateText.toDoubleOrNull() ?: 8.25
                    val dep = depositText.toIntOrNull() ?: 30
                    onComplete(
                        safeName,
                        selectedTrade,
                        phone,
                        licenseNumber,
                        tax,
                        dep,
                        defaultLanguage,
                        logoUri
                    )
                },
                modifier = Modifier.testTag("onboarding_finish_button")
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
