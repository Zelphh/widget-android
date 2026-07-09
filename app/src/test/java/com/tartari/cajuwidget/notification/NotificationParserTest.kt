package com.tartari.cajuwidget.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserTest {

    @Test
    fun `extrai valor de notificacao de compra`() {
        assertEquals(
            23_50L,
            NotificationParser.extrairValorGasto("Compra aprovada de R$ 23,50 no RESTAURANTE X"),
        )
    }

    @Test
    fun `aceita valor sem espaco apos o cifrao`() {
        assertEquals(
            9_90L,
            NotificationParser.extrairValorGasto("Você usou R$9,90 em PADARIA"),
        )
    }

    @Test
    fun `aceita valores com separador de milhar`() {
        assertEquals(
            1_234_56L,
            NotificationParser.extrairValorGasto("Pagamento de R$ 1.234,56 aprovado"),
        )
    }

    @Test
    fun `ignora recarga de beneficio`() {
        assertNull(NotificationParser.extrairValorGasto("Recarga de R$ 660,00 recebida no seu Caju"))
    }

    @Test
    fun `ignora estorno`() {
        assertNull(NotificationParser.extrairValorGasto("Estorno de compra de R$ 23,50"))
    }

    @Test
    fun `ignora texto sem palavra de gasto`() {
        assertNull(NotificationParser.extrairValorGasto("Seu saldo é R$ 120,00"))
    }

    @Test
    fun `ignora texto sem valor`() {
        assertNull(NotificationParser.extrairValorGasto("Compra aprovada"))
    }

    @Test
    fun `usa o valor da compra e ignora o saldo do cartao na mesma notificacao`() {
        val titulo = "Pagamento aprovado"
        val texto = "Compra de R$ 28,40 em Kalzone APROVADA no CRÉDITO, seu saldo em " +
            "AUXÍLIO ALIMENTAÇÃO é R$ 570,95. Use VOUCHER no próximo pagamento."
        assertEquals(
            28_40L,
            NotificationParser.extrairValorGasto("$titulo $texto"),
        )
    }

    @Test
    fun `aceita espaco inquebravel entre o cifrao e o valor`() {
        val texto = "Compra de R$ 28,40 em Kalzone APROVADA no CRÉDITO, seu saldo em " +
            "AUXÍLIO ALIMENTAÇÃO é R$ 570,95."
        assertEquals(
            28_40L,
            NotificationParser.extrairValorGasto(texto),
        )
    }

    @Test
    fun `nao se confunde se o saldo aparecer antes da compra no texto`() {
        val texto = "Seu saldo em AUXÍLIO ALIMENTAÇÃO é R$ 570,95. Compra de R$ 28,40 em Kalzone."
        assertEquals(
            28_40L,
            NotificationParser.extrairValorGasto(texto),
        )
    }
}
