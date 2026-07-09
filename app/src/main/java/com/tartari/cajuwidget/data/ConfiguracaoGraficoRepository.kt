package com.tartari.cajuwidget.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Preferência do usuário para a exibição do gráfico do widget. Estado
 * próprio, fora do Room: hoje guarda só se o valor de cada dia deve
 * aparecer em cima da barra correspondente. Padrão (ausência da chave) é
 * mostrar, para não exigir configuração em instalações já existentes.
 */
class ConfiguracaoGraficoRepository private constructor(private val prefs: SharedPreferences) {

    fun mostrarValorDiario(): Boolean = prefs.getBoolean(KEY_MOSTRAR_VALOR_DIARIO, true)

    fun definirMostrarValorDiario(ativo: Boolean) {
        prefs.edit { putBoolean(KEY_MOSTRAR_VALOR_DIARIO, ativo) }
    }

    companion object {
        private const val PREFS = "configuracao_grafico"
        private const val KEY_MOSTRAR_VALOR_DIARIO = "mostrar_valor_diario"

        @Volatile
        private var instancia: ConfiguracaoGraficoRepository? = null

        fun get(context: Context): ConfiguracaoGraficoRepository =
            instancia ?: synchronized(this) {
                instancia ?: ConfiguracaoGraficoRepository(
                    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
                ).also { instancia = it }
            }
    }
}
