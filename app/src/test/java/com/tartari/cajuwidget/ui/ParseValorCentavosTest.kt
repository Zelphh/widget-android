package com.tartari.cajuwidget.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseValorCentavosTest {

    private fun parse(s: String) = LancamentoManualActivity.parseValorCentavos(s)

    @Test
    fun `aceita virgula`() = assertEquals(23_50L, parse("23,50"))

    @Test
    fun `aceita ponto como decimal`() = assertEquals(23_50L, parse("23.50"))

    @Test
    fun `aceita inteiro`() = assertEquals(30_00L, parse("30"))

    @Test
    fun `aceita um digito decimal`() = assertEquals(12_50L, parse("12,5"))

    @Test
    fun `aceita prefixo R$`() = assertEquals(5_00L, parse("R$ 5,00"))

    @Test
    fun `rejeita vazio`() = assertNull(parse(""))

    @Test
    fun `rejeita texto`() = assertNull(parse("abc"))
}
