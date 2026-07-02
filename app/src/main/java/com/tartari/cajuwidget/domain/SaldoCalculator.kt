package com.tartari.cajuwidget.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** Saldo acumulado ao fim de um dia, em centavos. */
data class SaldoDia(val data: LocalDate, val saldoCentavos: Long)

/**
 * Lógica pura do saldo do vale-alimentação:
 *
 *     saldo(dia) = saldo(dia - 1) + (30 se dia útil senão 0) - gastos(dia)
 *
 * - Crédito de R$30,00 em todo dia útil (seg–sex, exceto feriados).
 * - Saldo acumulativo desde o 1º dia do mês; pode ficar negativo.
 * - Fim de semana/feriado: sem crédito, saldo do dia anterior permanece.
 */
object SaldoCalculator {

    const val CREDITO_DIA_UTIL_CENTAVOS = 30_00L

    fun ehDiaUtil(data: LocalDate, feriados: Set<LocalDate> = emptySet()): Boolean =
        data.dayOfWeek != DayOfWeek.SATURDAY &&
            data.dayOfWeek != DayOfWeek.SUNDAY &&
            data !in feriados

    /**
     * Série de saldos do dia 1º do mês de [ate] até [ate] (inclusive),
     * um ponto por dia corrido — fins de semana entram como barra "flat"
     * do saldo anterior.
     */
    fun serieDoMes(
        ate: LocalDate,
        gastosPorDia: Map<LocalDate, Long>,
        feriados: Set<LocalDate> = emptySet(),
    ): List<SaldoDia> {
        val serie = ArrayList<SaldoDia>(ate.dayOfMonth)
        var saldo = 0L
        var dia = ate.withDayOfMonth(1)
        while (!dia.isAfter(ate)) {
            if (ehDiaUtil(dia, feriados)) saldo += CREDITO_DIA_UTIL_CENTAVOS
            saldo -= gastosPorDia[dia] ?: 0L
            serie.add(SaldoDia(dia, saldo))
            dia = dia.plusDays(1)
        }
        return serie
    }
}
