package com.example.data.local.entity

import com.example.domain.model.Business
import com.example.domain.model.Customer
import com.example.domain.model.Invoice
import com.example.domain.model.InvoiceStatus
import com.example.domain.model.LineItem
import com.example.domain.model.LineItemCategory
import com.example.domain.model.PriceBookItem
import com.example.domain.model.PriceSource
import com.example.domain.model.PricingUnit
import com.example.domain.model.Quote
import com.example.domain.model.QuoteStatus
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.SyncStatus
import com.example.domain.model.TradeType

fun BusinessEntity.toDomain(): Business = Business(
    id = id,
    ownerUid = ownerUid,
    name = name,
    trade = TradeType.fromNameOrDefault(trade),
    phone = phone,
    email = email,
    address = address,
    licenseNumber = licenseNumber,
    logoLocalUri = logoLocalUri,
    logoRemoteUrl = logoRemoteUrl,
    defaultTaxRateBps = defaultTaxRateBps,
    defaultDepositPercent = defaultDepositPercent,
    defaultTermsEn = defaultTermsEn,
    defaultTermsEs = defaultTermsEs,
    defaultLanguage = SupportedLanguage.fromCodeOrDefault(defaultLanguage),
    quoteNumberPrefix = quoteNumberPrefix,
    nextQuoteNumber = nextQuoteNumber,
    nextInvoiceNumber = nextInvoiceNumber,
    stripeConnectAccountId = stripeConnectAccountId,
    stripeChargesEnabled = stripeChargesEnabled,
    subscriptionTier = SubscriptionTier.fromNameOrDefault(subscriptionTier),
    quotesCreatedThisMonth = quotesCreatedThisMonth,
    quotaPeriodYearMonth = quotaPeriodYearMonth,
    onboardingCompleted = onboardingCompleted,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = SyncStatus.fromNameOrDefault(syncStatus)
)

fun Business.toEntity(): BusinessEntity = BusinessEntity(
    id = id,
    ownerUid = ownerUid,
    name = name,
    trade = trade.name,
    phone = phone,
    email = email,
    address = address,
    licenseNumber = licenseNumber,
    logoLocalUri = logoLocalUri,
    logoRemoteUrl = logoRemoteUrl,
    defaultTaxRateBps = defaultTaxRateBps,
    defaultDepositPercent = defaultDepositPercent,
    defaultTermsEn = defaultTermsEn,
    defaultTermsEs = defaultTermsEs,
    defaultLanguage = defaultLanguage.code,
    quoteNumberPrefix = quoteNumberPrefix,
    nextQuoteNumber = nextQuoteNumber,
    nextInvoiceNumber = nextInvoiceNumber,
    stripeConnectAccountId = stripeConnectAccountId,
    stripeChargesEnabled = stripeChargesEnabled,
    subscriptionTier = subscriptionTier.name,
    quotesCreatedThisMonth = quotesCreatedThisMonth,
    quotaPeriodYearMonth = quotaPeriodYearMonth,
    onboardingCompleted = onboardingCompleted,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = syncStatus.name
)

fun CustomerEntity.toDomain(): Customer = Customer(
    id = id,
    businessId = businessId,
    name = name,
    phone = phone,
    email = email,
    address = address,
    preferredLanguage = SupportedLanguage.fromCodeOrDefault(preferredLanguage),
    notes = notes,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = SyncStatus.fromNameOrDefault(syncStatus)
)

fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    businessId = businessId,
    name = name,
    phone = phone,
    email = email,
    address = address,
    preferredLanguage = preferredLanguage.code,
    notes = notes,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = syncStatus.name
)

fun PriceBookItemEntity.toDomain(): PriceBookItem = PriceBookItem(
    id = id,
    businessId = businessId,
    name = name,
    nameEs = nameEs,
    category = LineItemCategory.fromNameOrDefault(category),
    unit = PricingUnit.fromNameOrDefault(unit),
    unitPriceCents = unitPriceCents,
    defaultQuantity = defaultQuantity,
    keywords = keywordsCsv
        .split("|")
        .map { it.trim() }
        .filter { it.isNotEmpty() },
    taxable = taxable,
    archived = archived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = SyncStatus.fromNameOrDefault(syncStatus)
)

fun PriceBookItem.toEntity(): PriceBookItemEntity = PriceBookItemEntity(
    id = id,
    businessId = businessId,
    name = name,
    nameEs = nameEs,
    category = category.name,
    unit = unit.name,
    unitPriceCents = unitPriceCents,
    defaultQuantity = defaultQuantity,
    keywordsCsv = keywords
        .map { it.replace("|", " ").trim() }
        .filter { it.isNotEmpty() }
        .joinToString("|"),
    taxable = taxable,
    archived = archived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = syncStatus.name
)

