package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.domain.model.Business
import com.example.domain.model.Invoice
import com.example.domain.model.LineItem
import com.example.domain.model.Quote
import com.example.domain.model.SupportedLanguage
import com.example.domain.pricing.CurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates branded, bilingual (EN / ES) PDF quotes and invoices completely on-device
 * using [PdfDocument] so contractors can export and send documents even with zero signal.
 */
object QuotePdfGenerator {

    private const val PAGE_WIDTH = 612  // US Letter 8.5" @ 72dpi
    private const val PAGE_HEIGHT = 792 // US Letter 11" @ 72dpi
    private const val MARGIN = 40f

    fun generateQuotePdf(context: Context, business: Business, quote: Quote): File {
        val isEs = quote.outputLanguage == SupportedLanguage.ES
        return generateDocumentPdf(
            context = context,
            fileName = "${quote.quoteNumber.replace(" ", "_")}.pdf",
            docTypeLabel = if (isEs) "COTIZACIÓN" else "QUOTE",
            docNumber = quote.quoteNumber,
            title = quote.title,
            language = quote.outputLanguage,
            business = business,
            customerName = quote.customerNameSnapshot,
            customerAddress = quote.customerAddressSnapshot,
            customerPhone = quote.customerPhoneSnapshot,
            lineItems = quote.lineItems,
            subtotalCents = quote.subtotalCents,
            taxRateBps = quote.taxRateBps,
            taxCents = quote.taxCents,
            totalCents = quote.totalCents,
            secondaryRowLabel = if (isEs) "Depósito (${quote.depositPercent}%)" else "Deposit Due (${quote.depositPercent}%)",
            secondaryRowCents = quote.depositCents,
            notes = quote.notes,
            terms = quote.termsSnapshot,
            signerName = quote.signerName,
            createdAtEpochMs = quote.createdAtEpochMs
        )
    }

    fun generateInvoicePdf(context: Context, business: Business, invoice: Invoice): File {
        val isEs = invoice.outputLanguage == SupportedLanguage.ES
        return generateDocumentPdf(
            context = context,
            fileName = "${invoice.invoiceNumber.replace(" ", "_")}.pdf",
            docTypeLabel = if (isEs) "FACTURA" else "INVOICE",
            docNumber = invoice.invoiceNumber,
            title = invoice.title,
            language = invoice.outputLanguage,
            business = business,
            customerName = invoice.customerNameSnapshot,
            customerAddress = invoice.customerAddressSnapshot,
            customerPhone = invoice.customerPhoneSnapshot,
            lineItems = invoice.lineItems,
            subtotalCents = invoice.subtotalCents,
            taxRateBps = invoice.taxRateBps,
            taxCents = invoice.taxCents,
            totalCents = invoice.totalCents,
            secondaryRowLabel = if (isEs) "Saldo Pendiente" else "Balance Due",
            secondaryRowCents = invoice.balanceDueCents,
            notes = invoice.notes,
            terms = business.defaultTermsFor(invoice.outputLanguage),
            signerName = null,
            createdAtEpochMs = invoice.createdAtEpochMs
        )
    }

