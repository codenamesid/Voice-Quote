package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "quotes",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["customerId"]),
        Index(value = ["status"]),
        Index(value = ["syncStatus"])
    ]
)
data class QuoteEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val customerId: String?,
    val customerNameSnapshot: String,
    val customerAddressSnapshot: String,
    val customerPhoneSnapshot: String,
    val customerEmailSnapshot: String,
    val quoteNumber: String,
    val title: String,
    val rawTranscript: String,
    val spokenLanguage: String,
    val outputLanguage: String,
    val notes: String,
    val termsSnapshot: String,
    val taxRateBps: Int,
    val depositPercent: Int,
    val status: String,
    val extractionPending: Boolean,
    val audioLocalPath: String?,
    val shareToken: String?,
    val shareUrl: String?,
    val pdfLocalPath: String?,
    val validUntilEpochMs: Long?,
    val sentAtEpochMs: Long?,
    val viewedAtEpochMs: Long?,
    val acceptedAtEpochMs: Long?,
    val signerName: String?,
    val signatureSvgPath: String?,
    val stripeDepositPaymentIntentId: String?,
    val depositPaidCents: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: String
)

@Entity(
    tableName = "quote_line_items",
    foreignKeys = [
        ForeignKey(
            entity = QuoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["quoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["quoteId"])]
)
data class QuoteLineItemEntity(
    @PrimaryKey val id: String,
    val quoteId: String,
    val sortOrder: Int,
    val name: String,
    val description: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val unitPriceCents: Long,
    val taxable: Boolean,
    val priceSource: String,
    val priceBookItemId: String?,
    val aiConfidence: Float?
)

data class QuoteWithLineItems(
    @Embedded val quote: QuoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "quoteId"
    )
    val lineItems: List<QuoteLineItemEntity>
)

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["quoteId"]),
        Index(value = ["status"]),
        Index(value = ["syncStatus"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val quoteId: String?,
    val customerId: String?,
    val customerNameSnapshot: String,
    val customerAddressSnapshot: String,
    val customerPhoneSnapshot: String,
    val customerEmailSnapshot: String,
    val invoiceNumber: String,
    val title: String,
    val outputLanguage: String,
    val taxRateBps: Int,
    val amountPaidCents: Long,
    val status: String,
    val dueDateEpochMs: Long,
    val sentAtEpochMs: Long?,
    val paidAtEpochMs: Long?,
    val shareToken: String?,
    val shareUrl: String?,
    val pdfLocalPath: String?,
    val stripePaymentLinkUrl: String?,
    val stripePaymentIntentId: String?,
    val autoRemindersEnabled: Boolean,
    val reminderCount: Int,
    val lastReminderSentAtEpochMs: Long?,
    val nextReminderAtEpochMs: Long?,
    val notes: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: String
)

@Entity(
    tableName = "invoice_line_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceLineItemEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val sortOrder: Int,
    val name: String,
    val description: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val unitPriceCents: Long,
    val taxable: Boolean,
    val priceSource: String,
    val priceBookItemId: String?,
    val aiConfidence: Float?
)

data class InvoiceWithLineItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val lineItems: List<InvoiceLineItemEntity>
)
