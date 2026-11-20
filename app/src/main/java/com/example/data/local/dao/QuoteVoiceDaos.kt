package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.BusinessEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceLineItemEntity
import com.example.data.local.entity.InvoiceWithLineItems
import com.example.data.local.entity.PriceBookItemEntity
import com.example.data.local.entity.QuoteEntity
import com.example.data.local.entity.QuoteLineItemEntity
import com.example.data.local.entity.QuoteWithLineItems
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses LIMIT 1")
    fun observeBusiness(): Flow<BusinessEntity?>

    @Query("SELECT * FROM businesses LIMIT 1")
    suspend fun getBusinessOnce(): BusinessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBusiness(business: BusinessEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: CustomerEntity)
}

@Dao
interface PriceBookDao {
    @Query("SELECT * FROM price_book_items WHERE archived = 0 ORDER BY category ASC, name ASC")
    fun observeActiveItems(): Flow<List<PriceBookItemEntity>>

    @Query("SELECT * FROM price_book_items WHERE archived = 0")
    suspend fun getActiveItemsOnce(): List<PriceBookItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: PriceBookItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PriceBookItemEntity>)

    @Query("DELETE FROM price_book_items WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: String)

    @Query("UPDATE price_book_items SET archived = 1, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun archiveItem(id: String, updatedAt: Long)
}

@Dao
interface QuoteDao {
    @Transaction
    @Query("SELECT * FROM quotes ORDER BY updatedAtEpochMs DESC")
    fun observeAllQuotesWithLineItems(): Flow<List<QuoteWithLineItems>>

    @Transaction
    @Query("SELECT * FROM quotes WHERE id = :quoteId LIMIT 1")
    fun observeQuoteWithLineItems(quoteId: String): Flow<QuoteWithLineItems?>

    @Transaction
    @Query("SELECT * FROM quotes WHERE id = :quoteId LIMIT 1")
    suspend fun getQuoteWithLineItemsOnce(quoteId: String): QuoteWithLineItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuote(quote: QuoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<QuoteLineItemEntity>)

    @Query("DELETE FROM quote_line_items WHERE quoteId = :quoteId")
    suspend fun deleteLineItemsForQuote(quoteId: String)

    @Transaction
    suspend fun upsertQuoteWithLineItems(quote: QuoteEntity, lineItems: List<QuoteLineItemEntity>) {
        upsertQuote(quote)
        deleteLineItemsForQuote(quote.id)
        if (lineItems.isNotEmpty()) {
            insertLineItems(lineItems)
        }
    }

    @Query("DELETE FROM quotes WHERE id = :quoteId")
    suspend fun deleteQuote(quoteId: String)
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY updatedAtEpochMs DESC")
    fun observeAllInvoicesWithLineItems(): Flow<List<InvoiceWithLineItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    fun observeInvoiceWithLineItems(invoiceId: String): Flow<InvoiceWithLineItems?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceWithLineItemsOnce(invoiceId: String): InvoiceWithLineItems?

    @Transaction
    @Query("SELECT * FROM invoices WHERE quoteId = :quoteId LIMIT 1")
    suspend fun findByQuoteId(quoteId: String): InvoiceWithLineItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<InvoiceLineItemEntity>)

    @Query("DELETE FROM invoice_line_items WHERE invoiceId = :invoiceId")
    suspend fun deleteLineItemsForInvoice(invoiceId: String)

    @Transaction
    suspend fun upsertInvoiceWithLineItems(
        invoice: InvoiceEntity,
        lineItems: List<InvoiceLineItemEntity>
    ) {
        upsertInvoice(invoice)
        deleteLineItemsForInvoice(invoice.id)
        if (lineItems.isNotEmpty()) {
            insertLineItems(lineItems)
        }
    }
}
