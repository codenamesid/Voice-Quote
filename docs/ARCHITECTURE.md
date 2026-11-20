# QuoteVoice — MVP Architecture & Module Structure

## 1. Product & Engineering Goals
QuoteVoice enables solo tradespeople and small crews (1–5 people) to speak a job description on-site in English or Spanish and send a branded, priced quote before leaving the driveway.

### Key Non-Functional Targets
| Requirement | Target | Architectural Mechanism |
|---|---|---|
| **Offline-first** | Recording, editing, price-book lookup, and PDF generation work with zero signal | Room SQLite as local Single Source of Truth (SSOT); on-device `SpeechRecognizer` + local audio fallback queue; Android `PdfDocument` on-device rendering; WorkManager sync queue |
| **Cold start** | `< 1.5s` | Lightweight `Application` init; `by lazy` initialization of Firebase AI Logic, Billing, and Sync engines; Room opened on `Dispatchers.IO` |
| **Speech → Draft Quote** | `< 10s` | Streaming partial captions from `SpeechRecognizer`; structured JSON extraction via `gemini-3.8-flash` (or thin Cloud Function Claude adapter) with deterministic local price-book matcher |
| **Glove-friendly A11y** | Min `56.dp` targets, high contrast, outdoor legibility | Custom M3 High-Contrast Safety Amber + Deep Slate theme, `56.dp+` primary actions, scalable `sp` typography, full TalkBack `contentDescription` coverage |
| **Bilingual EN / ES** | Full UI localization + cross-language quote output | Android `values/` + `values-es/` resources; independent `spokenLanguage` (voice input) and `outputLanguage` (customer PDF/web quote) per quote |

---

## 2. Backend Choice: Firebase (over Supabase)

**Decision: Firebase (Auth + Firestore + Firebase AI Logic + Cloud Functions + Cloud Storage)**

### Justification for an Android-First Field-Contractor MVP
1. **First-class Android SDK & Offline Resilience**: Firestore integrates cleanly alongside our Room offline SSOT and WorkManager sync, with automatic retry semantics and Google Sign-In via Jetpack `CredentialManager`.
2. **Zero Shipped API Keys (App Check + AI Logic)**: Firebase AI Logic with App Check attestation lets the Android client call structured JSON extraction (`gemini-3.8-flash`) securely with zero embedded API keys, while Cloud Functions provide the thin backend layer for Claude API fallback (if enabled), Whisper audio fallback, Stripe Connect webhooks, and customer share pages.
3. **Single Unified Security Boundary**: One Firebase Auth UID (`request.auth.uid`) secures Firestore user-scoped collections (`/users/{uid}/...`), Storage logos/PDFs, and callable Cloud Functions without bridging separate JWT issuers.
4. **Public Customer Share Page & E-Sign**: Firebase Hosting + Cloud Functions (HTTPS) cleanly serve unauthenticated customer quote pages (`/q/{shareToken}`) via opaque 256-bit tokens without exposing private contractor Firestore collections.
5. **Why not Supabase for this MVP**: While Supabase excels for complex relational SQL reporting, QuoteVoice's MVP data access is strictly contractor-scoped document trees (`Business -> Customers, PriceBook, Quotes, Invoices`) with offline mobile writes, Android-native Google Auth, and scheduled reminder functions—where Firebase reduces operational glue and cold-start latency.

---

## 3. System Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          ANDROID CLIENT (Jetpack Compose)                    │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Onboarding  │  │    Record    │  │ Review/Edit  │  │ Quote Preview &  │  │
│  │ (Trade Seed) │  │ (Big Mic UI) │  │ (Swipe/Tap)  │  │ Invoices / List  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
│         └─────────────────┴─────────────────┴───────────────────┘            │
│                                     │                                        │
│                             MVVM ViewModels                                  │
│                      (StateFlow<UiState>, UseCases)                          │
│                                     │                                        │
│  ┌──────────────────────────────────┴─────────────────────────────────────┐  │
│  │                          DOMAIN LAYER (Pure Kotlin)                    │  │
│  │  Models (Business, Customer, Quote, LineItem, PriceBookItem, Invoice)  │  │
│  │  PriceBookMatcher  •  QuoteCalculator (Int Cents)  •  QuotaEnforcer    │  │
│  └────────────┬──────────────────────┬───────────────────────┬────────────┘  │
│               │                      │                       │               │
│  ┌────────────▼──────────┐ ┌─────────▼──────────┐ ┌──────────▼────────────┐  │
│  │  LOCAL DATA (SSOT)    │ │ SPEECH + AI ENGINE │ │   ON-DEVICE PDF &     │  │
│  │  Room SQLite Database │ │ 1. SpeechRecognizer│ │   SHARING ENGINE      │  │
│  │  (Offline-first CRUD) │ │ 2. Audio -> Whisper│ │   PdfDocument (EN/ES) │  │
│  │  SyncStatus Metadata  │ │ 3. QuoteExtractor  │ │   FileProvider +      │  │
│  └────────────┬──────────┘ │    (AI Logic /     │ │   ACTION_SENDTO       │  │
│               │            │     Claude Proxy)  │ │   (Zero-Perm SMS/Mail)│  │
│  ┌────────────▼──────────┐ └─────────┬──────────┘ └───────────────────────┘  │
│  │  WorkManager Sync     │           │                                       │
│  │  (Pending Upsert/Del) │           │                                       │
│  └────────────┬──────────┘           │                                       │
└───────────────┼──────────────────────┼───────────────────────────────────────┘
                │ HTTPS / App Check    │
