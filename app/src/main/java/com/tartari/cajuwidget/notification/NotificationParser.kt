package com.tartari.cajuwidget.notification

/**
 * Extrai o valor de uma compra a partir do texto da notificação do Caju.
 *
 * ATENÇÃO (ponto aberto do plano): o texto exato das notificações reais do
 * Caju ainda precisa ser confirmado com 5–10 exemplos capturados. As
 * palavras-chave abaixo são um chute razoável e devem ser ajustadas quando
 * as amostras reais existirem. Se o Caju mudar o texto, o parser falha
 * silenciosamente — risco assumido; o fallback é o lançamento manual.
 */
object NotificationParser {

    // "R$" seguido de espaço comum, espaço fixo (NBSP, U+00A0 — usado por apps
    // financeiros para não quebrar linha entre o cifrão e o valor) ou nada.
    private val REGEX_VALOR = Regex("""R\$[  ]?(\d{1,3}(?:\.\d{3})*|\d+),(\d{2})""")

    // Âncora para o template confirmado "Compra de R$ X,XX ..., seu saldo ... é
    // R$ Y,YY" — evita pegar o saldo do cartão quando ele aparece na mesma
    // mensagem que a compra.
    private val REGEX_VALOR_COMPRA = Regex(
        """compra\s+de\s+R\$[  ]?(\d{1,3}(?:\.\d{3})*|\d+),(\d{2})""",
        RegexOption.IGNORE_CASE,
    )

    private val PALAVRAS_GASTO = listOf(
        "compra", "comprou", "pagamento", "pagou",
        "débito", "debito", "você usou", "voce usou", "utilizou", "gastou",
    )

    private val PALAVRAS_IGNORAR = listOf(
        "recarga", "recebeu", "depósito", "deposito", "estorno",
        "crédito de", "credito de", "creditamos",
    )

    /**
     * Retorna o valor do gasto em centavos, ou null se o texto não parecer
     * uma notificação de compra (ex.: recarga de benefício).
     */
    fun extrairValorGasto(texto: String): Long? {
        val t = texto.lowercase()
        if (PALAVRAS_IGNORAR.any { it in t }) return null
        if (PALAVRAS_GASTO.none { it in t }) return null
        val match = REGEX_VALOR_COMPRA.find(texto) ?: REGEX_VALOR.find(texto) ?: return null
        val reais = match.groupValues[1].replace(".", "").toLong()
        val centavos = match.groupValues[2].toLong()
        return reais * 100 + centavos
    }
}
