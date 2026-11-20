package com.example.domain.model

import kotlin.math.roundToLong

/**
 * Core quote aggregate created from a contractor's voice description.
 * Snapshots customer contact details so sent/signed quotes remain immutable
 * even if the customer record is later edited.
 */
data class Quote(
    val id: String,
    val businessId: String,
    val customerId: String? = null,
    val customerNameSnapshot: String = "",
    val customerAddressSnapshot: String = "",
    val customerPhoneSnapshot: String = "",
    val customerEmailSnapshot: String = "",
    val quoteNumber: String,
    val title: String = "",
    val rawTranscript: String = "",
    val spokenLanguage: SupportedLanguage = SupportedLanguage.EN,
    val outputLanguage: SupportedLanguage = SupportedLanguage.EN,
    val notes: String = "",
    val termsSnapshot: String = "",
    val lineItems: List<LineItem> = emptyList(),
    /** Tax rate in basis points (e.g., 825 = 8.25%). */
    val taxRateBps: Int = 0,
    /** Deposit percentage (0..100). */
    val depositPercent: Int = 30,
    val status: QuoteStatus = QuoteStatus.DRAFT,
    /** True when recorded offline and awaiting cloud LLM extraction once back online. */
    val extractionPending: Boolean = false,
    val audioLocalPath: String? = null,
    val shareToken: String? = null,
    val shareUrl: String? = null,
    val pdfLocalPath: String? = null,
    val validUntilEpochMs: Long? = null,
    val sentAtEpochMs: Long? = null,
    val viewedAtEpochMs: Long? = null,
    val acceptedAtEpochMs: Long? = null,
    val signerName: String? = null,
    val signatureSvgPath: String? = null,
    val stripeDepositPaymentIntentId: String? = null,
    val depositPaidCents: Long = 0L,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPSERT
) {
    val subtotalCents: Long
        get() = lineItems.sumOf { it.lineTotalCents }

    val taxableSubtotalCents: Long
        get() = lineItems.filter { it.taxable }.sumOf { it.lineTotalCents }

    val taxCents: Long
        get() = ((taxableSubtotalCents.toDouble() * taxRateBps.toDouble()) / 10_000.0).roundToLong()

    val totalCents: Long
        get() = subtotalCents + taxCents

    val depositCents: Long
        get() = ((totalCents.toDouble() * depositPercent.coerceIn(0, 100).toDouble()) / 100.0)
            .roundToLong()

    val estimatedItemCount: Int
        get() = lineItems.count { it.priceSource == PriceSource.AI_ESTIMATE }
}
