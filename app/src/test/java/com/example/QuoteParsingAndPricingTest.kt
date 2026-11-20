package com.example

import com.example.data.ai.PlaceholderQuoteExtractionRepository
import com.example.data.ai.QuoteExtractionJsonParser
import com.example.data.ai.SampleTranscriptFixtures
import com.example.domain.model.Business
import com.example.domain.model.Invoice
import com.example.domain.model.LineItem
import com.example.domain.model.LineItemCategory
import com.example.domain.model.PriceSource
import com.example.domain.model.PricingUnit
import com.example.domain.model.Quote
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TradeType
import com.example.domain.pricing.CurrencyFormatter
import com.example.domain.pricing.ExtractedLineItemCandidate
import com.example.domain.pricing.PriceBookMatcher
import com.example.domain.pricing.TradePriceBookSeeder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deliverable 6: Unit tests for LLM JSON parsing, all 5 EN/ES test transcripts,
 * deterministic price-book matching, integer-cent calculations, and Free-tier quota enforcement.
 */
class QuoteParsingAndPricingTest {

    @Test
    fun `all five test transcripts parse strictly with two in Spanish`() {
        val fixtures = SampleTranscriptFixtures.all
        assertEquals(5, fixtures.size)
        assertEquals(2, fixtures.count { it.spokenLanguage == SupportedLanguage.ES })
        assertEquals(3, fixtures.count { it.spokenLanguage == SupportedLanguage.EN })

        fixtures.forEach { fixture ->
            val draft = QuoteExtractionJsonParser.parse(fixture.sampleJsonResponse)
            assertEquals(fixture.spokenLanguage, draft.spokenLanguage)
            assertEquals(fixture.expectedOutputLanguage, draft.preferredOutputLanguage)
            assertTrue("Customer name should be extracted for ${fixture.id}", draft.customer.name.isNotBlank())
            assertTrue("Line items should be non-empty for ${fixture.id}", draft.lineItems.isNotEmpty())
        }
    }

    @Test
    fun `price book matcher applies saved prices and flags unknown items as AI estimates`() = runTest {
        val electricalBook = TradePriceBookSeeder.seedForTrade("biz-1", TradeType.ELECTRICAL)
        val evFixture = SampleTranscriptFixtures.all.first { it.id == "en_electrical_ev_charger" }
        val repo = PlaceholderQuoteExtractionRepository()

        val draft = repo.extractQuote(
            transcript = evFixture.transcript,
            trade = TradeType.ELECTRICAL,
            spokenLanguageHint = SupportedLanguage.EN,
            priceBook = electricalBook
        ).getOrThrow()

        val priced = PriceBookMatcher.priceLineItems(
            quoteId = "q-ev",
            candidates = draft.lineItems,
            priceBook = electricalBook,
            outputLanguage = SupportedLanguage.EN
        )

        // 4 items match the electrical price book, 1 weatherproof junction box is flagged as AI_ESTIMATE
        val matchedCount = priced.count { it.priceSource == PriceSource.PRICE_BOOK }
        val estimateCount = priced.count { it.priceSource == PriceSource.AI_ESTIMATE }
        assertEquals(4, matchedCount)
        assertEquals(1, estimateCount)

        // Verify saved price book rate ($14.00/lin ft = 1400 cents) overrides LLM suggested $15.00
        val wireItem = priced.first { it.name.contains("Copper Wire", ignoreCase = true) }
        assertEquals(1400L, wireItem.unitPriceCents)
        assertEquals(35.0, wireItem.quantity, 0.001)
        assertEquals(49000L, wireItem.lineTotalCents)
    }

    @Test
    fun `spanish transcript matches accent-insensitive plumbing price book and outputs Spanish names`() = runTest {
        val plumbingBook = TradePriceBookSeeder.seedForTrade("biz-2", TradeType.PLUMBING)
        val spanishFixture = SampleTranscriptFixtures.all.first { it.id == "es_plumbing_kitchen_leak" }
        val repo = PlaceholderQuoteExtractionRepository()

        val draft = repo.extractQuote(
            transcript = spanishFixture.transcript,
            trade = TradeType.PLUMBING,
            spokenLanguageHint = SupportedLanguage.ES,
            priceBook = plumbingBook
        ).getOrThrow()

        assertEquals(SupportedLanguage.ES, draft.spokenLanguage)
        assertEquals(SupportedLanguage.ES, draft.preferredOutputLanguage)
        assertEquals("Rosa Delgado", draft.customer.name)

        val priced = PriceBookMatcher.priceLineItems(
            quoteId = "q-es",
            candidates = draft.lineItems,
            priceBook = plumbingBook,
            outputLanguage = SupportedLanguage.ES
        )

        val faucet = priced.first { it.name.contains("mezcladora", ignoreCase = true) }
        assertEquals(PriceSource.PRICE_BOOK, faucet.priceSource)
        assertEquals(16500L, faucet.unitPriceCents)

        // Silicone reseal is not in the starter price book -> flagged as AI_ESTIMATE
        val silicone = priced.first { it.name.contains("silicón", ignoreCase = true) }
        assertEquals(PriceSource.AI_ESTIMATE, silicone.priceSource)
    }

