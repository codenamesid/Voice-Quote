package com.example.domain.model

/**
 * Contractor business profile, branding configuration, quoting defaults,
 * Stripe Connect payout linkage, and Free/Pro subscription quota state.
 */
data class Business(
    val id: String,
    val ownerUid: String,
    val name: String,
    val trade: TradeType = TradeType.HANDYMAN,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val licenseNumber: String = "",
    val logoLocalUri: String? = null,
    val logoRemoteUrl: String? = null,
    /** Tax rate in basis points (e.g., 825 = 8.25%). */
    val defaultTaxRateBps: Int = 0,
    /** Default deposit percentage required on quote acceptance (0..100). */
    val defaultDepositPercent: Int = 30,
    val defaultTermsEn: String = DEFAULT_TERMS_EN,
    val defaultTermsEs: String = DEFAULT_TERMS_ES,
    val defaultLanguage: SupportedLanguage = SupportedLanguage.EN,
    val quoteNumberPrefix: String = "QV",
    val nextQuoteNumber: Int = 1001,
    val nextInvoiceNumber: Int = 2001,
    val stripeConnectAccountId: String? = null,
    val stripeChargesEnabled: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val quotesCreatedThisMonth: Int = 0,
    /** Year-month tracker in YYYY-MM format for resetting the Free tier monthly limit. */
    val quotaPeriodYearMonth: String = "",
    val onboardingCompleted: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPSERT
) {
    fun canCreateQuote(): Boolean {
        val limit = subscriptionTier.monthlyQuoteLimit ?: return true
        return quotesCreatedThisMonth < limit
    }

    fun remainingFreeQuotes(): Int? {
        val limit = subscriptionTier.monthlyQuoteLimit ?: return null
        return (limit - quotesCreatedThisMonth).coerceAtLeast(0)
    }

    fun defaultTermsFor(language: SupportedLanguage): String =
        when (language) {
            SupportedLanguage.EN -> defaultTermsEn
            SupportedLanguage.ES -> defaultTermsEs
        }

    companion object {
        const val DEFAULT_TERMS_EN =
            "Quote valid for 14 days. A deposit is due upon approval; remaining balance is due upon completion."
        const val DEFAULT_TERMS_ES =
            "Cotización válida por 14 días. Se requiere un depósito al aprobar; el saldo restante se paga al finalizar el trabajo."
    }
}