┌───────────────▼──────────────────────▼───────────────────────────────────────┐
│                         FIREBASE CLOUD BACKEND                               │
│                                                                              │
│  • Firebase Auth (Google Sign-In via Jetpack Credential Manager)             │
│  • Cloud Firestore (Named DB — /users/{uid}/business, customers, quotes...)  │
│  • Firebase AI Logic (Gemini 3.8 Flash structured JSON via App Check)        │
│  • Cloud Functions (HTTPS / Scheduled):                                      │
│      1. GET  /q/{shareToken}           -> Branded web quote + e-sign UI      │
│      2. POST /q/{shareToken}/accept    -> Persist signature + deposit link   │
│      3. POST /api/stripe/webhook       -> Mark deposit/invoice paid          │
│      4. POST /api/extract-quote        -> Optional Claude / Whisper proxy    │
│      5. CRON dailyPaymentReminders     -> Automated overdue invoice alerts   │
│  • Stripe Connect Express (Direct contractor payouts; zero funds held)       │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Module & Package Structure

For rapid MVP iteration in this repository while enforcing strict Clean Architecture dependency boundaries, the project uses a **clean single-module `:app` layout partitioned by exact future Gradle module boundaries**. Each top-level package under `com.example` maps 1:1 to a future Gradle submodule (`:core:model`, `:core:database`, `:core:sync`, `:core:ai`, `:core:pdf`, `:feature:*`) and obeys acyclic dependency rules (`ui -> domain <- data`).

```
app/src/main/java/com/example/
├── domain/                       # Future :core:model & :core:domain (Pure Kotlin, zero Android/Firebase deps)
│   ├── model/
│   │   ├── QuoteVoiceEnums.kt    # TradeType, SupportedLanguage, QuoteStatus, InvoiceStatus, PriceSource, SyncStatus
│   │   ├── Business.kt           # Contractor profile, branding, tax/deposit defaults, Stripe & Pro quota state
│   │   ├── Customer.kt           # Client contact, address, preferred quote language
│   │   ├── PriceBookItem.kt      # Trade-seeded & custom priced services/materials with EN/ES aliases
│   │   ├── LineItem.kt           # Quote/Invoice line item with cents math & PriceSource tracking
│   │   ├── Quote.kt              # Draft/Sent/Accepted quote aggregate, transcript, share & e-sign fields
│   │   └── Invoice.kt            # One-tap converted invoice, deposit credit, Stripe link, reminder state
│   ├── pricing/
│   │   ├── PriceBookMatcher.kt   # Fuzzy EN/ES matching against saved price book -> PRICE_BOOK vs AI_ESTIMATE
│   │   └── TradePriceBookSeeder.kt # Pre-seeded starter catalogs for Plumbing, Electrical, Handyman, Painting, Landscaping
│   └── repository/               # Repository interfaces consumed by ViewModels & UseCases
├── data/                         # Future :core:database & :core:network
│   ├── local/
│   │   ├── entity/               # Room @Entity tables (BusinessEntity, CustomerEntity, PriceBookItemEntity,
│   │   │                         # QuoteEntity, LineItemEntity, InvoiceEntity) + TypeConverters + Mappers
│   │   ├── dao/                  # Flow-based DAOs with transactional QuoteWithLineItems queries
│   │   └── QuoteVoiceDatabase.kt # RoomDatabase singleton
│   ├── remote/
│   │   ├── firestore/            # Firestore named-DB data source, zero-trust user-scoped sync DTOs, error handler
│   │   ├── ai/                   # QuoteExtractionService interface + FirebaseAiQuoteExtractor (Gemini 3.8 Flash
│   │   │                         # strict JSON schema) + optional Cloud Function Claude/Whisper HTTP adapter
│   │   └── billing/              # Play Billing wrapper + monthly Free-tier (5 quotes/mo) quota guard
│   ├── sync/
│   │   └── QuoteSyncWorker.kt    # WorkManager offline-to-cloud sync worker (exponential backoff, network constraint)
│   └── repository/               # Offline-first Repository implementations (write Room first, enqueue sync)
├── speech/                       # Future :core:speech
│   └── VoiceCaptureManager.kt    # Android SpeechRecognizer (EN/ES, up to 3 min) + local M4A recorder for Whisper fallback
├── pdf/                          # Future :core:pdf
│   └── QuotePdfGenerator.kt      # On-device PdfDocument generator (branded header, license #, bilingual EN/ES labels)
├── ui/                           # Future :feature:* screens & shared design system
│   ├── theme/                    # High-contrast Safety Amber + Deep Slate Material 3 theme & typography
│   ├── components/               # Glove-friendly 56dp+ buttons, estimate badges, language pills, swipe rows
│   ├── auth/                     # Google Sign-In via Credential Manager (GetSignInWithGoogleOption)
│   ├── onboarding/               # Business profile setup + trade picker + starter price book review
│   ├── record/                   # Big Mic button, live waveform/timer (3:00 cap), EN/ES toggle, transcript preview
│   ├── review/                   # Swipe-to-delete, tap-to-edit line items, price book vs AI estimate indicators
│   ├── preview/                  # Branded PDF preview, EN/ES output toggle, Share (SMS/Email/Link) actions
│   ├── quotes/                   # Quotes list with status filter chips & search
│   └── invoice/                  # One-tap Quote->Invoice screen, payment status, reminder schedule toggle
└── backend/                      # Deliverable 5: Firebase Cloud Functions (TypeScript)
    └── functions/src/            # quoteSharePage, acceptAndSignQuote, stripeWebhook, paymentReminders, claudeProxy
```

