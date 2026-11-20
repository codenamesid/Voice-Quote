package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BusinessDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.InvoiceDao
import com.example.data.local.dao.PriceBookDao
import com.example.data.local.dao.QuoteDao
import com.example.data.local.entity.BusinessEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceLineItemEntity
import com.example.data.local.entity.PriceBookItemEntity
import com.example.data.local.entity.QuoteEntity
import com.example.data.local.entity.QuoteLineItemEntity

@Database(
    entities = [
        BusinessEntity::class,
        CustomerEntity::class,
        PriceBookItemEntity::class,
        QuoteEntity::class,
        QuoteLineItemEntity::class,
        InvoiceEntity::class,
        InvoiceLineItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class QuoteVoiceDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun customerDao(): CustomerDao
    abstract fun priceBookDao(): PriceBookDao
    abstract fun quoteDao(): QuoteDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: QuoteVoiceDatabase? = null

        fun getInstance(context: Context): QuoteVoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuoteVoiceDatabase::class.java,
                    "quotevoice_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