    private fun generateDocumentPdf(
        context: Context,
        fileName: String,
        docTypeLabel: String,
        docNumber: String,
        title: String,
        language: SupportedLanguage,
        business: Business,
        customerName: String,
        customerAddress: String,
        customerPhone: String,
        lineItems: List<LineItem>,
        subtotalCents: Long,
        taxRateBps: Int,
        taxCents: Long,
        totalCents: Long,
        secondaryRowLabel: String,
        secondaryRowCents: Long,
        notes: String,
        terms: String,
        signerName: String?,
        createdAtEpochMs: Long
    ): File {
        val isEs = language == SupportedLanguage.ES
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val slatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#17212D") }
        val amberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F59E0B") }
        val mutedFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F3F4F6") }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }

        val whiteHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val whiteSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D1D5DB")
            textSize = 10f
        }
        val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val darkBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val darkBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = 10f
        }
        val rightBody = Paint(darkBody).apply { textAlign = Paint.Align.RIGHT }
        val rightBold = Paint(darkBold).apply { textAlign = Paint.Align.RIGHT }

        // Header banner
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, slatePaint)
        canvas.drawRect(0f, 106f, PAGE_WIDTH.toFloat(), 112f, amberPaint)

        var textStartX = MARGIN
        business.logoLocalUri?.let { uriStr ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bmp ->
                        val dst = RectF(MARGIN, 22f, MARGIN + 60f, 82f)
                        canvas.drawBitmap(bmp, null, dst, null)
                        textStartX = MARGIN + 74f
                    }
                }
            }
        }

        canvas.drawText(business.name.ifBlank { "AI Voice Quote Contractor" }, textStartX, 46f, whiteHeader)
        val bizMeta = buildList {
            if (business.licenseNumber.isNotBlank()) {
                add((if (isEs) "Licencia #: " else "Lic #: ") + business.licenseNumber)
            }
            if (business.phone.isNotBlank()) add(business.phone)
            if (business.email.isNotBlank()) add(business.email)
        }.joinToString("  •  ")
        if (bizMeta.isNotBlank()) {
            canvas.drawText(bizMeta, textStartX, 66f, whiteSub)
        }
        val dateStr = SimpleDateFormat("MMM dd, yyyy", if (isEs) Locale("es", "US") else Locale.US)
            .format(Date(createdAtEpochMs))
        canvas.drawText("${if (isEs) "Fecha" else "Date"}: $dateStr", textStartX, 84f, whiteSub)

        // Document badge on right
        val pillRect = RectF(PAGE_WIDTH - 190f, 26f, PAGE_WIDTH - MARGIN, 72f)
        canvas.drawRoundRect(pillRect, 8f, 8f, amberPaint)
        canvas.drawText(docTypeLabel, PAGE_WIDTH - MARGIN - 12f, 46f, badgeText)
        canvas.drawText(docNumber, PAGE_WIDTH - MARGIN - 12f, 62f, badgeText)

        var y = 136f

        // Customer & Job block
        canvas.drawText(if (isEs) "PREPARADO PARA" else "PREPARED FOR", MARGIN, y, darkBold)
        y += 16f
        canvas.drawText(customerName.ifBlank { if (isEs) "Cliente" else "Valued Customer" }, MARGIN, y, darkBold)
        y += 14f
        if (customerAddress.isNotBlank()) {
            canvas.drawText(customerAddress, MARGIN, y, darkBody)
            y += 14f
        }
        if (customerPhone.isNotBlank()) {
            canvas.drawText(customerPhone, MARGIN, y, darkBody)
            y += 14f
        }
        if (title.isNotBlank()) {
            y += 6f
            canvas.drawText("${if (isEs) "Proyecto" else "Job"}: $title", MARGIN, y, darkBold)
            y += 18f
        }

        // Table Header
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 24f, mutedFillPaint)
        canvas.drawText(if (isEs) "DESCRIPCIÓN" else "DESCRIPTION", MARGIN + 8f, y + 16f, darkBold)
        canvas.drawText(if (isEs) "CANT." else "QTY", 370f, y + 16f, rightBold)
        canvas.drawText(if (isEs) "PRECIO" else "RATE", 465f, y + 16f, rightBold)
        canvas.drawText(if (isEs) "IMPORTE" else "AMOUNT", PAGE_WIDTH - MARGIN - 8f, y + 16f, rightBold)
        y += 38f

        // Line items
        for (item in lineItems) {
            if (y > PAGE_HEIGHT - 190f) break
            val unitLabel = if (isEs) item.unit.labelEs else item.unit.labelEn
            canvas.drawText(item.name.take(48), MARGIN + 8f, y, darkBold)
            canvas.drawText("${CurrencyFormatter.formatQuantity(item.quantity)} $unitLabel", 370f, y, rightBody)
            canvas.drawText(CurrencyFormatter.formatCents(item.unitPriceCents), 465f, y, rightBody)
            canvas.drawText(CurrencyFormatter.formatCents(item.lineTotalCents), PAGE_WIDTH - MARGIN - 8f, y, rightBold)
            y += 13f
            if (item.description.isNotBlank()) {
                canvas.drawText(item.description.take(65), MARGIN + 8f, y, darkBody)
                y += 12f
            }
            canvas.drawLine(MARGIN, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, dividerPaint)
            y += 14f
        }

        // Totals block
        y += 10f
        val totalsLabelX = 390f
        val totalsValX = PAGE_WIDTH - MARGIN - 8f
        canvas.drawText(if (isEs) "Subtotal" else "Subtotal", totalsLabelX, y, darkBody)
        canvas.drawText(CurrencyFormatter.formatCents(subtotalCents), totalsValX, y, rightBody)
        y += 16f

        if (taxRateBps > 0) {
            val taxPct = String.format(Locale.US, "%.2f%%", taxRateBps / 100.0)
            canvas.drawText("${if (isEs) "Impuesto" else "Tax"} ($taxPct)", totalsLabelX, y, darkBody)
            canvas.drawText(CurrencyFormatter.formatCents(taxCents), totalsValX, y, rightBody)
            y += 16f
        }

        canvas.drawLine(totalsLabelX, y - 4f, PAGE_WIDTH - MARGIN, y - 4f, dividerPaint)
        y += 12f
        val grandPaint = Paint(darkBold).apply { textSize = 14f }
        val grandRight = Paint(rightBold).apply { textSize = 14f }
        canvas.drawText("TOTAL", totalsLabelX, y, grandPaint)
        canvas.drawText(CurrencyFormatter.formatCents(totalCents), totalsValX, y, grandRight)
        y += 20f

        canvas.drawText(secondaryRowLabel, totalsLabelX, y, darkBold)
        canvas.drawText(CurrencyFormatter.formatCents(secondaryRowCents), totalsValX, y, rightBold)
        y += 28f

        // Notes & Terms
        if (notes.isNotBlank() && y < PAGE_HEIGHT - 110f) {
            canvas.drawText(if (isEs) "NOTAS" else "NOTES", MARGIN, y, darkBold)
            y += 14f
            canvas.drawText(notes.take(92), MARGIN, y, darkBody)
            y += 22f
        }
        if (terms.isNotBlank() && y < PAGE_HEIGHT - 70f) {
            canvas.drawText(if (isEs) "TÉRMINOS" else "TERMS", MARGIN, y, darkBold)
            y += 14f
            canvas.drawText(terms.take(96), MARGIN, y, darkBody)
            y += 20f
        }
        if (!signerName.isNullOrBlank() && y < PAGE_HEIGHT - 36f) {
            canvas.drawText(
                if (isEs) "Firmado electrónicamente por: $signerName" else "Electronically signed by: $signerName",
                MARGIN,
                y,
                darkBold
            )
        }

        document.finishPage(page)

        val pdfDir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val outFile = File(pdfDir, fileName)
        FileOutputStream(outFile).use { stream ->
            document.writeTo(stream)
        }
        document.close()
        return outFile
    }
}

