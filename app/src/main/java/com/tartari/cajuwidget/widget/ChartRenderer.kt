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
        totalRealTexto: String? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(larguraPx, alturaPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (serie.isEmpty()) return bitmap

        val densidade = larguraPx / 320f
        val textoAltura = 12f * densidade
        val padding = 4f * densidade
        // Reserva uma faixa no topo para o rótulo do saldo real do cartão,
        // espelhando a faixa já reservada embaixo para os rótulos de dia.
        val bandaTopo = if (totalRealTexto != null) textoAltura + 2f * densidade else 0f
        val areaTopo = padding + bandaTopo
        val areaBase = alturaPx - padding - textoAltura - 2f * densidade
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

        if (totalRealTexto != null) {
            val paintTotal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COR_TEXTO
                textSize = textoAltura
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(totalRealTexto, larguraPx - padding, padding + textoAltura, paintTotal)
        }

        return bitmap
    }
}
