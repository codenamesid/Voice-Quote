package com.example.domain.model

/**
 * Customer contact and job-site details for a contractor's client.
 */
data class Customer(
    val id: String,
    val businessId: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val preferredLanguage: SupportedLanguage = SupportedLanguage.EN,
    val notes: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPSERT
)
