package com.example.domain.model

import kotlin.math.roundToLong

/**
 * Individual labor, material, equipment, or permit line item belonging to a Quote or Invoice.
 * All currency math uses integer cents to prevent floating-point rounding drift.
 */
data class LineItem(
    val id: String,
    val quoteId: String,
    val sortOrder: Int = 0,
    val name: String,
    val description: String = "",
    val category: LineItemCategory = LineItemCategory.LABOR,
    val quantity: Double = 1.0,
    val unit: PricingUnit = PricingUnit.EACH,
    /** Unit price in integer cents (e.g., 8500L = $85.00). */
    val unitPriceCents: Long,
    val taxable: Boolean = true,
    val priceSource: PriceSource = PriceSource.PRICE_BOOK,
    val priceBookItemId: String? = null,
    /** Optional 0.0..1.0 confidence score returned by LLM extraction for review cues. */
    val aiConfidence: Float? = null
) {
    val lineTotalCents: Long
        get() = (quantity * unitPriceCents.toDouble()).roundToLong()

    fun requiresPriceReview(): Boolean = priceSource == PriceSource.AI_ESTIMATE
}
