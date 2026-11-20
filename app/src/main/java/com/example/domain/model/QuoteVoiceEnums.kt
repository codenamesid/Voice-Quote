package com.example.domain.model

enum class TradeType(val displayNameEn: String, val displayNameEs: String) {
    PLUMBING("Plumbing", "Plomería"),
    ELECTRICAL("Electrical", "Electricidad"),
    HANDYMAN("Handyman", "Mantenimiento General"),
    PAINTING("Painting", "Pintura"),
    LANDSCAPING("Landscaping", "Jardinería"),
    GENERAL("General Contracting", "Contratista General");

    companion object {
        fun fromNameOrDefault(raw: String?, default: TradeType = HANDYMAN): TradeType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}

enum class SupportedLanguage(val code: String, val localeTag: String, val label: String) {
    EN("en", "en-US", "English"),
    ES("es", "es-US", "Español");

    companion object {
        fun fromCodeOrDefault(raw: String?, default: SupportedLanguage = EN): SupportedLanguage =
            entries.firstOrNull {
                it.code.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            } ?: default
    }
}

enum class LineItemCategory(val labelEn: String, val labelEs: String) {
    LABOR("Labor", "Mano de obra"),
    MATERIAL("Materials", "Materiales"),
    EQUIPMENT("Equipment", "Equipo"),
    PERMIT("Permit / Fee", "Permiso / Tarifa"),
    OTHER("Other", "Otro");

    companion object {
        fun fromNameOrDefault(raw: String?, default: LineItemCategory = LABOR): LineItemCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}

enum class PricingUnit(val labelEn: String, val labelEs: String) {
    EACH("ea", "c/u"),
    HOUR("hr", "hr"),
    SQ_FT("sq ft", "pie²"),
    LINEAR_FT("lin ft", "pie lin"),
    DAY("day", "día"),
    JOB("job", "trabajo");

    companion object {
        fun fromNameOrDefault(raw: String?, default: PricingUnit = EACH): PricingUnit =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}

enum class PriceSource {
    PRICE_BOOK,
    AI_ESTIMATE,
    MANUAL;

    companion object {
        fun fromNameOrDefault(raw: String?, default: PriceSource = AI_ESTIMATE): PriceSource =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}

enum class QuoteStatus {
    DRAFT,
    SENT,
    VIEWED,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    INVOICED;

    companion object {
        fun fromNameOrDefault(raw: String?, default: QuoteStatus = DRAFT): QuoteStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}

enum class InvoiceStatus {
    DRAFT,
    SENT,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    VOID;

    companion object {
        fun fromNameOrDefault(raw: String?, default: InvoiceStatus = DRAFT): InvoiceStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}

enum class SubscriptionTier(val monthlyQuoteLimit: Int?) {
    FREE(monthlyQuoteLimit = 5),
    PRO_MONTHLY(monthlyQuoteLimit = null),
    PRO_YEARLY(monthlyQuoteLimit = null);

    fun hasUnlimitedQuotes(): Boolean = monthlyQuoteLimit == null

    companion object {
        fun fromNameOrDefault(raw: String?, default: SubscriptionTier = FREE): SubscriptionTier =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}

enum class SyncStatus {
    SYNCED,
    PENDING_UPSERT,
    PENDING_DELETE,
    FAILED;

    companion object {
        fun fromNameOrDefault(raw: String?, default: SyncStatus = PENDING_UPSERT): SyncStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}