fun QuoteLineItemEntity.toDomain(): LineItem = LineItem(
    id = id,
    quoteId = quoteId,
    sortOrder = sortOrder,
    name = name,
    description = description,
    category = LineItemCategory.fromNameOrDefault(category),
    quantity = quantity,
    unit = PricingUnit.fromNameOrDefault(unit),
    unitPriceCents = unitPriceCents,
    taxable = taxable,
    priceSource = PriceSource.fromNameOrDefault(priceSource),
    priceBookItemId = priceBookItemId,
    aiConfidence = aiConfidence
)

fun LineItem.toQuoteLineItemEntity(parentQuoteId: String = quoteId): QuoteLineItemEntity =
    QuoteLineItemEntity(
        id = id,
        quoteId = parentQuoteId,
        sortOrder = sortOrder,
        name = name,
        description = description,
        category = category.name,
        quantity = quantity,
        unit = unit.name,
        unitPriceCents = unitPriceCents,
        taxable = taxable,
        priceSource = priceSource.name,
        priceBookItemId = priceBookItemId,
        aiConfidence = aiConfidence
    )

fun InvoiceLineItemEntity.toDomain(): LineItem = LineItem(
    id = id,
    quoteId = invoiceId,
    sortOrder = sortOrder,
    name = name,
    description = description,
    category = LineItemCategory.fromNameOrDefault(category),
    quantity = quantity,
    unit = PricingUnit.fromNameOrDefault(unit),
    unitPriceCents = unitPriceCents,
    taxable = taxable,
    priceSource = PriceSource.fromNameOrDefault(priceSource),
    priceBookItemId = priceBookItemId,
    aiConfidence = aiConfidence
)

fun LineItem.toInvoiceLineItemEntity(parentInvoiceId: String): InvoiceLineItemEntity =
    InvoiceLineItemEntity(
        id = id,
        invoiceId = parentInvoiceId,
        sortOrder = sortOrder,
        name = name,
        description = description,
        category = category.name,
        quantity = quantity,
        unit = unit.name,
        unitPriceCents = unitPriceCents,
        taxable = taxable,
        priceSource = priceSource.name,
        priceBookItemId = priceBookItemId,
        aiConfidence = aiConfidence
    )

fun QuoteWithLineItems.toDomain(): Quote = Quote(
    id = quote.id,
    businessId = quote.businessId,
    customerId = quote.customerId,
    customerNameSnapshot = quote.customerNameSnapshot,
    customerAddressSnapshot = quote.customerAddressSnapshot,
    customerPhoneSnapshot = quote.customerPhoneSnapshot,
    customerEmailSnapshot = quote.customerEmailSnapshot,
    quoteNumber = quote.quoteNumber,
    title = quote.title,
    rawTranscript = quote.rawTranscript,
    spokenLanguage = SupportedLanguage.fromCodeOrDefault(quote.spokenLanguage),
    outputLanguage = SupportedLanguage.fromCodeOrDefault(quote.outputLanguage),
    notes = quote.notes,
    termsSnapshot = quote.termsSnapshot,
    lineItems = lineItems.sortedBy { it.sortOrder }.map { it.toDomain() },
    taxRateBps = quote.taxRateBps,
    depositPercent = quote.depositPercent,
    status = QuoteStatus.fromNameOrDefault(quote.status),
    extractionPending = quote.extractionPending,
    audioLocalPath = quote.audioLocalPath,
    shareToken = quote.shareToken,
    shareUrl = quote.shareUrl,
    pdfLocalPath = quote.pdfLocalPath,
    validUntilEpochMs = quote.validUntilEpochMs,
    sentAtEpochMs = quote.sentAtEpochMs,
    viewedAtEpochMs = quote.viewedAtEpochMs,
    acceptedAtEpochMs = quote.acceptedAtEpochMs,
    signerName = quote.signerName,
    signatureSvgPath = quote.signatureSvgPath,
    stripeDepositPaymentIntentId = quote.stripeDepositPaymentIntentId,
    depositPaidCents = quote.depositPaidCents,
    createdAtEpochMs = quote.createdAtEpochMs,
    updatedAtEpochMs = quote.updatedAtEpochMs,
    syncStatus = SyncStatus.fromNameOrDefault(quote.syncStatus)
)

