package com.tartari.cajuwidget.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GastoDao {
    @Insert
    suspend fun inserir(gasto: Gasto): Long

    @Query("SELECT * FROM gastos WHERE mes = :mes AND ano = :ano")
    suspend fun doMes(mes: Int, ano: Int): List<Gasto>
}
