package com.tartari.cajuwidget.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Crédito mensal do saldo REAL do cartão — separado da série de controle em
 * SaldoCalculator.serieDoMes. Puro, sem dependências de Android.
 *
 * No mundo real o cartão recebe o depósito do mês inteiro de uma vez, no dia
 * 1º: 30 x quantidade de dias úteis do mês.
 */
object CreditoMensal {

    /** Quantidade de dias úteis no mês inteiro, reaproveitando a regra de SaldoCalculator. */
    fun diasUteis(mes: YearMonth, feriados: Set<LocalDate> = emptySet()): Int {
        var dia = mes.atDay(1)
        val fim = mes.atEndOfMonth()
        var total = 0
        while (!dia.isAfter(fim)) {
            if (SaldoCalculator.ehDiaUtil(dia, feriados)) total++
            dia = dia.plusDays(1)
        }
        return total
    }

    /**
     * Crédito acumulado dos meses no intervalo (ultimoCreditado, mesAtual],
     * com catch-up de meses pulados. Retorna 0 se mesAtual não for posterior
     * a ultimoCreditado.
     */
    fun creditoEntre(
        ultimoCreditado: YearMonth,
        mesAtual: YearMonth,
        feriados: Set<LocalDate> = emptySet(),
        creditoDiarioCentavos: Long = SaldoCalculator.CREDITO_DIA_UTIL_CENTAVOS,
    ): Long {
        if (!mesAtual.isAfter(ultimoCreditado)) return 0L
        var total = 0L
        var mes = ultimoCreditado.plusMonths(1)
        while (!mes.isAfter(mesAtual)) {
            total += diasUteis(mes, feriados) * creditoDiarioCentavos
            mes = mes.plusMonths(1)
        }
        return total
    }
}
