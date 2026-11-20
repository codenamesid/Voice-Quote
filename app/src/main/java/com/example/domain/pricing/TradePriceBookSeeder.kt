package com.example.domain.pricing

import com.example.domain.model.LineItemCategory
import com.example.domain.model.PriceBookItem
import com.example.domain.model.PricingUnit
import com.example.domain.model.SyncStatus
import com.example.domain.model.TradeType
import java.util.UUID

/**
 * Provides trade-specific starter price books seeded during Onboarding so contractors
 * get deterministic pricing on their very first voice quote.
 */
object TradePriceBookSeeder {

    fun seedForTrade(businessId: String, trade: TradeType): List<PriceBookItem> {
        val raw = when (trade) {
            TradeType.PLUMBING -> plumbingItems()
            TradeType.ELECTRICAL -> electricalItems()
            TradeType.HANDYMAN -> handymanItems()
            TradeType.PAINTING -> paintingItems()
            TradeType.LANDSCAPING -> landscapingItems()
            TradeType.GENERAL -> generalItems()
        }
        val now = System.currentTimeMillis()
        return raw.map { template ->
            PriceBookItem(
                id = UUID.randomUUID().toString(),
                businessId = businessId,
                name = template.nameEn,
                nameEs = template.nameEs,
                category = template.category,
                unit = template.unit,
                unitPriceCents = template.unitPriceCents,
                defaultQuantity = template.defaultQuantity,
                keywords = template.keywords,
                taxable = template.taxable,
                archived = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        }
    }

    private data class SeedTemplate(
        val nameEn: String,
        val nameEs: String,
        val category: LineItemCategory,
        val unit: PricingUnit,
        val unitPriceCents: Long,
        val defaultQuantity: Double = 1.0,
        val keywords: List<String>,
        val taxable: Boolean = true
    )

    private fun plumbingItems() = listOf(
        SeedTemplate(
            nameEn = "Plumbing Service Labor",
            nameEs = "Mano de obra de plomería",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.HOUR,
            unitPriceCents = 11500L,
            keywords = listOf("plumbing labor", "plumbing service", "mano de obra de plomeria", "plomero", "hourly labor"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "50-Gallon Gas Water Heater Install",
            nameEs = "Instalación de calentador de agua a gas 50 gal",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.JOB,
            unitPriceCents = 145000L,
            keywords = listOf("50-gallon gas water heater", "50 gallon water heater", "water heater install", "calentador de agua"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Braided Flex Supply Line",
            nameEs = "Manguera flexible trenzada",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 2400L,
            keywords = listOf("braided flex", "supply line", "flex line", "manguera flexible"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Thermal Expansion Tank",
            nameEs = "Tanque de expansión térmica",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 14500L,
            keywords = listOf("expansion tank", "thermal expansion", "tanque de expansion"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Kitchen Faucet Replacement",
            nameEs = "Llave mezcladora de cocina",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 16500L,
            keywords = listOf("kitchen faucet", "llave mezcladora", "faucet", "grifo de cocina"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "PVC P-Trap Drain Kit",
            nameEs = "Kit de trampa P de PVC",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 2800L,
            keywords = listOf("p-trap", "p trap", "trampa p", "drain kit"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Angle Stop Shutoff Valve",
            nameEs = "Válvula angular de cierre",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 3200L,
            keywords = listOf("angle stop", "shutoff valve", "valvula angular", "valvula de cierre"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Old Unit Haul Away & Disposal",
            nameEs = "Retiro y disposición de equipo viejo",
            category = LineItemCategory.OTHER,
            unit = PricingUnit.JOB,
            unitPriceCents = 8500L,
            keywords = listOf("haul away", "disposal", "retiro", "old unit"),
            taxable = false
        )
    )

    private fun electricalItems() = listOf(
        SeedTemplate(
            nameEn = "Master Electrician Labor",
            nameEs = "Mano de obra de electricista",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.HOUR,
            unitPriceCents = 12500L,
            keywords = listOf("electrical labor", "electrician", "mano de obra electrica"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Level 2 EV Charger Installation (240V)",
            nameEs = "Instalación de cargador EV Nivel 2 (240V)",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.JOB,
            unitPriceCents = 65000L,
            keywords = listOf("ev charger", "240v", "level 2 charger", "cargador ev"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "6-Gauge Copper Wire & Conduit Run",
            nameEs = "Cable de cobre calibre 6 y tubería conduit",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.LINEAR_FT,
            unitPriceCents = 1400L,
            keywords = listOf("6-gauge", "copper wire", "conduit run", "cable calibre 6"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "60-Amp 2-Pole Circuit Breaker",
            nameEs = "Interruptor termomagnético 60A 2 polos",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 7500L,
            keywords = listOf("60-amp breaker", "60 amp breaker", "breaker", "pastilla 60"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "GFCI Outlet Replacement",
            nameEs = "Reemplazo de contacto GFCI",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 6500L,
            keywords = listOf("gfci", "outlet", "contacto gfci"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Ceiling Fan Installation",
            nameEs = "Instalación de ventilador de techo",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.EACH,
            unitPriceCents = 19500L,
            keywords = listOf("ceiling fan", "ventilador de techo"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "City Electrical Permit",
            nameEs = "Permiso eléctrico municipal",
            category = LineItemCategory.PERMIT,
            unit = PricingUnit.EACH,
            unitPriceCents = 15000L,
            keywords = listOf("electrical permit", "city permit", "permiso electrico"),
            taxable = false
        )
    )

    private fun handymanItems() = listOf(
        SeedTemplate(
            nameEn = "Handyman Skilled Labor",
            nameEs = "Mano de obra de mantenimiento",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.HOUR,
            unitPriceCents = 9500L,
            keywords = listOf("handyman labor", "general labor", "mano de obra", "hourly"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Drywall Patch & Texture Match",
            nameEs = "Reparación y textura de tablaroca",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.EACH,
            unitPriceCents = 18500L,
            keywords = listOf("drywall", "sheetrock", "tablaroca", "wall patch"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Interior Door Hang & Hardware",
            nameEs = "Instalación de puerta interior y herrajes",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.EACH,
            unitPriceCents = 22000L,
            keywords = listOf("door install", "interior door", "puerta interior"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "TV Wall Mount Installation",
            nameEs = "Montaje de televisor en pared",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.EACH,
            unitPriceCents = 13500L,
            keywords = listOf("tv mount", "wall mount", "soporte tv"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Caulking & Weatherstripping Kit",
            nameEs = "Kit de sellador y burletes",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 3500L,
            keywords = listOf("caulk", "weatherstripping", "sellador"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Misc Hardware & Fasteners",
            nameEs = "Ferretería y tornillería varia",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.JOB,
            unitPriceCents = 4500L,
            keywords = listOf("hardware", "fasteners", "screws", "tornilleria"),
            taxable = true
        )
    )

    private fun paintingItems() = listOf(
        SeedTemplate(
            nameEn = "Exterior Prep, Scraping & Sanding",
            nameEs = "Preparación exterior, raspado y lijado",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.HOUR,
            unitPriceCents = 8500L,
            keywords = listOf("exterior prep", "scraping", "sanding", "preparacion exterior", "raspado"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Exterior Satin Paint (Premium)",
            nameEs = "Pintura exterior satinada (Premium)",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 6800L,
            keywords = listOf("exterior satin paint", "exterior paint", "gallon paint", "pintura exterior"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Front Entry Door Refinish",
            nameEs = "Acabado y pintura de puerta principal",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.EACH,
            unitPriceCents = 24000L,
            keywords = listOf("front entry door", "front door", "door refinish", "puerta principal"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Interior Wall Painting (2 Coats)",
            nameEs = "Pintura de muros interiores (2 manos)",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.SQ_FT,
            unitPriceCents = 275L,
            keywords = listOf("interior wall", "wall painting", "pintura interior"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Masking Tape, Plastic & Drop Cloths",
            nameEs = "Cinta de enmascarar, plástico y lonas",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.JOB,
            unitPriceCents = 4500L,
            keywords = listOf("masking tape", "drop cloths", "plastic", "cinta", "lonas"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Primer Sealer (Gallon)",
            nameEs = "Sellador primer (Galón)",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 4200L,
            keywords = listOf("primer", "sealer", "sellador"),
            taxable = true
        )
    )

    private fun landscapingItems() = listOf(
        SeedTemplate(
            nameEn = "Yard Cleanup & Shrub Pruning",
            nameEs = "Limpieza de patio y poda de arbustos",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.HOUR,
            unitPriceCents = 7500L,
            keywords = listOf("yard cleanup", "shrub pruning", "limpieza y poda", "poda de arbustos", "trimming"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Black Hardwood Mulch (Bag)",
            nameEs = "Mulch negro de madera (Bolsa)",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 950L,
            keywords = listOf("black mulch", "mulch", "mulch negro", "bolsas de mulch"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Lawn Mowing & Edging (Front & Back)",
            nameEs = "Corte de césped y orillas (Frente y atrás)",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.JOB,
            unitPriceCents = 9500L,
            keywords = listOf("lawn mowing", "edging", "recorte de cesped", "orillas", "mow"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Green Waste & Branch Dump Run",
            nameEs = "Retiro de ramas y basura verde al tiradero",
            category = LineItemCategory.OTHER,
            unit = PricingUnit.JOB,
            unitPriceCents = 11000L,
            keywords = listOf("green waste", "dump run", "retiro de ramas", "basura verde", "haul branches"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Sod Installation (Per Sq Ft)",
            nameEs = "Instalación de pasto en rollo (por pie²)",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.SQ_FT,
            unitPriceCents = 350L,
            keywords = listOf("sod", "pasto en rollo", "turf"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Sprinkler Head Replacement",
            nameEs = "Reemplazo de aspersor de riego",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.EACH,
            unitPriceCents = 4800L,
            keywords = listOf("sprinkler", "irrigation", "aspersor"),
            taxable = true
        )
    )

    private fun generalItems() = listOf(
        SeedTemplate(
            nameEn = "General Contracting Labor",
            nameEs = "Mano de obra de contratista",
            category = LineItemCategory.LABOR,
            unit = PricingUnit.HOUR,
            unitPriceCents = 10500L,
            keywords = listOf("labor", "mano de obra", "carpentry", "general"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Project Materials Allowance",
            nameEs = "Materiales de proyecto",
            category = LineItemCategory.MATERIAL,
            unit = PricingUnit.JOB,
            unitPriceCents = 25000L,
            keywords = listOf("materials", "materiales", "supplies"),
            taxable = true
        ),
        SeedTemplate(
            nameEn = "Site Cleanup & Debris Removal",
            nameEs = "Limpieza de obra y retiro de escombro",
            category = LineItemCategory.OTHER,
            unit = PricingUnit.JOB,
            unitPriceCents = 12500L,
            keywords = listOf("cleanup", "debris", "escombro", "haul away"),
            taxable = false
        ),
        SeedTemplate(
            nameEn = "Equipment Rental",
            nameEs = "Renta de equipo",
            category = LineItemCategory.EQUIPMENT,
            unit = PricingUnit.DAY,
            unitPriceCents = 15000L,
            keywords = listOf("rental", "equipment", "renta de equipo"),
            taxable = true
        )
    )
}
