package com.tartari.cajuwidget.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.tartari.cajuwidget.R
import com.tartari.cajuwidget.data.GastoRepository
import com.tartari.cajuwidget.data.Origem
import com.tartari.cajuwidget.widget.WidgetUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tela leve (estilo diálogo) para lançamento manual de gasto — fallback
 * para quando o parser de notificação falhar. Aberta ao tocar no widget.
 */
class LancamentoManualActivity : Activity() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lancamento_manual)

        val campoValor = findViewById<EditText>(R.id.campo_valor)
        val botaoSalvar = findViewById<Button>(R.id.botao_salvar)

        botaoSalvar.setOnClickListener {
            val valor = parseValorCentavos(campoValor.text.toString())
            if (valor == null || valor <= 0) {
                campoValor.error = getString(R.string.erro_valor_invalido)
                return@setOnClickListener
            }
            botaoSalvar.isEnabled = false
            escopo.launch {
                withContext(Dispatchers.IO) {
                    GastoRepository.get(applicationContext)
                        .salvarGasto(valor, Origem.MANUAL)
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

    companion object {
        /** Aceita "12,34", "12.34", "12" → centavos. */
        fun parseValorCentavos(entrada: String): Long? {
            val normalizado = entrada.trim()
                .removePrefix("R$").trim()
                .replace(".", ",")
            if (normalizado.isEmpty()) return null
            val partes = normalizado.split(",")
            return try {
                when (partes.size) {
                    1 -> partes[0].toLong() * 100
                    2 -> partes[0].toLong() * 100 + partes[1].padEnd(2, '0').take(2).toLong()
                    else -> null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}
