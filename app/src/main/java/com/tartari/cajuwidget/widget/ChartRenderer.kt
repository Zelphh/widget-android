package com.tartari.cajuwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.tartari.cajuwidget.domain.SaldoDia
import kotlin.math.max

/**
 * Desenha o gráfico de barras do saldo em um Bitmap (RemoteViews não
 * renderiza view customizada; o widget exibe o bitmap num ImageView).
 *
 * - Linha do zero fixa; barras verdes para cima (saldo positivo),
 *   vermelhas para baixo (negativo).
 * - Eixo X: dias corridos do dia 1º até hoje (fim de semana = barra flat).
 */
object ChartRenderer {

    private const val COR_POSITIVO = 0xFF2E9E5B.toInt()
    private const val COR_NEGATIVO = 0xFFD64545.toInt()
    private const val COR_LINHA_ZERO = 0x66FFFFFF
    private const val COR_TEXTO = 0xB3FFFFFF.toInt()

    fun render(
        serie: List<SaldoDia>,
        larguraPx: Int,
        alturaPx: Int,
        mostrarValorDiario: Boolean = false,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(larguraPx, alturaPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (serie.isEmpty()) return bitmap

        val densidade = larguraPx / 320f
        val textoAltura = 12f * densidade
        val padding = 4f * densidade
        // Quando os rótulos de valor por barra estão ativos, reserva uma faixa
        // extra em cima (para a barra mais alta) e embaixo (para a mais
        // negativa), para o texto não ser cortado nem colidir com os números
        // de dia.
        val bandaValor = if (mostrarValorDiario) textoAltura + 2f * densidade else 0f
        val areaTopo = padding + bandaValor
        val areaBase = alturaPx - padding - textoAltura - 2f * densidade - bandaValor
        val areaAltura = areaBase - areaTopo

        // Escala: espaço acima e abaixo do zero proporcional aos extremos.
        val maxPositivo = max(serie.maxOf { it.saldoCentavos }, 30_00L)
        val maxNegativo = max(-serie.minOf { it.saldoCentavos }, 0L)
        val amplitude = (maxPositivo + maxNegativo).toFloat()
        val zeroY = areaTopo + areaAltura * (maxPositivo / amplitude)

        val paintBarra = Paint(Paint.ANTI_ALIAS_FLAG)
        val paintLinha = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COR_LINHA_ZERO
            strokeWidth = 1.5f * densidade
        }
        val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COR_TEXTO
            textSize = textoAltura
            textAlign = Paint.Align.CENTER
        }

        val n = serie.size
        val passo = larguraPx.toFloat() / n
        val gap = passo * 0.18f
        val raio = 2f * densidade

        serie.forEachIndexed { i, ponto ->
            val esq = i * passo + gap / 2
            val dir = (i + 1) * passo - gap / 2
            val alturaBarra = areaAltura * (ponto.saldoCentavos / amplitude)
            paintBarra.color = if (ponto.saldoCentavos >= 0) COR_POSITIVO else COR_NEGATIVO
            if (alturaBarra >= 0) {
                canvas.drawRoundRect(esq, zeroY - alturaBarra, dir, zeroY, raio, raio, paintBarra)
            } else {
                canvas.drawRoundRect(esq, zeroY, dir, zeroY - alturaBarra, raio, raio, paintBarra)
            }
        }

        canvas.drawLine(0f, zeroY, larguraPx.toFloat(), zeroY, paintLinha)

        if (mostrarValorDiario) {
            val paintValor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COR_TEXTO
                textAlign = Paint.Align.CENTER
            }
            val tamanhoBase = textoAltura.coerceAtMost(passo * 0.62f)
            val tamanhoMinimo = 6f * densidade
            serie.forEachIndexed { i, ponto ->
                val texto = formatarValorAbreviado(ponto.saldoCentavos)
                val tamanho = tamanhoDeFonteParaRotulo(texto, passo, tamanhoBase, tamanhoMinimo)
                if (tamanho <= 0f) return@forEachIndexed
                paintValor.textSize = tamanho
                val alturaBarra = areaAltura * (ponto.saldoCentavos / amplitude)
                val x = i * passo + passo / 2
                val y = if (alturaBarra >= 0) {
                    zeroY - alturaBarra - 2f * densidade
                } else {
                    zeroY - alturaBarra + tamanho + 2f * densidade
                }
                canvas.drawText(texto, x, y, paintValor)
            }
        }

        // Rótulo do dia do mês em todos os pontos do eixo X.
        // Tamanho do texto limitado à largura de cada barra, para não sobrepor
        // os vizinhos quando o mês avança e há mais pontos no gráfico.
        paintTexto.textSize = textoAltura.coerceAtMost(passo * 0.62f)
        val yTexto = alturaPx - padding
        serie.forEachIndexed { i, ponto ->
            val diaDoMes = ponto.data.dayOfMonth
            val x = i * passo + passo / 2
            canvas.drawText(diaDoMes.toString(), x, yTexto, paintTexto)
        }

        return bitmap
    }

    /** Formata o saldo de forma compacta para o rótulo por barra (sem centavos). */
    fun formatarValorAbreviado(centavos: Long): String {
        val negativo = centavos < 0
        val reaisAbs = (if (negativo) -centavos else centavos) / 100
        return if (negativo) "-R$$reaisAbs" else "R$$reaisAbs"
    }

    /**
     * Tamanho de fonte (px) para [texto] caber em uma fatia de [passoPx] de
     * largura, sem Paint.measureText (mantém a função testável sem Robolectric).
     * Retorna 0f quando nem [tamanhoMinimo] cabe — sinal para o chamador pular
     * o rótulo daquela barra em vez de sobrepor os vizinhos.
     */
    fun tamanhoDeFonteParaRotulo(
        texto: String,
        passoPx: Float,
        tamanhoBase: Float,
        tamanhoMinimo: Float,
        fatorLarguraPorCaractere: Float = 0.62f,
    ): Float {
        if (texto.isEmpty() || passoPx <= 0f) return 0f
        val tamanhoQueCabe = passoPx / (texto.length * fatorLarguraPorCaractere)
        val tamanho = minOf(tamanhoBase, tamanhoQueCabe)
        return if (tamanho < tamanhoMinimo) 0f else tamanho
    }
}
