package com.example.data.ai

import com.example.domain.model.LineItemCategory
import com.example.domain.model.PriceBookItem
import com.example.domain.model.PricingUnit
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TradeType
import com.example.domain.pricing.ExtractedLineItemCandidate
import com.example.domain.pricing.PriceBookMatcher
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class ExtractedCustomerDraft(
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = ""
)

data class ExtractedQuoteDraft(
    val spokenLanguage: SupportedLanguage,
    val preferredOutputLanguage: SupportedLanguage,
    val jobTitle: String,
    val customer: ExtractedCustomerDraft,
    val lineItems: List<ExtractedLineItemCandidate>,
    val notes: String,
    val isPlaceholderMode: Boolean = true
)

data class SampleTranscriptFixture(
    val id: String,
    val label: String,
    val trade: TradeType,
    val spokenLanguage: SupportedLanguage,
    val expectedOutputLanguage: SupportedLanguage,
    val transcript: String,
    val sampleJsonResponse: String
)

/**
 * Single AI boundary for QuoteVoice.
 *
 * When Firebase is enabled, swap [PlaceholderQuoteExtractionRepository] for a
 * Firebase AI Logic (`gemini-3.8-flash` with strict JSON `responseSchema`) or
 * thin Cloud Function Claude adapter implementing this exact interface.
 */
interface QuoteExtractionRepository {
    val isPlaceholderMode: Boolean
    suspend fun extractQuote(
        transcript: String,
        trade: TradeType,
        spokenLanguageHint: SupportedLanguage,
        priceBook: List<PriceBookItem>
    ): Result<ExtractedQuoteDraft>
}

private data class RawCustomerPayload(
    val name: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null
)

private data class RawLineItemPayload(
    val name: String? = null,
    val nameEs: String? = null,
    val description: String? = null,
    val category: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val suggestedUnitPriceCents: Long? = null,
    val taxable: Boolean? = null,
    val confidence: Float? = null
)

private data class RawExtractionPayload(
    val spokenLanguage: String? = null,
    val preferredOutputLanguage: String? = null,
    val jobTitle: String? = null,
    val customer: RawCustomerPayload? = null,
    val lineItems: List<RawLineItemPayload>? = null,
    val notes: String? = null
)

/**
 * Parses strict LLM JSON payloads adhering to docs/LLM_PROMPT_AND_SCHEMA.md into
 * typed [ExtractedQuoteDraft] objects using Moshi (pure JVM & Android compatible).
 */
object QuoteExtractionJsonParser {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(RawExtractionPayload::class.java)

    fun parse(rawJson: String, isPlaceholderMode: Boolean = false): ExtractedQuoteDraft {
        val root = adapter.fromJson(rawJson.trim())
            ?: throw IllegalArgumentException("Empty or invalid JSON payload.")

        val spoken = SupportedLanguage.fromCodeOrDefault(root.spokenLanguage)
        val output = SupportedLanguage.fromCodeOrDefault(
            root.preferredOutputLanguage ?: spoken.code,
            default = spoken
        )
        val jobTitle = root.jobTitle.orEmpty().trim().ifBlank {
            if (output == SupportedLanguage.ES) "Cotización de Trabajo" else "Service Quote"
        }

        val customer = ExtractedCustomerDraft(
            name = root.customer?.name.orEmpty().trim(),
            address = root.customer?.address.orEmpty().trim(),
            phone = root.customer?.phone.orEmpty().trim(),
            email = root.customer?.email.orEmpty().trim()
        )

        val candidates = root.lineItems.orEmpty().mapNotNull { obj ->
            val nameEn = obj.name.orEmpty().trim()
            if (nameEn.isBlank()) return@mapNotNull null
            ExtractedLineItemCandidate(
                nameEn = nameEn,
                nameEs = obj.nameEs.orEmpty().trim().ifBlank { nameEn },
                description = obj.description.orEmpty().trim(),
                category = LineItemCategory.fromNameOrDefault(obj.category),
                quantity = (obj.quantity ?: 1.0).coerceAtLeast(0.01),
                unit = PricingUnit.fromNameOrDefault(obj.unit),
                suggestedUnitPriceCents = (obj.suggestedUnitPriceCents ?: 0L).coerceAtLeast(0L),
                taxable = obj.taxable ?: true,
                confidence = (obj.confidence ?: 0.85f).coerceIn(0f, 1f)
            )
        }

        require(candidates.isNotEmpty()) { "Extracted JSON must contain at least one valid line item." }

        return ExtractedQuoteDraft(
            spokenLanguage = spoken,
            preferredOutputLanguage = output,
            jobTitle = jobTitle,
            customer = customer,
            lineItems = candidates,
            notes = root.notes.orEmpty().trim(),
            isPlaceholderMode = isPlaceholderMode
        )
    }
}