---

## 5. Offline-First & Sync Strategy
1. **Local Writes Always Succeed Immediately**: Every mutation (creating a quote, editing a line item, converting to an invoice) writes to Room inside a transaction with `syncStatus = SyncStatus.PENDING_UPSERT` and an updated `updatedAtEpochMs`.
2. **Offline AI Fallback**: If the contractor records a job with zero cell signal:
   - On-device `SpeechRecognizer` (or local audio file) captures the job immediately and saves a `DRAFT` quote with the raw transcript.
   - A lightweight local keyword parser matches obvious price-book items immediately so the contractor can keep editing, OR marks `extractionPending = true` so WorkManager runs cloud LLM extraction automatically when signal returns.
3. **Background Sync (`WorkManager`)**:
   - Triggered with `NetworkType.CONNECTED` and exponential backoff whenever any table has `syncStatus != SYNCED`.
   - Syncs strictly into `/users/{authUid}/...` in the named Firestore database (`R.string.firestore_database_id`), using server timestamps (`FieldValue.serverTimestamp()`) and last-write-wins conflict resolution via `updatedAtEpochMs`.

---

## 6. Data Integrity & Precision Decisions
- **Monetary Values as Integer Cents (`Long`)**: All prices, subtotals, tax amounts, deposits, and balances are stored as `Long` cents (e.g., `$125.50` = `12550L`) to eliminate floating-point drift across Room, JSON, Firestore, and Stripe (which also uses cents).
- **Tax Rate as Basis Points (`Int`)**: Stored as integer basis points (e.g., `8.25%` = `825`), avoiding floating-point percentage errors.
- **Price Provenance (`PriceSource`)**: Every `LineItem` explicitly tracks whether its price came from `PRICE_BOOK`, `AI_ESTIMATE` (visually flagged in the Review/Edit UI so contractors never send an unverified guess blindly), or `MANUAL` override.
- **Snapshots on Quotes & Invoices**: Customer name, address, phone, email, and business terms are snapshotted onto each `Quote` and `Invoice` so historical documents never silently mutate if a customer changes address later.

---

## 7. Security, Privacy & Google Play Compliance
- **Authentication**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`); zero-trust Firestore rules isolate all contractor data under `/users/{uid}/...`.
- **No Shipped Secrets**: No LLM or Stripe secret keys exist in the APK. AI extraction uses Firebase AI Logic with App Check attestation; Stripe Connect webhooks and secret operations live exclusively in Cloud Functions.
- **Google Play Creator Program & Play Policy Compliance**:
  - **SMS Sharing**: Uses zero-permission `Intent.ACTION_SENDTO` (`smsto:`) so the contractor reviews and sends the message via their default SMS app — **never** declares the prohibited `SEND_SMS` permission.
  - **Logo Selection**: Uses zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) — **never** requests `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`.
  - **Audio Recording**: Requests `RECORD_AUDIO` contextually at runtime only when the user taps the mic button.
  - **Monetization**: Pro subscriptions use Google Play Billing; customer quote deposits and invoice payments occur on the hosted web share page via Stripe Connect Express (no funds held by QuoteVoice, no advertising SDKs).
