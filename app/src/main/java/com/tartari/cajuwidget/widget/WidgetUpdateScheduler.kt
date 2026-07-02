package com.tartari.cajuwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

/**
 * Atualiza o widget fora do ciclo mínimo de 30 min do Android:
 * - imediatamente, sempre que um gasto é gravado (listener ou manual);
 * - na virada do dia, para o crédito diário aparecer sem depender de gasto.
 */
object WidgetUpdateScheduler {

    const val ACTION_ATUALIZAR = "com.tartari.cajuwidget.ACTION_ATUALIZAR"

    fun atualizarAgora(context: Context) {
        SaldoWidgetProvider.atualizarTodos(context)
    }

    fun agendarViradaDeDia(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val proximaMeiaNoite = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .plusMinutes(1)
            .toInstant()
            .toEpochMilli()
        val pendente = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SaldoWidgetProvider::class.java).setAction(ACTION_ATUALIZAR),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Inexato de propósito: não exige permissão de alarme exato e
        // alguns minutos de atraso na virada do dia são irrelevantes aqui.
        alarmManager.set(AlarmManager.RTC, proximaMeiaNoite, pendente)
    }
}
