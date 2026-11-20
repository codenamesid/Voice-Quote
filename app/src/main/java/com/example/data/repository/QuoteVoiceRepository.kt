package com.example.data.repository

import com.example.data.ai.ExtractedQuoteDraft
import com.example.data.local.QuoteVoiceDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.local.entity.toInvoiceLineItemEntity
import com.example.data.local.entity.toQuoteLineItemEntity
import com.example.domain.model.Business
import com.example.domain.model.Customer
import com.example.domain.model.Invoice
import com.example.domain.model.InvoiceStatus
import com.example.domain.model.LineItem
import com.example.domain.model.PriceBookItem
import com.example.domain.model.Quote
import com.example.domain.model.QuoteStatus
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.SyncStatus
import com.example.domain.model.TradeType
import com.example.domain.pricing.PriceBookMatcher
import com.example.domain.pricing.TradePriceBookSeeder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Offline-first repository backed by Room SQLite.
 * All mutations persist locally immediately with [SyncStatus.PENDING_UPSERT].
 */
class QuoteVoiceRepository(private val db: QuoteVoiceDatabase) {

    private val businessDao = db.businessDao()
    private val customerDao = db.customerDao()
    private val priceBookDao = db.priceBookDao()
    private val quoteDao = db.quoteDao()
    private val invoiceDao = db.invoiceDao()

    fun observeBusiness(): Flow<Business?> =
        businessDao.observeBusiness().map { it?.toDomain() }

    fun observePriceBook(): Flow<List<PriceBookItem>> =
        priceBookDao.observeActiveItems().map { list -> list.map { it.toDomain() } }

    fun observeQuotes(): Flow<List<Quote>> =
        quoteDao.observeAllQuotesWithLineItems().map { list -> list.map { it.toDomain() } }

    fun observeQuote(quoteId: String): Flow<Quote?> =
        quoteDao.observeQuoteWithLineItems(quoteId).map { it?.toDomain() }

    fun observeInvoices(): Flow<List<Invoice>> =
        invoiceDao.observeAllInvoicesWithLineItems().map { list -> list.map { it.toDomain() } }

    fun observeInvoice(invoiceId: String): Flow<Invoice?> =
        invoiceDao.observeInvoiceWithLineItems(invoiceId).map { it?.toDomain() }

    suspend fun getBusinessOnce(): Business? = businessDao.getBusinessOnce()?.toDomain()

    suspend fun getActivePriceBookOnce(): List<PriceBookItem> =
        priceBookDao.getActiveItemsOnce().map { it.toDomain() }

