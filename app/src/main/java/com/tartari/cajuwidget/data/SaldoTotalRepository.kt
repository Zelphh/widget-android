package com.tartari.cajuwidget.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.tartari.cajuwidget.BuildConfig
import com.tartari.cajuwidget.domain.CreditoMensal
import com.tartari.cajuwidget.domain.Feriados
import java.time.LocalDate
import java.time.YearMonth

/**
 * Saldo REAL do cartão Caju (centavos), independente da série de controle de
 * SaldoCalculator. Estado próprio, fora do Room: total corrente + último
 * ano-mês já creditado. Semeado uma única vez a partir de
 * BuildConfig.SALDO_INICIAL_CENTAVOS — nunca sobrescreve estado já existente.
 */
class SaldoTotalRepository private constructor(private val prefs: SharedPreferences) {

    fun totalCentavos(): Long = prefs.getLong(KEY_TOTAL, 0L)

    /** Aplica créditos de todos os meses pendentes (com catch-up) e devolve o total atualizado. */
    @Synchronized
    fun aplicarCreditosPendentes(
        hoje: LocalDate = LocalDate.now(),
        feriados: Set<LocalDate> = Feriados.datas,
        creditoDiarioCentavos: Long = BuildConfig.VALOR_DIARIO_CENTAVOS,
    ): Long {
        semearSeAusente(hoje)
        val ultimo = YearMonth.parse(prefs.getString(KEY_ULTIMO_MES, null))
        val mesAtual = YearMonth.from(hoje)
        val credito = CreditoMensal.creditoEntre(ultimo, mesAtual, feriados, creditoDiarioCentavos)
        if (credito != 0L) {
            prefs.edit {
                putLong(KEY_TOTAL, totalCentavos() + credito)
                putString(KEY_ULTIMO_MES, mesAtual.toString())
            }
        }
        return totalCentavos()
    }

    /** Desconta um gasto do saldo real. Chamado por GastoRepository. */
    @Synchronized
    fun registrarGasto(valorCentavos: Long) {
        semearSeAusente(LocalDate.now())
        prefs.edit { putLong(KEY_TOTAL, totalCentavos() - valorCentavos) }
    }

    /** Semeia uma única vez a partir do valor de build; nunca sobrescreve estado existente. */
    private fun semearSeAusente(hoje: LocalDate) {
        if (prefs.contains(KEY_TOTAL)) return
        prefs.edit {
            putLong(KEY_TOTAL, BuildConfig.SALDO_INICIAL_CENTAVOS)
            // O valor semeado já reflete o saldo real vivo — não recredita o mês corrente.
            putString(KEY_ULTIMO_MES, YearMonth.from(hoje).toString())
        }
    }

    companion object {
        private const val PREFS = "saldo_total"
        private const val KEY_TOTAL = "total_centavos"
        private const val KEY_ULTIMO_MES = "ultimo_mes_creditado"

        @Volatile
        private var instancia: SaldoTotalRepository? = null

        fun get(context: Context): SaldoTotalRepository =
            instancia ?: synchronized(this) {
                instancia ?: SaldoTotalRepository(
                    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
                ).also { instancia = it }
            }
    }
}