/**
 * Five realistic field-contractor test transcripts (3 English, 2 Spanish) plus
 * their schema-compliant JSON extraction payloads (Deliverable 3).
 */
object SampleTranscriptFixtures {

    val all: List<SampleTranscriptFixture> = listOf(
        SampleTranscriptFixture(
            id = "en_plumbing_water_heater",
            label = "EN • Plumbing: 50-Gal Water Heater",
            trade = TradeType.PLUMBING,
            spokenLanguage = SupportedLanguage.EN,
            expectedOutputLanguage = SupportedLanguage.EN,
            transcript = "Hey, quote for Marcus Henderson at 418 Oak Ridge Drive, phone 512-555-0194. " +
                "We are replacing a leaking 50-gallon gas water heater in the garage. " +
                "Put down one 50-gallon gas water heater install, two braided flex supply lines, " +
                "one new thermal expansion tank, and haul away of the old unit. " +
                "Customer wants the quote in English. Note: shutoff valve is tight behind the washer, allow half day.",
            sampleJsonResponse = """
                {
                  "spokenLanguage": "en",
                  "preferredOutputLanguage": "en",
                  "jobTitle": "50-Gallon Gas Water Heater Replacement",
                  "customer": {
                    "name": "Marcus Henderson",
                    "address": "418 Oak Ridge Drive",
                    "phone": "512-555-0194",
                    "email": ""
                  },
                  "lineItems": [
                    {
                      "name": "50-Gallon Gas Water Heater Install",
                      "nameEs": "Instalación de calentador de agua a gas 50 gal",
                      "description": "Remove leaking unit and install new 50-gal gas water heater in garage",
                      "category": "LABOR",
                      "quantity": 1.0,
                      "unit": "JOB",
                      "suggestedUnitPriceCents": 150000,
                      "taxable": true,
                      "confidence": 0.96
                    },
                    {
                      "name": "Braided Flex Supply Line",
                      "nameEs": "Manguera flexible trenzada",
                      "description": "Hot and cold stainless flex connectors",
                      "category": "MATERIAL",
                      "quantity": 2.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 2500,
                      "taxable": true,
                      "confidence": 0.94
                    },
                    {
                      "name": "Thermal Expansion Tank",
                      "nameEs": "Tanque de expansión térmica",
                      "description": "2-gallon potable thermal expansion tank",
                      "category": "MATERIAL",
                      "quantity": 1.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 14000,
                      "taxable": true,
                      "confidence": 0.92
                    },
                    {
                      "name": "Old Unit Haul Away & Disposal",
                      "nameEs": "Retiro y disposición de equipo viejo",
                      "description": "Drain and recycle old leaking heater",
                      "category": "OTHER",
                      "quantity": 1.0,
                      "unit": "JOB",
                      "suggestedUnitPriceCents": 9000,
                      "taxable": false,
                      "confidence": 0.95
                    }
                  ],
                  "notes": "Shutoff valve is tight behind the washer; allow half day for install."
                }
            """.trimIndent()
        ),
        SampleTranscriptFixture(
            id = "en_electrical_ev_charger",
            label = "EN • Electrical: Level 2 EV Charger",
            trade = TradeType.ELECTRICAL,
            spokenLanguage = SupportedLanguage.EN,
            expectedOutputLanguage = SupportedLanguage.EN,
            transcript = "New estimate for Sarah Jenkins over at 2905 Maple Creek Lane. " +
                "She wants a Level 2 EV charger installed in the attached garage. " +
                "That's one 240V EV charger installation, 35 linear feet of 6-gauge copper wire conduit run, " +
                "one 60-amp breaker, and a city electrical permit. Also add a heavy-duty weatherproof junction box. " +
                "Send in English. Note: panel has two open slots on the bottom right.",
            sampleJsonResponse = """
                {
                  "spokenLanguage": "en",
                  "preferredOutputLanguage": "en",
                  "jobTitle": "Level 2 EV Charger Installation",
                  "customer": {
                    "name": "Sarah Jenkins",
                    "address": "2905 Maple Creek Lane",
                    "phone": "",
                    "email": ""
                  },
                  "lineItems": [
                    {
                      "name": "Level 2 EV Charger Installation (240V)",
                      "nameEs": "Instalación de cargador EV Nivel 2 (240V)",
                      "description": "Mount customer-supplied charger in attached garage and test load",
                      "category": "LABOR",
                      "quantity": 1.0,
                      "unit": "JOB",
                      "suggestedUnitPriceCents": 65000,
                      "taxable": false,
                      "confidence": 0.97
                    },
                    {
                      "name": "6-Gauge Copper Wire & Conduit Run",
                      "nameEs": "Cable de cobre calibre 6 y tubería conduit",
                      "description": "35 ft EMT conduit run with 6 AWG copper THHN conductors",
                      "category": "MATERIAL",
                      "quantity": 35.0,
                      "unit": "LINEAR_FT",
                      "suggestedUnitPriceCents": 1500,
                      "taxable": true,
                      "confidence": 0.93
                    },
                    {
                      "name": "60-Amp 2-Pole Circuit Breaker",
                      "nameEs": "Interruptor termomagnético 60A 2 polos",
                      "description": "Dedicated 60A breaker in main panel",
                      "category": "MATERIAL",
                      "quantity": 1.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 8000,
                      "taxable": true,
                      "confidence": 0.95
                    },
                    {
                      "name": "Heavy-Duty Weatherproof Junction Box",
                      "nameEs": "Caja de conexiones impermeable reforzada",
                      "description": "NEMA 3R outdoor-rated splice enclosure",
                      "category": "MATERIAL",
                      "quantity": 1.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 6200,
                      "taxable": true,
                      "confidence": 0.78
                    },
                    {
                      "name": "City Electrical Permit",
                      "nameEs": "Permiso eléctrico municipal",
                      "description": "Municipal permit filing and inspection coordination",
                      "category": "PERMIT",
                      "quantity": 1.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 15000,
                      "taxable": false,
                      "confidence": 0.96
                    }
                  ],
                  "notes": "Main panel has two open slots on the bottom right."
                }
            """.trimIndent()
        ),
        SampleTranscriptFixture(
            id = "en_painting_cross_lang_es",
            label = "EN → ES • Painting: Exterior Trim (Spanish PDF)",
            trade = TradeType.PAINTING,
            spokenLanguage = SupportedLanguage.EN,
            expectedOutputLanguage = SupportedLanguage.ES,
            transcript = "Quote for Carlos Mendez at 812 Sunset Boulevard, phone 305-555-0148. " +
                "Exterior trim and front door refresh. Give him 6 hours of exterior prep and scraping, " +
                "3 gallons of exterior satin paint, 1 front entry door refinish, and masking tape and plastic drop cloths. " +
                "Please send the final quote in Spanish because his mom reviews the paperwork.",
            sampleJsonResponse = """
                {
                  "spokenLanguage": "en",
                  "preferredOutputLanguage": "es",
                  "jobTitle": "Pintura de Molduras Exteriores y Puerta",
                  "customer": {
                    "name": "Carlos Mendez",
                    "address": "812 Sunset Boulevard",
                    "phone": "305-555-0148",
                    "email": ""
                  },
                  "lineItems": [
                    {
                      "name": "Exterior Prep, Scraping & Sanding",
                      "nameEs": "Preparación exterior, raspado y lijado",
                      "description": "Scrape peeling trim, sand smooth, and spot-prime bare wood",
                      "category": "LABOR",
                      "quantity": 6.0,
                      "unit": "HOUR",
                      "suggestedUnitPriceCents": 8500,
                      "taxable": false,
                      "confidence": 0.95
                    },
                    {
                      "name": "Exterior Satin Paint (Premium)",
                      "nameEs": "Pintura exterior satinada (Premium)",
                      "description": "Weather-shield satin finish paint",
                      "category": "MATERIAL",
                      "quantity": 3.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 7000,
                      "taxable": true,
                      "confidence": 0.94
                    },
                    {
                      "name": "Front Entry Door Refinish",
                      "nameEs": "Acabado y pintura de puerta principal",
                      "description": "Prep and apply two finish coats to front entry door",
                      "category": "LABOR",
                      "quantity": 1.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 24000,
                      "taxable": false,
                      "confidence": 0.91
                    },
                    {
                      "name": "Masking Tape, Plastic & Drop Cloths",
                      "nameEs": "Cinta de enmascarar, plástico y lonas",
                      "description": "Surface protection for windows, porch, and shrubs",
                      "category": "MATERIAL",
                      "quantity": 1.0,
                      "unit": "JOB",
                      "suggestedUnitPriceCents": 4500,
                      "taxable": true,
                      "confidence": 0.90
                    }
                  ],
                  "notes": "Enviar cotización en español."
                }
            """.trimIndent()
        ),
        SampleTranscriptFixture(
            id = "es_plumbing_kitchen_leak",
            label = "ES • Plomería: Fuga en Fregadero",
            trade = TradeType.PLUMBING,
            spokenLanguage = SupportedLanguage.ES,
            expectedOutputLanguage = SupportedLanguage.ES,
            transcript = "Cotización para Rosa Delgado en 1540 Avenida Cypress, teléfono 713-555-0182. " +
                "Tiene fuga debajo del fregadero de la cocina. Incluye dos horas de mano de obra de plomería, " +
                "una llave mezcladora de cocina nueva, un kit de trampa P de PVC, dos válvulas angulares de cierre, " +
                "y sellado con silicón impermeable alrededor de la tarja. " +
                "Nota: el gabinete tiene humedad leve en la base, dejar secar antes de sellar.",
            sampleJsonResponse = """
                {
                  "spokenLanguage": "es",
                  "preferredOutputLanguage": "es",
                  "jobTitle": "Reparación de Fuga y Llave de Cocina",
                  "customer": {
                    "name": "Rosa Delgado",
                    "address": "1540 Avenida Cypress",
                    "phone": "713-555-0182",
                    "email": ""
                  },
                  "lineItems": [
                    {
                      "name": "Plumbing Service Labor",
                      "nameEs": "Mano de obra de plomería",
                      "description": "Desmontaje, reparación de fuga y pruebas de presión",
                      "category": "LABOR",
                      "quantity": 2.0,
                      "unit": "HOUR",
                      "suggestedUnitPriceCents": 11500,
                      "taxable": false,
                      "confidence": 0.96
                    },
                    {
                      "name": "Kitchen Faucet Replacement",
                      "nameEs": "Llave mezcladora de cocina",
                      "description": "Suministro e instalación de mezcladora nueva",
                      "category": "MATERIAL",
                      "quantity": 1.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 16500,
                      "taxable": true,
                      "confidence": 0.93
                    },
                    {
                      "name": "PVC P-Trap Drain Kit",
                      "nameEs": "Kit de trampa P de PVC",
                      "description": "Reemplazo completo de desagüe bajo fregadero",
                      "category": "MATERIAL",
                      "quantity": 1.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 2800,
                      "taxable": true,
                      "confidence": 0.95
                    },
                    {
                      "name": "Angle Stop Shutoff Valve",
                      "nameEs": "Válvula angular de cierre",
                      "description": "Válvulas de cierre para agua fría y caliente",
                      "category": "MATERIAL",
                      "quantity": 2.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 3200,
                      "taxable": true,
                      "confidence": 0.94
                    },
                    {
                      "name": "Waterproof Sink Silicone Reseal",
                      "nameEs": "Sellado con silicón impermeable en tarja",
                      "description": "Retirar silicón dañado y aplicar cordón antimoho",
                      "category": "MATERIAL",
                      "quantity": 1.0,
                      "unit": "JOB",
                      "suggestedUnitPriceCents": 4500,
                      "taxable": true,
                      "confidence": 0.80
                    }
                  ],
                  "notes": "El gabinete tiene humedad leve en la base; dejar secar antes de sellar."
                }
            """.trimIndent()
        ),
        SampleTranscriptFixture(
            id = "es_landscaping_cross_lang_en",
            label = "ES → EN • Jardinería: Limpieza de Patio (English PDF)",
            trade = TradeType.LANDSCAPING,
            spokenLanguage = SupportedLanguage.ES,
            expectedOutputLanguage = SupportedLanguage.EN,
            transcript = "Presupuesto para David Miller en 7420 Pinecrest Way. " +
                "Limpieza completa del patio trasero. Ponle 4 horas de limpieza y poda de arbustos, " +
                "6 bolsas de mulch negro, recorte de césped y orillas del frente y atrás, " +
                "y retiro de ramas y basura verde al tiradero. Mándaselo en inglés por favor. " +
                "Nota: perro grande en el patio, avisar antes de llegar.",
            sampleJsonResponse = """
                {
                  "spokenLanguage": "es",
                  "preferredOutputLanguage": "en",
                  "jobTitle": "Full Backyard Cleanup & Mulch",
                  "customer": {
                    "name": "David Miller",
                    "address": "7420 Pinecrest Way",
                    "phone": "",
                    "email": ""
                  },
                  "lineItems": [
                    {
                      "name": "Yard Cleanup & Shrub Pruning",
                      "nameEs": "Limpieza de patio y poda de arbustos",
                      "description": "Trim overgrown shrubs, weed garden beds, and clear debris",
                      "category": "LABOR",
                      "quantity": 4.0,
                      "unit": "HOUR",
                      "suggestedUnitPriceCents": 7500,
                      "taxable": false,
                      "confidence": 0.96
                    },
                    {
                      "name": "Black Hardwood Mulch (Bag)",
                      "nameEs": "Mulch negro de madera (Bolsa)",
                      "description": "Spread black hardwood mulch across flower beds",
                      "category": "MATERIAL",
                      "quantity": 6.0,
                      "unit": "EACH",
                      "suggestedUnitPriceCents": 950,
                      "taxable": true,
                      "confidence": 0.95
                    },
                    {
                      "name": "Lawn Mowing & Edging (Front & Back)",
                      "nameEs": "Corte de césped y orillas (Frente y atrás)",
                      "description": "Mow, trim edges, and blow walkways",
                      "category": "LABOR",
                      "quantity": 1.0,
                      "unit": "JOB",
                      "suggestedUnitPriceCents": 9500,
                      "taxable": false,
                      "confidence": 0.94
                    },
                    {
                      "name": "Green Waste & Branch Dump Run",
                      "nameEs": "Retiro de ramas y basura verde al tiradero",
                      "description": "Load and haul green waste to local disposal yard",
                      "category": "OTHER",
                      "quantity": 1.0,
                      "unit": "JOB",
                      "suggestedUnitPriceCents": 11000,
                      "taxable": false,
                      "confidence": 0.92
                    }
                  ],
                  "notes": "Large dog in backyard; text customer 15 minutes before arrival."
                }
            """.trimIndent()
        )
    )
}