    @Test
    fun `cross-language transcript outputs English quote from Spanish speech`() = runTest {
        val landscapeBook = TradePriceBookSeeder.seedForTrade("biz-3", TradeType.LANDSCAPING)
        val fixture = SampleTranscriptFixtures.all.first { it.id == "es_landscaping_cross_lang_en" }
        val repo = PlaceholderQuoteExtractionRepository()

        val draft = repo.extractQuote(
            transcript = fixture.transcript,
            trade = TradeType.LANDSCAPING,
            spokenLanguageHint = SupportedLanguage.ES,
            priceBook = landscapeBook
        ).getOrThrow()

        assertEquals(SupportedLanguage.ES, draft.spokenLanguage)
        assertEquals(SupportedLanguage.EN, draft.preferredOutputLanguage)

        val priced = PriceBookMatcher.priceLineItems(
            quoteId = "q-cross",
            candidates = draft.lineItems,
            priceBook = landscapeBook,
            outputLanguage = SupportedLanguage.EN
        )

        val mulch = priced.first { it.name.contains("Mulch", ignoreCase = true) }
        assertEquals(PriceSource.PRICE_BOOK, mulch.priceSource)
        assertEquals(6.0, mulch.quantity, 0.001)
        assertEquals(5700L, mulch.lineTotalCents) // 6 * $9.50 = $57.00
    }

    @Test
    fun `quote and invoice integer-cent totals, tax basis points, deposit, and balance due are exact`() {
        val labor = LineItem(
            id = "li-1",
            quoteId = "q-1",
            name = "Labor",
            category = LineItemCategory.LABOR,
            quantity = 2.5,
            unit = PricingUnit.HOUR,
            unitPriceCents = 10000L, // $100.00 -> $250.00 (non-taxable)
            taxable = false
        )
        val material = LineItem(
            id = "li-2",
            quoteId = "q-1",
            name = "Pipe & Fittings",
            category = LineItemCategory.MATERIAL,
            quantity = 1.0,
            unit = PricingUnit.JOB,
            unitPriceCents = 20000L, // $200.00 (taxable at 8.25% = $16.50)
            taxable = true
        )

        val quote = Quote(
            id = "q-1",
            businessId = "b-1",
            quoteNumber = "QV-1001",
            lineItems = listOf(labor, material),
            taxRateBps = 825, // 8.25%
            depositPercent = 30
        )

        assertEquals(45000L, quote.subtotalCents)
        assertEquals(20000L, quote.taxableSubtotalCents)
        assertEquals(1650L, quote.taxCents)
        assertEquals(46650L, quote.totalCents)
        assertEquals(13995L, quote.depositCents) // 30% of $466.50 = $139.95
        assertEquals("$466.50", CurrencyFormatter.formatCents(quote.totalCents))

        val invoice = Invoice(
            id = "inv-1",
            businessId = "b-1",
            quoteId = quote.id,
            invoiceNumber = "INV-2001",
            lineItems = quote.lineItems,
            taxRateBps = quote.taxRateBps,
            amountPaidCents = quote.depositCents,
            dueDateEpochMs = 0L
        )
        assertEquals(32655L, invoice.balanceDueCents) // $466.50 - $139.95 = $326.55
    }

    @Test
    fun `free tier enforces 5 quotes per month while pro is unlimited`() {
        val freeBiz = Business(
            id = "b-free",
            ownerUid = "u1",
            name = "Solo Plumbing",
            subscriptionTier = SubscriptionTier.FREE,
            quotesCreatedThisMonth = 5
        )
        assertFalse(freeBiz.canCreateQuote())
        assertEquals(0, freeBiz.remainingFreeQuotes())

        val proBiz = freeBiz.copy(subscriptionTier = SubscriptionTier.PRO_MONTHLY)
        assertTrue(proBiz.canCreateQuote())
        assertEquals(null, proBiz.remainingFreeQuotes())
    }

    @Test
    fun `free-form unmatched transcript produces fallback estimate items`() = runTest {
        val repo = PlaceholderQuoteExtractionRepository()
        val result = repo.extractQuote(
            transcript = "Estimate for John Doe at 123 Main St. 3 hours of custom skylight flashing repair.",
            trade = TradeType.HANDYMAN,
            spokenLanguageHint = SupportedLanguage.EN,
            priceBook = emptyList()
        ).getOrThrow()

        assertEquals("John Doe", result.customer.name)
        assertTrue(result.lineItems.isNotEmpty())
        assertTrue(result.isPlaceholderMode)

        val priced = PriceBookMatcher.priceLineItems(
            quoteId = "q-freeform",
            candidates = result.lineItems,
            priceBook = emptyList(),
            outputLanguage = SupportedLanguage.EN
        )
        assertTrue(priced.all { it.priceSource == PriceSource.AI_ESTIMATE })
    }

    @Test
    fun `accent normalization matches Spanish price book aliases`() {
        val item = TradePriceBookSeeder.seedForTrade("b", TradeType.PLUMBING)
            .first { it.name.contains("Angle Stop", ignoreCase = true) }
        val candidate = ExtractedLineItemCandidate(
            nameEn = "Valvula angular de cierre",
            nameEs = "Válvula angular de cierre",
            category = LineItemCategory.MATERIAL,
            quantity = 2.0,
            unit = PricingUnit.EACH,
            suggestedUnitPriceCents = 4000L
        )
        val matched = PriceBookMatcher.findBestMatch(candidate, listOf(item))
        assertEquals(item.id, matched?.id)
    }
}
