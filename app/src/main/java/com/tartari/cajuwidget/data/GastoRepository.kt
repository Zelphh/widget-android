package com.tartari.cajuwidget.data

import android.content.Context
import java.time.LocalDate

/**
 * Único ponto de escrita/leitura de gastos.
 * Retenção: meses passados ficam no banco (histórico preservado).
 */
class GastoRepository private constructor(
    private val dao: GastoDao,
    private val saldoTotal: SaldoTotalRepository,
) {

    suspend fun salvarGasto(
        valorCentavos: Long,
        origem: Origem,
        data: LocalDate = LocalDate.now(),
    ) {
        dao.inserir(
            Gasto(
                dia = data.dayOfMonth,
                mes = data.monthValue,
                ano = data.year,
                valorCentavos = valorCentavos,
                origem = origem.name.lowercase(),
                criadoEm = System.currentTimeMillis(),
            )
        )
        saldoTotal.registrarGasto(valorCentavos)
    }

    /** Total gasto por dia (centavos) no mês/ano informados. */
    suspend fun gastosPorDia(mes: Int, ano: Int): Map<LocalDate, Long> =
        dao.doMes(mes, ano)
            .groupBy { LocalDate.of(it.ano, it.mes, it.dia) }
            .mapValues { (_, gastos) -> gastos.sumOf { it.valorCentavos } }

    companion object {
        @Volatile
        private var instancia: GastoRepository? = null

        fun get(context: Context): GastoRepository =
            instancia ?: synchronized(this) {
                instancia ?: GastoRepository(
                    AppDatabase.get(context).gastoDao(),
                    SaldoTotalRepository.get(context),
                ).also { instancia = it }
            }
    }
}