/**
 * Local placeholder AI extraction repository used when Firebase AI Logic is not enabled.
 *
 * - Matches exact sample transcripts via [QuoteExtractionJsonParser].
 * - Parses free-form spoken/typed English or Spanish transcripts offline using regex +
 *   local [PriceBookItem] keyword scanning so the entire Record -> Edit -> PDF -> Invoice
 *   flow works end-to-end without shipping any API keys.
 */
class PlaceholderQuoteExtractionRepository : QuoteExtractionRepository {
    override val isPlaceholderMode: Boolean = true

    override suspend fun extractQuote(
        transcript: String,
        trade: TradeType,
        spokenLanguageHint: SupportedLanguage,
        priceBook: List<PriceBookItem>
    ): Result<ExtractedQuoteDraft> {
        val cleaned = transcript.trim()
        if (cleaned.isBlank()) {
            return Result.failure(IllegalArgumentException("Transcript cannot be empty."))
        }

        // 1. Check if transcript matches one of our 5 bilingual test fixtures
        val matchedFixture = SampleTranscriptFixtures.all.firstOrNull { fixture ->
            PriceBookMatcher.normalize(fixture.transcript) == PriceBookMatcher.normalize(cleaned)
        }
        if (matchedFixture != null) {
            return runCatching {
                QuoteExtractionJsonParser.parse(
                    rawJson = matchedFixture.sampleJsonResponse,
                    isPlaceholderMode = true
                )
            }
        }

        // 2. Deterministic local placeholder extraction for arbitrary English/Spanish speech
        val lower = cleaned.lowercase()
        val detectedSpoken = if (
            spokenLanguageHint == SupportedLanguage.ES ||
            listOf("cotizacion", "cotización", "presupuesto", "horas", "para ", "trabajo", "plomeria", "plomería")
                .any { lower.contains(it) }
        ) SupportedLanguage.ES else SupportedLanguage.EN

        val outputLang = when {
            lower.contains("in spanish") || lower.contains("en español") || lower.contains("en espanol") ->
                SupportedLanguage.ES
            lower.contains("in english") || lower.contains("en inglés") || lower.contains("en ingles") ->
                SupportedLanguage.EN
            else -> detectedSpoken
        }

        val customerName = extractRegexGroup(
            cleaned,
            "(?:for|para)\\s+([A-ZÁÉÍÓÚÑ][a-zA-Záéíóúñ]+(?:\\s+[A-ZÁÉÍÓÚÑ][a-zA-Záéíóúñ]+)?)"
        )
        val address = extractRegexGroup(
            cleaned,
            "(?:at|over at|en)\\s+(\\d{2,6}\\s+[A-Za-zÁÉÍÓÚÑáéíóúñ0-9\\s]+?(?=[,.]|\\s+phone|\\s+tel|$))"
        )
        val phone = extractRegexGroup(
            cleaned,
            "(\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4})"
        )
        val notes = extractRegexGroup(
            cleaned,
            "(?:note|nota)\\s*[:\\-]\\s*(.+)$"
        )

        val matchedCandidates = mutableListOf<ExtractedLineItemCandidate>()

        // Match any saved price-book items mentioned in the transcript
        val normalizedTranscript = PriceBookMatcher.normalize(cleaned)
        for (bookItem in priceBook.filterNot { it.archived }) {
            val needles = buildList {
                add(PriceBookMatcher.normalize(bookItem.name))
                if (bookItem.nameEs.isNotBlank()) add(PriceBookMatcher.normalize(bookItem.nameEs))
                bookItem.keywords.forEach { add(PriceBookMatcher.normalize(it)) }
            }.filter { it.length >= 4 }

            val hitNeedle = needles.firstOrNull { normalizedTranscript.contains(it) }
            if (hitNeedle != null) {
                val qty = extractQuantityNear(normalizedTranscript, hitNeedle) ?: bookItem.defaultQuantity
                matchedCandidates.add(
                    ExtractedLineItemCandidate(
                        nameEn = bookItem.name,
                        nameEs = bookItem.nameEs.ifBlank { bookItem.name },
                        description = "",
                        category = bookItem.category,
                        quantity = qty,
                        unit = bookItem.unit,
                        suggestedUnitPriceCents = bookItem.unitPriceCents,
                        taxable = bookItem.taxable,
                        confidence = 0.88f
                    )
                )
            }
        }

        // Extract labor hours if mentioned and no hourly labor matched yet
        val hoursMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|horas?)").find(lower)
        if (hoursMatch != null && matchedCandidates.none { it.unit == PricingUnit.HOUR }) {
            val hours = hoursMatch.groupValues[1].toDoubleOrNull() ?: 2.0
            matchedCandidates.add(
                0,
                ExtractedLineItemCandidate(
                    nameEn = "${trade.displayNameEn} Labor (Estimate)",
                    nameEs = "Mano de obra de ${trade.displayNameEs} (Estimado)",
                    description = "Extracted from voice transcript (placeholder mode)",
                    category = LineItemCategory.LABOR,
                    quantity = hours,
                    unit = PricingUnit.HOUR,
                    suggestedUnitPriceCents = 10500L,
                    taxable = false,
                    confidence = 0.72f
                )
            )
        }

