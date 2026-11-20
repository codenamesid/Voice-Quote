# QuoteVoice — LLM Extraction Prompt, JSON Schema & Test Transcripts

## 1. System Prompt

```text
You are QuoteVoice's estimating parser for solo trade contractors (plumbers, electricians, handymen, painters, landscapers).
You receive a raw, potentially noisy or Spanglish job-site speech transcript and the contractor's trade.

RULES:
1. Return ONLY valid JSON conforming strictly to the provided JSON Schema. No markdown fences or commentary.
2. Extract customer details (name, street address, phone, email) if mentioned; otherwise return empty strings.
3. Split the job into discrete line items covering LABOR, MATERIAL, EQUIPMENT, PERMIT, or OTHER.
4. Preserve spoken quantities accurately (e.g., "three and a half hours" -> 3.5, "two GFCI outlets" -> 2.0, "400 square feet" -> 400.0). Default to 1.0 when unspecified.
5. Provide BOTH concise English (`name`) and Spanish (`nameEs`) titles for every line item so the app can render the customer's quote in either English or Spanish regardless of the spoken input language.
6. Provide a realistic U.S. solo-contractor `suggestedUnitPriceCents` (integer USD cents, e.g., $125.00 = 12500) for every item. The mobile app will override this with the contractor's saved Price Book whenever an item matches, and flag unmatched items as estimates.
7. Detect `spokenLanguage` ("en" or "es") and infer `preferredOutputLanguage` ("en" or "es") if the contractor mentions what language the customer speaks (e.g., "send it in Spanish" -> "es", "mándaselo en inglés" -> "en"); otherwise default to `spokenLanguage`.
8. Capture special site constraints, access codes, warranty notes, or exclusions in `notes`.
9. Set `confidence` (0.0 to 1.0) per line item to flag ambiguous quantities or descriptions for quick contractor review.
```

---

## 2. Strict JSON Schema (Draft 2020-12 / Gemini `responseSchema` & Claude Tool Schema)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "QuoteVoiceExtraction",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "spokenLanguage",
    "preferredOutputLanguage",
    "jobTitle",
    "customer",
    "lineItems",
    "notes"
  ],
  "properties": {
    "spokenLanguage": {
      "type": "string",
      "enum": ["en", "es"]
    },
    "preferredOutputLanguage": {
      "type": "string",
      "enum": ["en", "es"]
    },
    "jobTitle": {
      "type": "string",
      "description": "Short 3-6 word summary of the job"
    },
    "customer": {
      "type": "object",
      "additionalProperties": false,
      "required": ["name", "address", "phone", "email"],
      "properties": {
        "name": { "type": "string" },
        "address": { "type": "string" },
        "phone": { "type": "string" },
        "email": { "type": "string" }
      }
    },
    "lineItems": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": [
          "name",
          "nameEs",
          "description",
          "category",
          "quantity",
          "unit",
          "suggestedUnitPriceCents",
          "taxable",
          "confidence"
        ],
        "properties": {
          "name": { "type": "string" },
          "nameEs": { "type": "string" },
          "description": { "type": "string" },
          "category": {
            "type": "string",
            "enum": ["LABOR", "MATERIAL", "EQUIPMENT", "PERMIT", "OTHER"]
          },
          "quantity": { "type": "number", "minimum": 0.01 },
          "unit": {
            "type": "string",
            "enum": ["EACH", "HOUR", "SQ_FT", "LINEAR_FT", "DAY", "JOB"]
          },
          "suggestedUnitPriceCents": { "type": "integer", "minimum": 0 },
          "taxable": { "type": "boolean" },
          "confidence": { "type": "number", "minimum": 0.0, "maximum": 1.0 }
        }
      }
    },
    "notes": { "type": "string" }
  }
}
```

---

## 3. Five Test Transcripts (3 English, 2 Spanish)

### Transcript 1 (English — Plumbing)
> "Hey, quote for Marcus Henderson at 418 Oak Ridge Drive, phone 512-555-0194. We are replacing a leaking 50-gallon gas water heater in the garage. Put down one 50-gallon gas water heater install, two braided flex supply lines, one new thermal expansion tank, and haul away of the old unit. Customer wants the quote in English. Note: shutoff valve is tight behind the washer, allow half day."

### Transcript 2 (English — Electrical)
> "New estimate for Sarah Jenkins over at 2905 Maple Creek Lane. She wants a Level 2 EV charger installed in the attached garage. That's one 240V EV charger installation, 35 linear feet of 6-gauge copper wire conduit run, one 60-amp breaker, and a city electrical permit. Send in English. Note: panel has two open slots on the bottom right."

### Transcript 3 (English — Painting, Customer Needs Spanish Output)
> "Quote for Carlos Mendez at 812 Sunset Boulevard, phone 305-555-0148. Exterior trim and front door refresh. Give him 6 hours of exterior prep and scraping, 3 gallons of exterior satin paint, 1 front entry door refinish, and masking tape and plastic drop cloths. Please send the final quote in Spanish because his mom reviews the paperwork."

### Transcript 4 (Spanish — Plomería)
> "Cotización para Rosa Delgado en 1540 Avenida Cypress, teléfono 713-555-0182. Tiene fuga debajo del fregadero de la cocina. Incluye dos horas de mano de obra de plomería, una llave mezcladora de cocina nueva, un kit de trampa P de PVC, y dos válvulas angulares de cierre. Nota: el gabinete tiene humedad leve en la base, dejar secar antes de sellar."

### Transcript 5 (Spanish — Jardinería / Landscaping, Customer Needs English Output)
> "Presupuesto para David Miller en 7420 Pinecrest Way. Limpieza completa del patio trasero. Ponle 4 horas de limpieza y poda de arbustos, 6 bolsas de mulch negro, recorte de césped y orillas del frente y atrás, y retiro de ramas y basura verde al tiradero. Mándaselo en inglés por favor. Nota: perro grande en el patio, avisar antes de llegar."
