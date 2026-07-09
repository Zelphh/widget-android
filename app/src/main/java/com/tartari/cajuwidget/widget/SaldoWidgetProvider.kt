package com.tartari.cajuwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import com.tartari.cajuwidget.BuildConfig
import com.tartari.cajuwidget.R
import com.tartari.cajuwidget.data.ConfiguracaoGraficoRepository
import com.tartari.cajuwidget.data.GastoRepository
import com.tartari.cajuwidget.data.SaldoTotalRepository
import com.tartari.cajuwidget.domain.Feriados
import com.tartari.cajuwidget.domain.SaldoCalculator
import com.tartari.cajuwidget.ui.LancamentoManualActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

class SaldoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendente = goAsync()
        escopo.launch {
            try {
                appWidgetIds.forEach { atualizarWidget(context, appWidgetManager, it) }
                WidgetUpdateScheduler.agendarViradaDeDia(context)
            } finally {
                pendente.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val pendente = goAsync()
        escopo.launch {
            try {
                atualizarWidget(context, appWidgetManager, appWidgetId)
            } finally {
                pendente.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetUpdateScheduler.ACTION_ATUALIZAR) {
            val pendente = goAsync()
            escopo.launch {
                try {
                    atualizarTodosInterno(context)
                    WidgetUpdateScheduler.agendarViradaDeDia(context)
                } finally {
                    pendente.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Atualização imediata de todas as instâncias do widget (fire-and-forget). */
        fun atualizarTodos(context: Context) {
            escopo.launch { atualizarTodosInterno(context) }
        }

        private suspend fun atualizarTodosInterno(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, SaldoWidgetProvider::class.java))
            ids.forEach { atualizarWidget(context, mgr, it) }
        }

        private suspend fun atualizarWidget(
            context: Context,
            mgr: AppWidgetManager,
            widgetId: Int,
        ) {
            val hoje = LocalDate.now()
            val gastos = GastoRepository.get(context).gastosPorDia(hoje.monthValue, hoje.year)
            val serie = SaldoCalculator.serieDoMes(hoje, gastos, Feriados.datas, BuildConfig.VALOR_DIARIO_CENTAVOS)
            val saldoAtual = serie.last().saldoCentavos

            val totalReal = SaldoTotalRepository.get(context)
                .aplicarCreditosPendentes(hoje, Feriados.datas)

            val (larguraPx, alturaPx) = dimensoesDoGrafico(context, mgr, widgetId)
            val mostrarValorDiario = ConfiguracaoGraficoRepository.get(context).mostrarValorDiario()
            val bitmap = ChartRenderer.render(
                serie,
                larguraPx,
                alturaPx,
                mostrarValorDiario = mostrarValorDiario,
            )

            val views = RemoteViews(context.packageName, R.layout.widget_saldo).apply {
                setImageViewBitmap(R.id.imagem_grafico, bitmap)
                setTextViewText(R.id.texto_saldo_real, formatarReais(totalReal))
                setTextViewText(R.id.texto_saldo, formatarReais(saldoAtual))
                setInt(
                    R.id.texto_saldo, "setTextColor",
                    if (saldoAtual >= 0) 0xFF2E9E5B.toInt() else 0xFFD64545.toInt(),
                )
                setOnClickPendingIntent(R.id.raiz_widget, intentLancamentoManual(context))
            }
            mgr.updateAppWidget(widgetId, views)
        }

        private fun intentLancamentoManual(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, LancamentoManualActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun dimensoesDoGrafico(
            context: Context,
            mgr: AppWidgetManager,
            widgetId: Int,
        ): Pair<Int, Int> {
            val opcoes = mgr.getAppWidgetOptions(widgetId)
            val larguraDp = opcoes.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                .takeIf { it > 0 } ?: 320
            val alturaDp = opcoes.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                .takeIf { it > 0 } ?: 140
            val metrics = context.resources.displayMetrics
            val largura = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, larguraDp.toFloat(), metrics,
            ).toInt().coerceIn(100, 1200)
            // ~40dp reservados para o cabeçalho (título + saldo).
            val altura = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, (alturaDp - 40).toFloat(), metrics,
            ).toInt().coerceIn(60, 800)
            return largura to altura
        }

        fun formatarReais(centavos: Long): String {
            val negativo = centavos < 0
            val abs = if (negativo) -centavos else centavos
            return String.format(
                Locale.forLanguageTag("pt-BR"),
                "%sR$ %d,%02d",
                if (negativo) "-" else "",
                abs / 100,
                abs % 100,
            )
        }
    }
}
