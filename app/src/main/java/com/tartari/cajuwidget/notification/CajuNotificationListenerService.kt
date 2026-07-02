package com.tartari.cajuwidget.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tartari.cajuwidget.data.GastoRepository
import com.tartari.cajuwidget.data.Origem
import com.tartari.cajuwidget.widget.WidgetUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captura notificações do app do Caju e registra o gasto direto,
 * sem diálogo de confirmação (decisão explícita do plano).
 */
class CajuNotificationListenerService : NotificationListenerService() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Dedup simples: a mesma notificação pode ser repostada pelo sistema. */
    private var ultimaChave: String? = null
    private var ultimoValor: Long = -1
    private var ultimoInstante: Long = 0

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in PACOTES_CAJU) return

        val extras = sbn.notification.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val textoGrande = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val completo = "$titulo $texto $textoGrande"

        val valor = NotificationParser.extrairValorGasto(completo)
        if (valor == null) {
            Log.d(TAG, "Notificação do Caju sem valor de gasto reconhecido: $completo")
            return
        }

        val agora = System.currentTimeMillis()
        if (sbn.key == ultimaChave && valor == ultimoValor && agora - ultimoInstante < JANELA_DEDUP_MS) {
            return
        }
        ultimaChave = sbn.key
        ultimoValor = valor
        ultimoInstante = agora

        val contexto = applicationContext
        escopo.launch {
            GastoRepository.get(contexto).salvarGasto(valor, Origem.AUTOMATICO)
            WidgetUpdateScheduler.atualizarAgora(contexto)
        }
    }

    override fun onDestroy() {
        escopo.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CajuListener"
        private const val JANELA_DEDUP_MS = 60_000L

        /**
         * Pacote(s) do app do Caju. Confirmar no aparelho com:
         *   adb shell pm list packages | grep -i caju
         */
        val PACOTES_CAJU = setOf("br.com.caju.cajuapp", "com.caju.beneficios")
    }
}