fun Quote.toEntity(): QuoteEntity = QuoteEntity(
    id = id,
    businessId = businessId,
    customerId = customerId,
    customerNameSnapshot = customerNameSnapshot,
    customerAddressSnapshot = customerAddressSnapshot,
    customerPhoneSnapshot = customerPhoneSnapshot,
    customerEmailSnapshot = customerEmailSnapshot,
    quoteNumber = quoteNumber,
    title = title,
    rawTranscript = rawTranscript,
    spokenLanguage = spokenLanguage.code,
    outputLanguage = outputLanguage.code,
    notes = notes,
    termsSnapshot = termsSnapshot,
    taxRateBps = taxRateBps,
    depositPercent = depositPercent,
    status = status.name,
    extractionPending = extractionPending,
    audioLocalPath = audioLocalPath,
    shareToken = shareToken,
    shareUrl = shareUrl,
    pdfLocalPath = pdfLocalPath,
    validUntilEpochMs = validUntilEpochMs,
    sentAtEpochMs = sentAtEpochMs,
    viewedAtEpochMs = viewedAtEpochMs,
    acceptedAtEpochMs = acceptedAtEpochMs,
    signerName = signerName,
    signatureSvgPath = signatureSvgPath,
    stripeDepositPaymentIntentId = stripeDepositPaymentIntentId,
    depositPaidCents = depositPaidCents,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = syncStatus.name
)

fun InvoiceWithLineItems.toDomain(): Invoice = Invoice(
    id = invoice.id,
    businessId = invoice.businessId,
    quoteId = invoice.quoteId,
    customerId = invoice.customerId,
    customerNameSnapshot = invoice.customerNameSnapshot,
    customerAddressSnapshot = invoice.customerAddressSnapshot,
    customerPhoneSnapshot = invoice.customerPhoneSnapshot,
    customerEmailSnapshot = invoice.customerEmailSnapshot,
    invoiceNumber = invoice.invoiceNumber,
    title = invoice.title,
    outputLanguage = SupportedLanguage.fromCodeOrDefault(invoice.outputLanguage),
    lineItems = lineItems.sortedBy { it.sortOrder }.map { it.toDomain() },
    taxRateBps = invoice.taxRateBps,
    amountPaidCents = invoice.amountPaidCents,
    status = InvoiceStatus.fromNameOrDefault(invoice.status),
    dueDateEpochMs = invoice.dueDateEpochMs,
    sentAtEpochMs = invoice.sentAtEpochMs,
    paidAtEpochMs = invoice.paidAtEpochMs,
    shareToken = invoice.shareToken,
    shareUrl = invoice.shareUrl,
    pdfLocalPath = invoice.pdfLocalPath,
    stripePaymentLinkUrl = invoice.stripePaymentLinkUrl,
    stripePaymentIntentId = invoice.stripePaymentIntentId,
    autoRemindersEnabled = invoice.autoRemindersEnabled,
    reminderCount = invoice.reminderCount,
    lastReminderSentAtEpochMs = invoice.lastReminderSentAtEpochMs,
    nextReminderAtEpochMs = invoice.nextReminderAtEpochMs,
    notes = invoice.notes,
    createdAtEpochMs = invoice.createdAtEpochMs,
    updatedAtEpochMs = invoice.updatedAtEpochMs,
    syncStatus = SyncStatus.fromNameOrDefault(invoice.syncStatus)
)

fun Invoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id,
    businessId = businessId,
    quoteId = quoteId,
    customerId = customerId,
    customerNameSnapshot = customerNameSnapshot,
    customerAddressSnapshot = customerAddressSnapshot,
    customerPhoneSnapshot = customerPhoneSnapshot,
    customerEmailSnapshot = customerEmailSnapshot,
    invoiceNumber = invoiceNumber,
    title = title,
    outputLanguage = outputLanguage.code,
    taxRateBps = taxRateBps,
    amountPaidCents = amountPaidCents,
    status = status.name,
    dueDateEpochMs = dueDateEpochMs,
    sentAtEpochMs = sentAtEpochMs,
    paidAtEpochMs = paidAtEpochMs,
    shareToken = shareToken,
    shareUrl = shareUrl,
    pdfLocalPath = pdfLocalPath,
    stripePaymentLinkUrl = stripePaymentLinkUrl,
    stripePaymentIntentId = stripePaymentIntentId,
    autoRemindersEnabled = autoRemindersEnabled,
    reminderCount = reminderCount,
    lastReminderSentAtEpochMs = lastReminderSentAtEpochMs,
    nextReminderAtEpochMs = nextReminderAtEpochMs,
    notes = notes,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncStatus = syncStatus.name
)