/**
 * Zero-permission sharing helpers compliant with Google Play Creator Program
 * (never uses prohibited SEND_SMS or broad storage permissions).
 */
object QuoteShareHelper {

    fun buildShareSummary(business: Business, quote: Quote): String {
        val isEs = quote.outputLanguage == SupportedLanguage.ES
        val total = CurrencyFormatter.formatCents(quote.totalCents)
        val link = quote.shareUrl.orEmpty()
        return if (isEs) {
            "Hola ${quote.customerNameSnapshot.ifBlank { "" }}, aquí tienes tu cotización ${quote.quoteNumber} de ${business.name} por $total. Revísala y fírmala aquí: $link"
        } else {
            "Hi ${quote.customerNameSnapshot.ifBlank { "" }}, here is your quote ${quote.quoteNumber} from ${business.name} for $total. Review & e-sign here: $link"
        }.trim()
    }

    fun launchSmsShare(context: Context, phone: String, message: String): Boolean {
        val uri = Uri.parse("smsto:${Uri.encode(phone)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Send SMS"))
            true
        }.getOrDefault(false)
    }

    fun launchEmailShare(context: Context, email: String, subject: String, body: String): Boolean {
        val uri = Uri.parse("mailto:${Uri.encode(email)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Send Email"))
            true
        }.getOrDefault(false)
    }

    fun launchPdfShare(context: Context, pdfFile: File, shareMessage: String): Boolean {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Share PDF"))
            true
        }.getOrDefault(false)
    }
}
