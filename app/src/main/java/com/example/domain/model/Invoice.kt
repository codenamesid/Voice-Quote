package com.example.domain.model

import kotlin.math.roundToLong

/**
 * Invoice generated with one tap from an accepted Quote (or created directly),
 * tracking deposit credits, remaining balance, Stripe Connect payment links,
 * and automated payment reminders.
 */
data class Invoice(
    val id: String,
    val businessId: String,
    val quoteId: String? = null,
    val customerId: String? = null,
    val customerNameSnapshot: String = "",
    val customerAddressSnapshot: String = "",
    val customerPhoneSnapshot: String = "",
    val customerEmailSnapshot: String = "",
    val invoiceNumber: String,
    val title: String = "",
    val outputLanguage: SupportedLanguage = SupportedLanguage.EN,
    val lineItems: List<LineItem> = emptyList(),
    /** Tax rate in basis points (e.g., 825 = 8.25%). */
    val taxRateBps: Int = 0,
    val amountPaidCents: Long = 0L,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val dueDateEpochMs: Long,
    val sentAtEpochMs: Long? = null,
    val paidAtEpochMs: Long? = null,
    val shareToken: String? = null,
    val shareUrl: String? = null,
    val pdfLocalPath: String? = null,
    val stripePaymentLinkUrl: String? = null,
    val stripePaymentIntentId: String? = null,
    val autoRemindersEnabled: Boolean = true,
    val reminderCount: Int = 0,
    val lastReminderSentAtEpochMs: Long? = null,
    val nextReminderAtEpochMs: Long? = null,
    val notes: String = "",
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

    val balanceDueCents: Long
        get() = (totalCents - amountPaidCents).coerceAtLeast(0L)
}
