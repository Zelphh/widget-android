package com.tartari.cajuwidget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SaldoCalculatorTest {

    // Junho/2026: dia 1º é segunda-feira.
    private fun dia(d: Int): LocalDate = LocalDate.of(2026, 6, d)

    @Test
    fun `dia util credita 30 reais`() {
        val serie = SaldoCalculator.serieDoMes(dia(1), emptyMap())
        assertEquals(30_00L, serie.single().saldoCentavos)
    }

    @Test
    fun `saldo acumula dia a dia nos dias uteis`() {
        val serie = SaldoCalculator.serieDoMes(dia(5), emptyMap()) // seg a sex
        assertEquals(listOf(30_00L, 60_00L, 90_00L, 120_00L, 150_00L), serie.map { it.saldoCentavos })
    }

    @Test
    fun `fim de semana nao credita mas saldo permanece`() {
        val serie = SaldoCalculator.serieDoMes(dia(7), emptyMap()) // sáb=6, dom=7
        assertEquals(150_00L, serie[4].saldoCentavos) // sexta
        assertEquals(150_00L, serie[5].saldoCentavos) // sábado: flat
        assertEquals(150_00L, serie[6].saldoCentavos) // domingo: flat
    }

    @Test
    fun `gasto e subtraido no dia em que ocorreu`() {
        val gastos = mapOf(dia(2) to 25_50L)
        val serie = SaldoCalculator.serieDoMes(dia(3), gastos)
        assertEquals(30_00L, serie[0].saldoCentavos)
        assertEquals(34_50L, serie[1].saldoCentavos) // 60,00 - 25,50
        assertEquals(64_50L, serie[2].saldoCentavos)
    }

    @Test
    fun `saldo pode ficar negativo`() {
        val gastos = mapOf(dia(1) to 80_00L)
        val serie = SaldoCalculator.serieDoMes(dia(2), gastos)
        assertEquals(-50_00L, serie[0].saldoCentavos)
        assertEquals(-20_00L, serie[1].saldoCentavos) // recupera com o crédito seguinte
    }

    @Test
    fun `gasto no fim de semana subtrai sem credito`() {
        val gastos = mapOf(dia(6) to 10_00L) // sábado
        val serie = SaldoCalculator.serieDoMes(dia(6), gastos)
        assertEquals(140_00L, serie.last().saldoCentavos) // 5 dias úteis * 30 - 10
    }

    @Test
    fun `feriado nao credita`() {
        val feriado = dia(4) // quinta
        val serie = SaldoCalculator.serieDoMes(dia(5), emptyMap(), setOf(feriado))
        assertEquals(90_00L, serie[3].saldoCentavos) // quinta: flat
        assertEquals(120_00L, serie[4].saldoCentavos)
    }

    @Test
    fun `varios gastos no mesmo dia sao somados pelo mapa de entrada`() {
        val gastos = mapOf(dia(1) to 12_00L + 8_50L)
        val serie = SaldoCalculator.serieDoMes(dia(1), gastos)
        assertEquals(9_50L, serie.single().saldoCentavos)
    }

    @Test
    fun `serie cobre todos os dias corridos ate hoje`() {
        val serie = SaldoCalculator.serieDoMes(dia(15), emptyMap())
        assertEquals(15, serie.size)
        assertEquals(dia(1), serie.first().data)
        assertEquals(dia(15), serie.last().data)
    }

    @Test
    fun `ehDiaUtil reconhece semana fim de semana e feriado`() {
        assertTrue(SaldoCalculator.ehDiaUtil(dia(1)))            // segunda
        assertFalse(SaldoCalculator.ehDiaUtil(dia(6)))           // sábado
        assertFalse(SaldoCalculator.ehDiaUtil(dia(7)))           // domingo
        assertFalse(SaldoCalculator.ehDiaUtil(dia(1), setOf(dia(1))))
    }
}
