package com.tartari.cajuwidget.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LimitesDataTest {

    private fun limites(s: String) = LancamentoManualActivity.limitesData(LocalDate.parse(s))

    @Test
    fun `mid-month clamps ao dia 1`() =
        assertEquals(LocalDate.parse("2026-07-01")..LocalDate.parse("2026-07-04"), limites("2026-07-04"))

    @Test
    fun `dia 1 do mes colapsa em um unico dia`() =
        assertEquals(LocalDate.parse("2026-07-01")..LocalDate.parse("2026-07-01"), limites("2026-07-01"))

    @Test
    fun `ultimo dia do mes`() =
        assertEquals(LocalDate.parse("2026-07-01")..LocalDate.parse("2026-07-31"), limites("2026-07-31"))

    @Test
    fun `funciona em fevereiro`() =
        assertEquals(LocalDate.parse("2026-02-01")..LocalDate.parse("2026-02-15"), limites("2026-02-15"))
}