    suspend fun completeOnboarding(
        businessName: String,
        trade: TradeType,
        phone: String,
        licenseNumber: String,
        taxRatePercent: Double,
        depositPercent: Int,
        defaultLanguage: SupportedLanguage,
        logoLocalUri: String?
    ): Business {
        val now = System.currentTimeMillis()
        val existing = businessDao.getBusinessOnce()?.toDomain()
        val businessId = existing?.id ?: UUID.randomUUID().toString()
        val taxBps = (taxRatePercent.coerceIn(0.0, 25.0) * 100.0).toInt()
        val currentMonth = currentYearMonth()

        val business = (existing ?: Business(
            id = businessId,
            ownerUid = "local-contractor",
            name = businessName.trim(),
            quotaPeriodYearMonth = currentMonth
        )).copy(
            name = businessName.trim(),
            trade = trade,
            phone = phone.trim(),
            licenseNumber = licenseNumber.trim(),
            logoLocalUri = logoLocalUri ?: existing?.logoLocalUri,
            defaultTaxRateBps = taxBps,
            defaultDepositPercent = depositPercent.coerceIn(0, 100),
            defaultLanguage = defaultLanguage,
            onboardingCompleted = true,
            quotaPeriodYearMonth = currentMonth,
            updatedAtEpochMs = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        businessDao.upsertBusiness(business.toEntity())

        // Seed starter price book if empty or trade changed
        val currentItems = priceBookDao.getActiveItemsOnce()
        if (currentItems.isEmpty() || (existing != null && existing.trade != trade)) {
            priceBookDao.deleteAllForBusiness(businessId)
            val seeded = TradePriceBookSeeder.seedForTrade(businessId, trade)
            priceBookDao.upsertAll(seeded.map { it.toEntity() })
        }

        return business
    }

    suspend fun savePriceBookItem(item: PriceBookItem) {
        priceBookDao.upsertItem(
            item.copy(
                updatedAtEpochMs = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPSERT
            ).toEntity()
        )
    }

    suspend fun archivePriceBookItem(itemId: String) {
        priceBookDao.archiveItem(itemId, System.currentTimeMillis())
    }

    suspend fun upgradeSubscriptionTier(tier: SubscriptionTier) {
        val business = businessDao.getBusinessOnce()?.toDomain() ?: return
        businessDao.upsertBusiness(
            business.copy(
                subscriptionTier = tier,
                updatedAtEpochMs = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPSERT
            ).toEntity()
        )
    }

    /**
     * Creates and persists a priced draft [Quote] from an [ExtractedQuoteDraft],
     * matching items against the contractor's saved Price Book and enforcing Free-tier quota.
     */
    suspend fun createDraftQuoteFromExtraction(
        rawTranscript: String,
        draft: ExtractedQuoteDraft
    ): Result<Quote> {
        val now = System.currentTimeMillis()
        val month = currentYearMonth()
        val existingBusiness = businessDao.getBusinessOnce()?.toDomain()
            ?: return Result.failure(IllegalStateException("Business onboarding not completed."))

        val rolledBusiness = if (existingBusiness.quotaPeriodYearMonth != month) {
            existingBusiness.copy(quotesCreatedThisMonth = 0, quotaPeriodYearMonth = month)
        } else {
            existingBusiness
        }

        if (!rolledBusiness.canCreateQuote()) {
            return Result.failure(
                IllegalStateException("Free tier limit reached (5 quotes/month). Upgrade to Pro for unlimited quotes.")
            )
        }

        val priceBook = priceBookDao.getActiveItemsOnce().map { it.toDomain() }
        val quoteId = UUID.randomUUID().toString()
        val quoteNumber = "${rolledBusiness.quoteNumberPrefix}-${rolledBusiness.nextQuoteNumber}"

        // Upsert customer if name is present
        val customerId: String? = if (draft.customer.name.isNotBlank()) {
            val existingCust = customerDao.findByName(draft.customer.name)?.toDomain()
            val cust = (existingCust ?: Customer(
                id = UUID.randomUUID().toString(),
                businessId = rolledBusiness.id,
                name = draft.customer.name,
                createdAtEpochMs = now
            )).copy(
                phone = draft.customer.phone.ifBlank { existingCust?.phone.orEmpty() },
                email = draft.customer.email.ifBlank { existingCust?.email.orEmpty() },
                address = draft.customer.address.ifBlank { existingCust?.address.orEmpty() },
                preferredLanguage = draft.preferredOutputLanguage,
                updatedAtEpochMs = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
            customerDao.upsertCustomer(cust.toEntity())
            cust.id
        } else {
            null
        }

        val pricedItems = PriceBookMatcher.priceLineItems(
            quoteId = quoteId,
            candidates = draft.lineItems,
            priceBook = priceBook,
            outputLanguage = draft.preferredOutputLanguage
        )

        val shareToken = UUID.randomUUID().toString().replace("-", "").take(16)
        val validUntil = now + (14L * 24L * 60L * 60L * 1000L)

        val quote = Quote(
            id = quoteId,
            businessId = rolledBusiness.id,
            customerId = customerId,
            customerNameSnapshot = draft.customer.name,
            customerAddressSnapshot = draft.customer.address,
            customerPhoneSnapshot = draft.customer.phone,
            customerEmailSnapshot = draft.customer.email,
            quoteNumber = quoteNumber,
            title = draft.jobTitle,
            rawTranscript = rawTranscript.trim(),
            spokenLanguage = draft.spokenLanguage,
            outputLanguage = draft.preferredOutputLanguage,
            notes = draft.notes,
            termsSnapshot = rolledBusiness.defaultTermsFor(draft.preferredOutputLanguage),
            lineItems = pricedItems,
            taxRateBps = rolledBusiness.defaultTaxRateBps,
            depositPercent = rolledBusiness.defaultDepositPercent,
            status = QuoteStatus.DRAFT,
            shareToken = shareToken,
            shareUrl = "https://quotevoice.app/q/$shareToken",
            validUntilEpochMs = validUntil,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )

        quoteDao.upsertQuoteWithLineItems(
            quote = quote.toEntity(),
            lineItems = quote.lineItems.mapIndexed { idx, item ->
                item.copy(sortOrder = idx).toQuoteLineItemEntity(quoteId)
            }
        )

        businessDao.upsertBusiness(
            rolledBusiness.copy(
                nextQuoteNumber = rolledBusiness.nextQuoteNumber + 1,
                quotesCreatedThisMonth = rolledBusiness.quotesCreatedThisMonth + 1,
                updatedAtEpochMs = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            ).toEntity()
        )

        return Result.success(quote)
    }

    suspend fun saveQuote(quote: Quote) {
        val now = System.currentTimeMillis()
        val updated = quote.copy(
            updatedAtEpochMs = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        quoteDao.upsertQuoteWithLineItems(
            quote = updated.toEntity(),
            lineItems = updated.lineItems.mapIndexed { idx, item ->
                item.copy(sortOrder = idx).toQuoteLineItemEntity(updated.id)
            }
        )
    }

    suspend fun switchQuoteOutputLanguage(quoteId: String, targetLanguage: SupportedLanguage): Quote? {
        val quote = quoteDao.getQuoteWithLineItemsOnce(quoteId)?.toDomain() ?: return null
        val business = businessDao.getBusinessOnce()?.toDomain()
        val priceBookById = priceBookDao.getActiveItemsOnce()
            .map { it.toDomain() }
            .associateBy { it.id }

        val translatedItems = quote.lineItems.map { item ->
            val pb = item.priceBookItemId?.let { priceBookById[it] }
            if (pb != null) {
                item.copy(name = pb.localizedName(targetLanguage))
            } else {
                item
            }
        }

        val updated = quote.copy(
            outputLanguage = targetLanguage,
            termsSnapshot = business?.defaultTermsFor(targetLanguage) ?: quote.termsSnapshot,
            lineItems = translatedItems,
            updatedAtEpochMs = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        saveQuote(updated)
        return updated
    }

    suspend fun markQuoteSent(quoteId: String, pdfLocalPath: String? = null) {
        val quote = quoteDao.getQuoteWithLineItemsOnce(quoteId)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        val nextStatus = if (quote.status == QuoteStatus.DRAFT) QuoteStatus.SENT else quote.status
        saveQuote(
            quote.copy(
                status = nextStatus,
                sentAtEpochMs = quote.sentAtEpochMs ?: now,
                pdfLocalPath = pdfLocalPath ?: quote.pdfLocalPath
            )
        )
    }

    suspend fun markQuoteAcceptedByCustomer(quoteId: String, signerName: String, depositPaid: Boolean) {
        val quote = quoteDao.getQuoteWithLineItemsOnce(quoteId)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        saveQuote(
            quote.copy(
                status = QuoteStatus.ACCEPTED,
                acceptedAtEpochMs = now,
                signerName = signerName.ifBlank { quote.customerNameSnapshot.ifBlank { "Customer" } },
                depositPaidCents = if (depositPaid) quote.depositCents else quote.depositPaidCents
            )
        )
    }

    suspend fun deleteQuote(quoteId: String) {
        quoteDao.deleteQuote(quoteId)
    }

    /**
     * One-tap conversion of an accepted (or sent) Quote into an Invoice,
     * preserving deposit credits and line items as an immutable snapshot.
     */
    suspend fun convertQuoteToInvoice(quoteId: String): Result<Invoice> {
        val existing = invoiceDao.findByQuoteId(quoteId)?.toDomain()
        if (existing != null) {
            return Result.success(existing)
        }

        val quote = quoteDao.getQuoteWithLineItemsOnce(quoteId)?.toDomain()
            ?: return Result.failure(IllegalArgumentException("Quote not found"))
        val business = businessDao.getBusinessOnce()?.toDomain()
            ?: return Result.failure(IllegalStateException("Business profile missing"))

        val now = System.currentTimeMillis()
        val invoiceId = UUID.randomUUID().toString()
        val invoiceNumber = "INV-${business.nextInvoiceNumber}"
        val dueDate = now + (14L * 24L * 60L * 60L * 1000L)
        val firstReminder = now + (3L * 24L * 60L * 60L * 1000L)
        val shareToken = UUID.randomUUID().toString().replace("-", "").take(16)

        val invoiceItems = quote.lineItems.mapIndexed { idx, item ->
            LineItem(
                id = UUID.randomUUID().toString(),
                quoteId = invoiceId,
                sortOrder = idx,
                name = item.name,
                description = item.description,
                category = item.category,
                quantity = item.quantity,
                unit = item.unit,
                unitPriceCents = item.unitPriceCents,
                taxable = item.taxable,
                priceSource = item.priceSource,
                priceBookItemId = item.priceBookItemId,
                aiConfidence = item.aiConfidence
            )
        }

        val initialStatus = when {
            quote.depositPaidCents >= quote.totalCents && quote.totalCents > 0L -> InvoiceStatus.PAID
            quote.depositPaidCents > 0L -> InvoiceStatus.PARTIALLY_PAID
            else -> InvoiceStatus.SENT
        }

        val invoice = Invoice(
            id = invoiceId,
            businessId = business.id,
            quoteId = quote.id,
            customerId = quote.customerId,
            customerNameSnapshot = quote.customerNameSnapshot,
            customerAddressSnapshot = quote.customerAddressSnapshot,
            customerPhoneSnapshot = quote.customerPhoneSnapshot,
            customerEmailSnapshot = quote.customerEmailSnapshot,
            invoiceNumber = invoiceNumber,
            title = quote.title,
            outputLanguage = quote.outputLanguage,
            lineItems = invoiceItems,
            taxRateBps = quote.taxRateBps,
            amountPaidCents = quote.depositPaidCents,
            status = initialStatus,
            dueDateEpochMs = dueDate,
            sentAtEpochMs = now,
            paidAtEpochMs = if (initialStatus == InvoiceStatus.PAID) now else null,
            shareToken = shareToken,
            shareUrl = "https://quotevoice.app/i/$shareToken",
            stripePaymentLinkUrl = "https://quotevoice.app/pay/$shareToken",
            autoRemindersEnabled = true,
            reminderCount = 0,
            nextReminderAtEpochMs = if (initialStatus != InvoiceStatus.PAID) firstReminder else null,
            notes = quote.notes,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )

        invoiceDao.upsertInvoiceWithLineItems(
            invoice = invoice.toEntity(),
            lineItems = invoiceItems.map { it.toInvoiceLineItemEntity(invoiceId) }
        )

        saveQuote(quote.copy(status = QuoteStatus.INVOICED))
        businessDao.upsertBusiness(
            business.copy(
                nextInvoiceNumber = business.nextInvoiceNumber + 1,
                updatedAtEpochMs = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            ).toEntity()
        )

        return Result.success(invoice)
    }

    suspend fun toggleInvoiceAutoReminders(invoiceId: String, enabled: Boolean) {
        val invoice = invoiceDao.getInvoiceWithLineItemsOnce(invoiceId)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        val nextReminder = if (enabled && invoice.status != InvoiceStatus.PAID) {
            now + (3L * 24L * 60L * 60L * 1000L)
        } else null

        val updated = invoice.copy(
            autoRemindersEnabled = enabled,
            nextReminderAtEpochMs = nextReminder,
            updatedAtEpochMs = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        invoiceDao.upsertInvoice(updated.toEntity())
    }

    suspend fun recordReminderSent(invoiceId: String) {
        val invoice = invoiceDao.getInvoiceWithLineItemsOnce(invoiceId)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        val updated = invoice.copy(
            reminderCount = invoice.reminderCount + 1,
            lastReminderSentAtEpochMs = now,
            nextReminderAtEpochMs = if (invoice.autoRemindersEnabled && invoice.status != InvoiceStatus.PAID) {
                now + (3L * 24L * 60L * 60L * 1000L)
            } else null,
            updatedAtEpochMs = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        invoiceDao.upsertInvoice(updated.toEntity())
    }

    suspend fun markInvoicePaidInFull(invoiceId: String) {
        val invoice = invoiceDao.getInvoiceWithLineItemsOnce(invoiceId)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        val updated = invoice.copy(
            amountPaidCents = invoice.totalCents,
            status = InvoiceStatus.PAID,
            paidAtEpochMs = now,
            nextReminderAtEpochMs = null,
            updatedAtEpochMs = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        invoiceDao.upsertInvoice(updated.toEntity())
    }

    private fun currentYearMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }
}
