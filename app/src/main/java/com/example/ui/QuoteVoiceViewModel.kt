package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.PlaceholderQuoteExtractionRepository
import com.example.data.ai.QuoteExtractionRepository
import com.example.data.ai.SampleTranscriptFixture
import com.example.data.local.QuoteVoiceDatabase
import com.example.data.repository.QuoteVoiceRepository
import com.example.domain.model.Business
import com.example.domain.model.Invoice
import com.example.domain.model.LineItem
import com.example.domain.model.LineItemCategory
import com.example.domain.model.PriceBookItem
import com.example.domain.model.PriceSource
import com.example.domain.model.PricingUnit
import com.example.domain.model.Quote
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TradeType
import com.example.pdf.QuotePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed interface ExtractionUiState {
    data object Idle : ExtractionUiState
    data object Extracting : ExtractionUiState
    data class Error(val message: String) : ExtractionUiState
}

data class QuoteVoiceAppState(
    val isLoading: Boolean = true,
    val business: Business? = null,
    val priceBook: List<PriceBookItem> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val isAiPlaceholderMode: Boolean = true
)

class QuoteVoiceViewModel(
    private val repository: QuoteVoiceRepository,
    private val extractionRepository: QuoteExtractionRepository
) : ViewModel() {

    val appState: StateFlow<QuoteVoiceAppState> = combine(
        repository.observeBusiness(),
        repository.observePriceBook(),
        repository.observeQuotes(),
        repository.observeInvoices()
    ) { business, priceBook, quotes, invoices ->
        QuoteVoiceAppState(
            isLoading = false,
            business = business,
            priceBook = priceBook,
            quotes = quotes,
            invoices = invoices,
            isAiPlaceholderMode = extractionRepository.isPlaceholderMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = QuoteVoiceAppState(isLoading = true)
    )

    private val _extractionState = MutableStateFlow<ExtractionUiState>(ExtractionUiState.Idle)
    val extractionState: StateFlow<ExtractionUiState> = _extractionState.asStateFlow()

    private val _draftTranscript = MutableStateFlow("")
    val draftTranscript: StateFlow<String> = _draftTranscript.asStateFlow()

    private val _spokenLanguage = MutableStateFlow(SupportedLanguage.EN)
    val spokenLanguage: StateFlow<SupportedLanguage> = _spokenLanguage.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    fun updateDraftTranscript(text: String) {
        _draftTranscript.value = text
    }

    fun selectSpokenLanguage(language: SupportedLanguage) {
        _spokenLanguage.value = language
    }

    fun loadSampleFixture(fixture: SampleTranscriptFixture) {
        _spokenLanguage.value = fixture.spokenLanguage
        _draftTranscript.value = fixture.transcript
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun clearExtractionError() {
        _extractionState.value = ExtractionUiState.Idle
    }

    fun completeOnboarding(
        businessName: String,
        trade: TradeType,
        phone: String,
        licenseNumber: String,
        taxRatePercent: Double,
        depositPercent: Int,
        defaultLanguage: SupportedLanguage,
        logoLocalUri: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.completeOnboarding(
                businessName = businessName,
                trade = trade,
                phone = phone,
                licenseNumber = licenseNumber,
                taxRatePercent = taxRatePercent,
                depositPercent = depositPercent,
                defaultLanguage = defaultLanguage,
                logoLocalUri = logoLocalUri
            )
            _spokenLanguage.value = defaultLanguage
            onDone()
        }
    }

    fun extractAndCreateDraftQuote(
        transcriptOverride: String? = null,
        onQuoteCreated: (String) -> Unit
    ) {
        val text = (transcriptOverride ?: _draftTranscript.value).trim()
        if (text.isBlank()) {
            _extractionState.value = ExtractionUiState.Error("Speak or enter a job description first.")
            return
        }

        viewModelScope.launch {
            _extractionState.value = ExtractionUiState.Extracting
            val business = repository.getBusinessOnce()
            val priceBook = repository.getActivePriceBookOnce()
            val trade = business?.trade ?: TradeType.HANDYMAN

            val extractionResult = extractionRepository.extractQuote(
                transcript = text,
                trade = trade,
                spokenLanguageHint = _spokenLanguage.value,
                priceBook = priceBook
            )

            extractionResult.fold(
                onSuccess = { draft ->
                    val created = repository.createDraftQuoteFromExtraction(text, draft)
                    created.fold(
                        onSuccess = { quote ->
                            _extractionState.value = ExtractionUiState.Idle
                            _draftTranscript.value = ""
                            onQuoteCreated(quote.id)
                        },
                        onFailure = { err ->
                            _extractionState.value = ExtractionUiState.Error(
                                err.message ?: "Could not save draft quote."
                            )
                        }
                    )
                },
                onFailure = { err ->
                    _extractionState.value = ExtractionUiState.Error(
                        err.message ?: "Could not parse transcript."
                    )
                }
            )
        }
    }

    fun updateQuote(quote: Quote) {
        viewModelScope.launch {
            repository.saveQuote(quote)
        }
    }

    fun removeLineItem(quote: Quote, lineItemId: String) {
        val updated = quote.copy(lineItems = quote.lineItems.filterNot { it.id == lineItemId })
        updateQuote(updated)
    }

    fun upsertLineItem(quote: Quote, item: LineItem) {
        val existingIdx = quote.lineItems.indexOfFirst { it.id == item.id }
        val nextList = if (existingIdx >= 0) {
            quote.lineItems.toMutableList().apply { set(existingIdx, item) }
        } else {
            quote.lineItems + item.copy(sortOrder = quote.lineItems.size)
        }
        updateQuote(quote.copy(lineItems = nextList))
    }

    fun addItemFromPriceBook(quote: Quote, pbItem: PriceBookItem) {
        val newItem = LineItem(
            id = UUID.randomUUID().toString(),
            quoteId = quote.id,
            sortOrder = quote.lineItems.size,
            name = pbItem.localizedName(quote.outputLanguage),
            description = "",
            category = pbItem.category,
            quantity = pbItem.defaultQuantity,
            unit = pbItem.unit,
            unitPriceCents = pbItem.unitPriceCents,
            taxable = pbItem.taxable,
            priceSource = PriceSource.PRICE_BOOK,
            priceBookItemId = pbItem.id
        )
        upsertLineItem(quote, newItem)
    }

    fun switchQuoteLanguage(quoteId: String, targetLanguage: SupportedLanguage) {
        viewModelScope.launch {
            repository.switchQuoteOutputLanguage(quoteId, targetLanguage)
        }
    }

    fun generateAndSaveQuotePdf(
        context: Context,
        business: Business,
        quote: Quote,
        onGenerated: (File) -> Unit
    ) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                QuotePdfGenerator.generateQuotePdf(context, business, quote)
            }
            repository.markQuoteSent(quote.id, pdfLocalPath = file.absolutePath)
            _statusBannerMessage.value = "PDF ready: ${file.name}"
            onGenerated(file)
        }
    }

    fun markQuoteSent(quoteId: String) {
        viewModelScope.launch {
            repository.markQuoteSent(quoteId)
            _statusBannerMessage.value = "Quote marked as Sent"
        }
    }

    fun simulateCustomerESign(quoteId: String, signerName: String, payDeposit: Boolean) {
        viewModelScope.launch {
            repository.markQuoteAcceptedByCustomer(quoteId, signerName, payDeposit)
        }
    }

    fun convertQuoteToInvoice(quoteId: String, onInvoiceCreated: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.convertQuoteToInvoice(quoteId)
            result.onSuccess { invoice ->
                _statusBannerMessage.value = "Invoice ${invoice.invoiceNumber} created"
                onInvoiceCreated(invoice.id)
            }
        }
    }

    fun toggleAutoReminders(invoiceId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleInvoiceAutoReminders(invoiceId, enabled)
        }
    }

    fun recordReminderSent(invoiceId: String) {
        viewModelScope.launch {
            repository.recordReminderSent(invoiceId)
            _statusBannerMessage.value = "Payment reminder logged"
        }
    }

    fun markInvoicePaidInFull(invoiceId: String) {
        viewModelScope.launch {
            repository.markInvoicePaidInFull(invoiceId)
            _statusBannerMessage.value = "Invoice marked Paid in Full"
        }
    }

    fun generateInvoicePdf(
        context: Context,
        business: Business,
        invoice: Invoice,
        onGenerated: (File) -> Unit
    ) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                QuotePdfGenerator.generateInvoicePdf(context, business, invoice)
            }
            _statusBannerMessage.value = "Invoice PDF ready: ${file.name}"
            onGenerated(file)
        }
    }

    fun savePriceBookItem(
        businessId: String,
        existingId: String?,
        nameEn: String,
        nameEs: String,
        category: LineItemCategory,
        unit: PricingUnit,
        unitPriceCents: Long
    ) {
        viewModelScope.launch {
            val item = PriceBookItem(
                id = existingId ?: UUID.randomUUID().toString(),
                businessId = businessId,
                name = nameEn.trim(),
                nameEs = nameEs.trim(),
                category = category,
                unit = unit,
                unitPriceCents = unitPriceCents.coerceAtLeast(0L),
                keywords = listOf(nameEn.trim(), nameEs.trim()).filter { it.isNotBlank() }
            )
            repository.savePriceBookItem(item)
        }
    }

    fun archivePriceBookItem(itemId: String) {
        viewModelScope.launch {
            repository.archivePriceBookItem(itemId)
        }
    }

    fun upgradeToPro(tier: SubscriptionTier) {
        viewModelScope.launch {
            repository.upgradeSubscriptionTier(tier)
            _statusBannerMessage.value = "AI Voice Quote Pro unlocked (local preview)"
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = QuoteVoiceDatabase.getInstance(context.applicationContext)
            val repo = QuoteVoiceRepository(db)
            val aiRepo = PlaceholderQuoteExtractionRepository()
            return QuoteVoiceViewModel(repo, aiRepo) as T
        }
    }
}
