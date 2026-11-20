package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.invoice.InvoiceDetailScreen
import com.example.ui.invoice.InvoicesListScreen
import com.example.ui.invoice.PriceBookScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.preview.QuotePreviewScreen
import com.example.ui.quotes.QuotesListScreen
import com.example.ui.record.RecordScreen
import com.example.ui.review.ReviewEditQuoteScreen

object QuoteVoiceRoutes {
    const val ONBOARDING = "onboarding"
    const val RECORD = "record"
    const val QUOTES = "quotes"
    const val INVOICES = "invoices"
    const val PRICE_BOOK = "price_book"
    const val REVIEW = "review/{quoteId}"
    const val PREVIEW = "preview/{quoteId}"
    const val INVOICE_DETAIL = "invoice/{invoiceId}"

    fun review(quoteId: String) = "review/$quoteId"
    fun preview(quoteId: String) = "preview/$quoteId"
    fun invoiceDetail(invoiceId: String) = "invoice/$invoiceId"
}

@Composable
fun QuoteVoiceApp(viewModel: QuoteVoiceViewModel) {
    val context = LocalContext.current
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val extractionState by viewModel.extractionState.collectAsStateWithLifecycle()
    val draftTranscript by viewModel.draftTranscript.collectAsStateWithLifecycle()
    val spokenLanguage by viewModel.spokenLanguage.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(bannerMessage) {
        bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    if (appState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val startDestination = if (appState.business?.onboardingCompleted == true) {
        QuoteVoiceRoutes.RECORD
    } else {
        QuoteVoiceRoutes.ONBOARDING
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelRoutes = setOf(
        QuoteVoiceRoutes.RECORD,
        QuoteVoiceRoutes.QUOTES,
        QuoteVoiceRoutes.INVOICES,
        QuoteVoiceRoutes.PRICE_BOOK
    )
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == QuoteVoiceRoutes.RECORD,
                        onClick = {
                            navController.navigate(QuoteVoiceRoutes.RECORD) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_record)) },
                        modifier = Modifier.testTag("nav_tab_record")
                    )
                    NavigationBarItem(
                        selected = currentRoute == QuoteVoiceRoutes.QUOTES,
                        onClick = {
                            navController.navigate(QuoteVoiceRoutes.QUOTES) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_quotes)) },
                        modifier = Modifier.testTag("nav_tab_quotes")
                    )
                    NavigationBarItem(
                        selected = currentRoute == QuoteVoiceRoutes.INVOICES,
                        onClick = {
                            navController.navigate(QuoteVoiceRoutes.INVOICES) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_invoices)) },
                        modifier = Modifier.testTag("nav_tab_invoices")
                    )
                    NavigationBarItem(
                        selected = currentRoute == QuoteVoiceRoutes.PRICE_BOOK,
                        onClick = {
                            navController.navigate(QuoteVoiceRoutes.PRICE_BOOK) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.LibraryBooks, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_price_book)) },
                        modifier = Modifier.testTag("nav_tab_price_book")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(QuoteVoiceRoutes.ONBOARDING) {
                OnboardingScreen(
                    existingBusiness = appState.business,
                    onComplete = { name, trade, phone, license, tax, deposit, lang, logoUri ->
                        viewModel.completeOnboarding(
                            businessName = name,
                            trade = trade,
                            phone = phone,
                            licenseNumber = license,
                            taxRatePercent = tax,
                            depositPercent = deposit,
                            defaultLanguage = lang,
                            logoLocalUri = logoUri,
                            onDone = {
                                navController.navigate(QuoteVoiceRoutes.RECORD) {
                                    popUpTo(QuoteVoiceRoutes.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }
                )
            }

            composable(QuoteVoiceRoutes.RECORD) {
                RecordScreen(
                    business = appState.business,
                    transcript = draftTranscript,
                    spokenLanguage = spokenLanguage,
                    extractionState = extractionState,
                    isAiPlaceholderMode = appState.isAiPlaceholderMode,
                    onTranscriptChange = viewModel::updateDraftTranscript,
                    onSpokenLanguageChange = viewModel::selectSpokenLanguage,
                    onSelectSample = viewModel::loadSampleFixture,
                    onBuildQuote = {
                        viewModel.extractAndCreateDraftQuote { quoteId ->
                            navController.navigate(QuoteVoiceRoutes.review(quoteId))
                        }
                    },
                    onDismissError = viewModel::clearExtractionError
                )
            }

            composable(
                route = QuoteVoiceRoutes.REVIEW,
                arguments = listOf(navArgument("quoteId") { type = NavType.StringType })
            ) { entry ->
                val quoteId = entry.arguments?.getString("quoteId").orEmpty()
                val quote = appState.quotes.firstOrNull { it.id == quoteId }
                if (quote != null) {
                    ReviewEditQuoteScreen(
                        quote = quote,
                        priceBook = appState.priceBook,
                        isAiPlaceholderMode = appState.isAiPlaceholderMode,
                        onBack = { navController.popBackStack() },
                        onUpdateQuote = viewModel::updateQuote,
                        onDeleteLineItem = { lineId -> viewModel.removeLineItem(quote, lineId) },
                        onSaveLineItem = { item -> viewModel.upsertLineItem(quote, item) },
                        onAddFromPriceBook = { pb -> viewModel.addItemFromPriceBook(quote, pb) },
                        onOpenPreview = { navController.navigate(QuoteVoiceRoutes.preview(quote.id)) }
                    )
                }
            }

            composable(
                route = QuoteVoiceRoutes.PREVIEW,
                arguments = listOf(navArgument("quoteId") { type = NavType.StringType })
            ) { entry ->
                val quoteId = entry.arguments?.getString("quoteId").orEmpty()
                val quote = appState.quotes.firstOrNull { it.id == quoteId }
                val business = appState.business
                if (quote != null && business != null) {
                    QuotePreviewScreen(
                        business = business,
                        quote = quote,
                        onBack = { navController.popBackStack() },
                        onSwitchLanguage = { lang -> viewModel.switchQuoteLanguage(quote.id, lang) },
                        onExportPdf = { onFile ->
                            viewModel.generateAndSaveQuotePdf(context, business, quote, onFile)
                        },
                        onMarkSent = { viewModel.markQuoteSent(quote.id) },
                        onSimulateESign = { signer, payDeposit ->
                            viewModel.simulateCustomerESign(quote.id, signer, payDeposit)
                        },
                        onConvertToInvoice = {
                            viewModel.convertQuoteToInvoice(quote.id) { invoiceId ->
                                navController.navigate(QuoteVoiceRoutes.invoiceDetail(invoiceId))
                            }
                        }
                    )
                }
            }

            composable(QuoteVoiceRoutes.QUOTES) {
                QuotesListScreen(
                    business = appState.business,
                    quotes = appState.quotes,
                    onOpenReview = { id -> navController.navigate(QuoteVoiceRoutes.review(id)) },
                    onOpenPreview = { id -> navController.navigate(QuoteVoiceRoutes.preview(id)) },
                    onConvertToInvoice = { quoteId ->
                        viewModel.convertQuoteToInvoice(quoteId) { invoiceId ->
                            navController.navigate(QuoteVoiceRoutes.invoiceDetail(invoiceId))
                        }
                    },
                    onUpgradePro = viewModel::upgradeToPro
                )
            }

            composable(QuoteVoiceRoutes.INVOICES) {
                InvoicesListScreen(
                    invoices = appState.invoices,
                    onOpenInvoice = { id -> navController.navigate(QuoteVoiceRoutes.invoiceDetail(id)) }
                )
            }

            composable(
                route = QuoteVoiceRoutes.INVOICE_DETAIL,
                arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
            ) { entry ->
                val invoiceId = entry.arguments?.getString("invoiceId").orEmpty()
                val invoice = appState.invoices.firstOrNull { it.id == invoiceId }
                val business = appState.business
                if (invoice != null && business != null) {
                    InvoiceDetailScreen(
                        business = business,
                        invoice = invoice,
                        onBack = { navController.popBackStack() },
                        onToggleAutoReminders = { enabled -> viewModel.toggleAutoReminders(invoice.id, enabled) },
                        onSendReminder = { viewModel.recordReminderSent(invoice.id) },
                        onMarkPaid = { viewModel.markInvoicePaidInFull(invoice.id) },
                        onExportInvoicePdf = { onFile ->
                            viewModel.generateInvoicePdf(context, business, invoice, onFile)
                        }
                    )
                }
            }

            composable(QuoteVoiceRoutes.PRICE_BOOK) {
                PriceBookScreen(
                    business = appState.business,
                    items = appState.priceBook,
                    onSaveItem = { existingId, nameEn, nameEs, category, unit, cents ->
                        appState.business?.id?.let { bizId ->
                            viewModel.savePriceBookItem(
                                businessId = bizId,
                                existingId = existingId,
                                nameEn = nameEn,
                                nameEs = nameEs,
                                category = category,
                                unit = unit,
                                unitPriceCents = cents
                            )
                        }
                    },
                    onEditShopProfile = {
                        navController.navigate(QuoteVoiceRoutes.ONBOARDING)
                    }
                )
            }
        }
    }
}
