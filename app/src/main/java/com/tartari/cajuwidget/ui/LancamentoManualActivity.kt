package com.tartari.cajuwidget.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import com.tartari.cajuwidget.R
import com.tartari.cajuwidget.data.ConfiguracaoGraficoRepository
import com.tartari.cajuwidget.data.GastoRepository
import com.tartari.cajuwidget.data.Origem
import com.tartari.cajuwidget.widget.WidgetUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Tela leve (estilo diálogo) para lançamento manual de gasto — fallback
 * para quando o parser de notificação falhar. Aberta ao tocar no widget.
 */
class LancamentoManualActivity : Activity() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var dataSelecionada: LocalDate = LocalDate.now()
    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lancamento_manual)

        val campoValor = findViewById<EditText>(R.id.campo_valor)
        val botaoData = findViewById<Button>(R.id.botao_data)
        val botaoSalvar = findViewById<Button>(R.id.botao_salvar)
        val alternadorValorDiario = findViewById<Switch>(R.id.alternador_mostrar_valor_diario)

        atualizarTextoData(botaoData)
        botaoData.setOnClickListener { abrirSeletorData(botaoData) }
        configurarAlternadorValorDiario(alternadorValorDiario)

        botaoSalvar.setOnClickListener {
            val valor = parseValorCentavos(campoValor.text.toString())
            if (valor == null || valor == 0L) {
                campoValor.error = getString(R.string.erro_valor_invalido)
                return@setOnClickListener
            }
            botaoSalvar.isEnabled = false
            escopo.launch {
                withContext(Dispatchers.IO) {
                    GastoRepository.get(applicationContext)
                        .salvarGasto(valor, Origem.MANUAL, dataSelecionada)
                }
                WidgetUpdateScheduler.atualizarAgora(applicationContext)
                Toast.makeText(
                    applicationContext,
                    R.string.gasto_salvo,
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }
        }
    }

    private fun atualizarTextoData(botaoData: Button) {
        botaoData.text = dataSelecionada.format(formatoData)
    }

    /**
     * Carrega o estado salvo antes de conectar o listener, para o ajuste
     * inicial do Switch não disparar uma escrita/refresh espúrios.
     */
    private fun configurarAlternadorValorDiario(alternador: Switch) {
        escopo.launch {
            val ativo = withContext(Dispatchers.IO) {
                ConfiguracaoGraficoRepository.get(applicationContext).mostrarValorDiario()
            }
            alternador.isChecked = ativo
            alternador.setOnCheckedChangeListener { _, novoValor ->
                escopo.launch {
                    withContext(Dispatchers.IO) {
                        ConfiguracaoGraficoRepository.get(applicationContext)
                            .definirMostrarValorDiario(novoValor)
                    }
                    WidgetUpdateScheduler.atualizarAgora(applicationContext)
                }
            }
        }
    }

    private fun abrirSeletorData(botaoData: Button) {
        val limites = limitesData(LocalDate.now())
        DatePickerDialog(
            this,
            { _, ano, mesZeroBased, diaDoMes ->
                dataSelecionada = LocalDate.of(ano, mesZeroBased + 1, diaDoMes)
                atualizarTextoData(botaoData)
            },
            dataSelecionada.year, dataSelecionada.monthValue - 1, dataSelecionada.dayOfMonth,
        ).apply {
            datePicker.minDate = limites.start.paraEpocaMillis()
            datePicker.maxDate = limites.endInclusive.paraEpocaMillis()
        }.show()
    }

    companion object {
        /** Intervalo de datas permitido para backfill: 1º do mês corrente até hoje. */
        fun limitesData(hoje: LocalDate): ClosedRange<LocalDate> = hoje.withDayOfMonth(1)..hoje

        private fun LocalDate.paraEpocaMillis(): Long =
            atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        /**
         * Aceita "12,34", "12.34", "12" → centavos. Aceita sinal negativo
         * ("-12,34") para permitir corrigir manualmente o saldo real do
         * cartão quando ele desalinha do valor real.
         */
        fun parseValorCentavos(entrada: String): Long? {
            val semPrefixo = entrada.trim().removePrefix("R$").trim()
            val negativo = semPrefixo.startsWith("-")
            val normalizado = semPrefixo.removePrefix("-").trim().replace(".", ",")
            if (normalizado.isEmpty()) return null
            val partes = normalizado.split(",")
            val valorAbsoluto = try {
                when (partes.size) {
                    1 -> partes[0].toLong() * 100
                    2 -> partes[0].toLong() * 100 + partes[1].padEnd(2, '0').take(2).toLong()
                    else -> null
                }
            } catch (e: NumberFormatException) {
                null
            } ?: return null
            return if (negativo) -valorAbsoluto else valorAbsoluto
        }
    }
}
