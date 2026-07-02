package com.tartari.cajuwidget.domain

import java.time.LocalDate

/**
 * Lista configurável de feriados (sem crédito nesses dias).
 * Começa vazia por decisão do plano — preencher conforme necessário com
 * feriados nacionais + municipais (João Costa/PI).
 *
 * Exemplo:
 *   LocalDate.of(2026, 9, 7),   // Independência
 *   LocalDate.of(2026, 10, 12), // N. Sra. Aparecida
 */
object Feriados {
    val datas: Set<LocalDate> = emptySet()
}
