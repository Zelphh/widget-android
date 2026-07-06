package com.tartari.cajuwidget.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CreditoMensalTest {

    // Julho/2026: dia 1º é quarta-feira, 23 dias úteis (8 dias de fim de semana em 31).
    private val julho2026 = YearMonth.of(2026, 7)

    @Test
    fun `conta dias uteis de um mes conhecido`() {
        assertEquals(23, CreditoMensal.diasUteis(julho2026))
    }

    @Test
    fun `feriado reduz a contagem de dias uteis`() {
        val feriado = LocalDate.of(2026, 7, 1) // quarta-feira, dia útil
        assertEquals(22, CreditoMensal.diasUteis(julho2026, setOf(feriado)))
    }

    @Test
    fun `mes onde todo dia util e feriado nao credita nada`() {
        val todosOsDiasUteis = (1..31)
            .map { LocalDate.of(2026, 7, it) }
            .filter { SaldoCalculator.ehDiaUtil(it) }
            .toSet()
        assertEquals(0, CreditoMensal.diasUteis(julho2026, todosOsDiasUteis))
    }

    @Test
    fun `mesmo mes nao credita`() {
        assertEquals(0L, CreditoMensal.creditoEntre(julho2026, julho2026))
    }

    @Test
    fun `mes anterior ao ultimo creditado nao credita`() {
        assertEquals(0L, CreditoMensal.creditoEntre(julho2026, YearMonth.of(2026, 6)))
    }

    @Test
    fun `credita um unico mes de rollover`() {
        val junho2026 = YearMonth.of(2026, 6)
        val esperado = CreditoMensal.diasUteis(julho2026) * SaldoCalculator.CREDITO_DIA_UTIL_CENTAVOS
        assertEquals(esperado, CreditoMensal.creditoEntre(junho2026, julho2026))
    }

    @Test
    fun `credita usando valor diario customizado`() {
        val junho2026 = YearMonth.of(2026, 6)
        val esperado = CreditoMensal.diasUteis(julho2026) * 35_00L
        assertEquals(esperado, CreditoMensal.creditoEntre(junho2026, julho2026, creditoDiarioCentavos = 35_00L))
    }

    @Test
    fun `credita meses pulados em catch-up`() {
        val janeiro2026 = YearMonth.of(2026, 1)
        val abril2026 = YearMonth.of(2026, 4)
        val esperado = (2..4).sumOf {
            CreditoMensal.diasUteis(YearMonth.of(2026, it)) * SaldoCalculator.CREDITO_DIA_UTIL_CENTAVOS
        }
        assertEquals(esperado, CreditoMensal.creditoEntre(janeiro2026, abril2026))
    }
}
