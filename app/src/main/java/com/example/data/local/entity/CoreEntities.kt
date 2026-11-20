package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "businesses",
    indices = [Index(value = ["ownerUid"], unique = true)]
)
data class BusinessEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val name: String,
    val trade: String,
    val phone: String,
    val email: String,
    val address: String,
    val licenseNumber: String,
    val logoLocalUri: String?,
    val logoRemoteUrl: String?,
    val defaultTaxRateBps: Int,
    val defaultDepositPercent: Int,
    val defaultTermsEn: String,
    val defaultTermsEs: String,
    val defaultLanguage: String,
    val quoteNumberPrefix: String,
    val nextQuoteNumber: Int,
    val nextInvoiceNumber: Int,
    val stripeConnectAccountId: String?,
    val stripeChargesEnabled: Boolean,
    val subscriptionTier: String,
    val quotesCreatedThisMonth: Int,
    val quotaPeriodYearMonth: String,
    val onboardingCompleted: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: String
)

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["businessId", "name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val preferredLanguage: String,
    val notes: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: String
)

@Entity(
    tableName = "price_book_items",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["businessId", "category"])
    ]
)
data class PriceBookItemEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val nameEs: String,
    val category: String,
    val unit: String,
    val unitPriceCents: Long,
    val defaultQuantity: Double,
    val keywordsCsv: String,
    val taxable: Boolean,
    val archived: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: String
)
