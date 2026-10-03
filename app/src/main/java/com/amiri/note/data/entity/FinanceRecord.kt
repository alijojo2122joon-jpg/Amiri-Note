package com.amiri.note.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Record type for the finance ledger. */
object FinanceType {
    const val INCOME = 0
    const val EXPENSE = 1
    const val DEBT = 2     // money I owe
    const val CREDIT = 3   // money owed to me
}

@Entity(
    tableName = "finance",
    indices = [Index("dayKey"), Index("type"), Index("createdAt")]
)
data class FinanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int = FinanceType.INCOME,
    /** amount stored in minor units is overkill for AFN; keep as double, 2dp in UI */
    val amount: Double = 0.0,
    val currency: String = "AFN",
    val description: String = "",
    val category: String = "General",
    val dayKey: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