        // Always produce at least one editable estimate item if nothing matched the price book
        if (matchedCandidates.isEmpty()) {
            matchedCandidates.add(
                ExtractedLineItemCandidate(
                    nameEn = "${trade.displayNameEn} Service Call (Sample Estimate)",
                    nameEs = "Servicio de ${trade.displayNameEs} (Estimado de prueba)",
                    description = cleaned.take(120),
                    category = LineItemCategory.LABOR,
                    quantity = 2.0,
                    unit = PricingUnit.HOUR,
                    suggestedUnitPriceCents = 10000L,
                    taxable = false,
                    confidence = 0.65f
                )
            )
            matchedCandidates.add(
                ExtractedLineItemCandidate(
                    nameEn = "Job Materials Allowance (Sample Estimate)",
                    nameEs = "Materiales del trabajo (Estimado de prueba)",
                    description = "Tap to adjust parts and pricing",
                    category = LineItemCategory.MATERIAL,
                    quantity = 1.0,
                    unit = PricingUnit.JOB,
                    suggestedUnitPriceCents = 7500L,
                    taxable = true,
                    confidence = 0.60f
                )
            )
        }

        val title = if (outputLang == SupportedLanguage.ES) {
            "Cotización de ${trade.displayNameEs}"
        } else {
            "${trade.displayNameEn} Estimate"
        }

