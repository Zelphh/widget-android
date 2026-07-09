package com.tartari.cajuwidget.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartRendererTest {

    @Test
    fun `formata valor positivo abreviado sem centavos`() =
        assertEquals("R$13", ChartRenderer.formatarValorAbreviado(13_40L))

    @Test
    fun `formata valor negativo abreviado sem centavos`() =
        assertEquals("-R$5", ChartRenderer.formatarValorAbreviado(-5_90L))

    @Test
    fun `formata zero como R$0`() =
        assertEquals("R$0", ChartRenderer.formatarValorAbreviado(0L))

    @Test
    fun `cabe no tamanho base quando a barra e larga`() {
        val tamanho = ChartRenderer.tamanhoDeFonteParaRotulo(
            texto = "R$13",
            passoPx = 100f,
            tamanhoBase = 12f,
            tamanhoMinimo = 6f,
        )
        assertEquals(12f, tamanho)
    }

    @Test
    fun `reduz o tamanho da fonte quando a barra e estreita`() {
        val tamanho = ChartRenderer.tamanhoDeFonteParaRotulo(
            texto = "R$570",
            passoPx = 20f,
            tamanhoBase = 12f,
            tamanhoMinimo = 4f,
        )
        assertTrue(tamanho in 4f..12f)
    }

    @Test
    fun `retorna zero quando nem o tamanho minimo cabe`() {
        val tamanho = ChartRenderer.tamanhoDeFonteParaRotulo(
            texto = "R$1.234",
            passoPx = 8f,
            tamanhoBase = 12f,
            tamanhoMinimo = 6f,
        )
        assertEquals(0f, tamanho)
    }
}
