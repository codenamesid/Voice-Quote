package com.example.domain.pricing

import com.example.domain.model.LineItem
import com.example.domain.model.LineItemCategory
import com.example.domain.model.PriceBookItem
import com.example.domain.model.PriceSource
import com.example.domain.model.PricingUnit
import com.example.domain.model.SupportedLanguage
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

/**
 * Extracted line item candidate returned by the AI extraction service prior to
 * deterministic Price Book pricing.
 */
data class ExtractedLineItemCandidate(
    val nameEn: String,
    val nameEs: String,
    val description: String = "",
    val category: LineItemCategory = LineItemCategory.LABOR,
    val quantity: Double = 1.0,
    val unit: PricingUnit = PricingUnit.EACH,
    val suggestedUnitPriceCents: Long,
    val taxable: Boolean = true,
    val confidence: Float = 0.85f
)

/**
 * Matches extracted voice line items against the contractor's saved Price Book using
 * accent-insensitive EN/ES normalization, keyword aliases, and token similarity.
 *
 * - Matched items receive exact saved unit prices and [PriceSource.PRICE_BOOK].
 * - Unmatched items fall back to the LLM suggested price and are flagged [PriceSource.AI_ESTIMATE].
 */
object PriceBookMatcher {

    private val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val NON_ALNUM_REGEX = "[^a-z0-9\\s]".toRegex()

    fun priceLineItems(
        quoteId: String,
        candidates: List<ExtractedLineItemCandidate>,
        priceBook: List<PriceBookItem>,
        outputLanguage: SupportedLanguage
    ): List<LineItem> {
        val activeBook = priceBook.filterNot { it.archived }
        return candidates.mapIndexed { index, candidate ->
            val bestMatch = findBestMatch(candidate, activeBook)
            if (bestMatch != null) {
                LineItem(
                    id = UUID.randomUUID().toString(),
                    quoteId = quoteId,
                    sortOrder = index,
                    name = bestMatch.localizedName(outputLanguage),
                    description = candidate.description,
                    category = bestMatch.category,
                    quantity = candidate.quantity.coerceAtLeast(0.01),
                    unit = bestMatch.unit,
                    unitPriceCents = bestMatch.unitPriceCents,
                    taxable = bestMatch.taxable,
                    priceSource = PriceSource.PRICE_BOOK,
                    priceBookItemId = bestMatch.id,
                    aiConfidence = candidate.confidence
                )
            } else {
                val localizedName = when (outputLanguage) {
                    SupportedLanguage.EN -> candidate.nameEn
                    SupportedLanguage.ES -> candidate.nameEs.ifBlank { candidate.nameEn }
                }
                LineItem(
                    id = UUID.randomUUID().toString(),
                    quoteId = quoteId,
                    sortOrder = index,
                    name = localizedName,
                    description = candidate.description,
                    category = candidate.category,
                    quantity = candidate.quantity.coerceAtLeast(0.01),
                    unit = candidate.unit,
                    unitPriceCents = candidate.suggestedUnitPriceCents.coerceAtLeast(0L),
                    taxable = candidate.taxable,
                    priceSource = PriceSource.AI_ESTIMATE,
                    priceBookItemId = null,
                    aiConfidence = candidate.confidence
                )
            }
        }
    }

    fun findBestMatch(
        candidate: ExtractedLineItemCandidate,
        priceBook: List<PriceBookItem>
    ): PriceBookItem? {
        if (priceBook.isEmpty()) return null

        val queryForms = listOf(candidate.nameEn, candidate.nameEs, candidate.description)
            .map { normalize(it) }
            .filter { it.isNotBlank() }

        var bestItem: PriceBookItem? = null
        var bestScore = 0.0

        for (item in priceBook) {
            val score = scoreMatch(queryForms, candidate, item)
            if (score > bestScore) {
                bestScore = score
                bestItem = item
            }
        }

        return if (bestScore >= 0.45) bestItem else null
    }

    private fun scoreMatch(
        queryForms: List<String>,
        candidate: ExtractedLineItemCandidate,
        item: PriceBookItem
    ): Double {
        val targetForms = buildList {
            add(normalize(item.name))
            if (item.nameEs.isNotBlank()) add(normalize(item.nameEs))
            item.keywords.forEach { add(normalize(it)) }
        }.filter { it.isNotBlank() }

        var maxScore = 0.0
        for (q in queryForms) {
            val qTokens = tokenize(q)
            for (t in targetForms) {
                if (q == t) return 1.0
                if (q.contains(t) && t.length >= 4) {
                    maxScore = maxOf(maxScore, 0.85)
                } else if (t.contains(q) && q.length >= 4) {
                    maxScore = maxOf(maxScore, 0.78)
                }
                val tTokens = tokenize(t)
                if (qTokens.isNotEmpty() && tTokens.isNotEmpty()) {
                    val intersection = qTokens.intersect(tTokens).size
                    val union = qTokens.union(tTokens).size
                    val jaccard = intersection.toDouble() / union.toDouble()
                    val containment = intersection.toDouble() / minOf(qTokens.size, tTokens.size).toDouble()
                    val combined = (jaccard * 0.6) + (containment * 0.4)
                    if (combined > maxScore) maxScore = combined
                }
            }
        }

        // Small boost when category matches
        if (maxScore >= 0.40 && candidate.category == item.category) {
            maxScore = (maxScore + 0.08).coerceAtMost(0.99)
        }
        return maxScore
    }

    internal fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(Locale.US), Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(decomposed, "")
            .replace(NON_ALNUM_REGEX, " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun tokenize(normalized: String): Set<String> {
        val stopWords = setOf(
            "the", "and", "for", "with", "of", "a", "an", "new", "old", "one", "two",
            "de", "la", "el", "en", "y", "con", "para", "un", "una", "del", "los", "las"
        )
        return normalized.split(" ")
            .filter { it.length >= 2 && it !in stopWords }
            .toSet()
    }
}

object CurrencyFormatter {
    fun formatCents(cents: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        return formatter.format(cents / 100.0)
    }

    fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
    }
}
