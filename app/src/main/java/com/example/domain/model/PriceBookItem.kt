package com.example.domain.model

/**
 * Saved contractor price book entry used to price extracted voice line items
 * deterministically before falling back to AI price estimates.
 */
data class PriceBookItem(
    val id: String,
    val businessId: String,
    val name: String,
    /** Optional Spanish display name for generating bilingual quotes seamlessly. */
    val nameEs: String = "",
    val category: LineItemCategory = LineItemCategory.LABOR,
    val unit: PricingUnit = PricingUnit.EACH,
    /** Unit price in integer cents (e.g., 12500L = $125.00). */
    val unitPriceCents: Long,
    val defaultQuantity: Double = 1.0,
    /** English and Spanish aliases/synonyms used by the local price-book matcher. */
    val keywords: List<String> = emptyList(),
    val taxable: Boolean = true,
    val archived: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPSERT
) {
    fun localizedName(language: SupportedLanguage): String =
        when (language) {
            SupportedLanguage.EN -> name
            SupportedLanguage.ES -> nameEs.ifBlank { name }
        }
}
