package com.tartari.cajuwidget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Origem { AUTOMATICO, MANUAL }

@Entity(tableName = "gastos")
data class Gasto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dia: Int,
    val mes: Int,
    val ano: Int,
    val valorCentavos: Long,
    /** "automatico" ou "manual" — útil para auditar a taxa de acerto do parser. */
    val origem: String,
    val criadoEm: Long,
)