        return Result.success(
            ExtractedQuoteDraft(
                spokenLanguage = detectedSpoken,
                preferredOutputLanguage = outputLang,
                jobTitle = title,
                customer = ExtractedCustomerDraft(
                    name = customerName,
                    address = address,
                    phone = phone
                ),
                lineItems = matchedCandidates,
                notes = notes,
                isPlaceholderMode = true
            )
        )
    }

    private fun extractRegexGroup(text: String, pattern: String): String {
        return Regex(pattern, RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
    }

    private fun extractQuantityNear(normalizedText: String, keyword: String): Double? {
        val idx = normalizedText.indexOf(keyword)
        if (idx <= 0) return null
        val prefix = normalizedText.substring(maxOf(0, idx - 24), idx)
        val numberRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:bags|gallons|feet|ft|linear|hours|hrs|bolsas|galones|horas|pies)?\\s*$")
        numberRegex.find(prefix)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let { return it }

        val wordNumbers = mapOf(
            "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0, "six" to 6.0,
            "un" to 1.0, "una" to 1.0, "dos" to 2.0, "tres" to 3.0, "cuatro" to 4.0, "cinco" to 5.0, "seis" to 6.0
        )
        val lastWord = prefix.trim().split(" ").lastOrNull().orEmpty()
        return wordNumbers[lastWord]
    }
}
