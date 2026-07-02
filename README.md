# Widget Vale Caju

Widget de tela inicial (Android) que mostra, em gráfico de barras, o saldo diário
acumulado do vale-alimentação (R$30/dia útil, crédito do Caju). Projeto pessoal,
sem fins comerciais. Plano completo em `planejamento-widget-vale-caju.md`.

## Como funciona

- **Saldo:** `saldo(dia) = saldo(dia-1) + (30 se dia útil senão 0) - gastos(dia)`.
  Acumula desde o 1º dia do mês, pode ficar negativo; fim de semana/feriado não
  credita, mas o saldo permanece.
- **Gráfico:** barras verdes (positivo) para cima e vermelhas (negativo) para
  baixo, linha do zero fixa. Desenhado via Canvas → Bitmap num `ImageView` do
  `RemoteViews`.
- **Entrada automática:** `CajuNotificationListenerService` escuta notificações
  do app do Caju e registra o gasto direto, sem confirmação.
- **Entrada manual (fallback):** tocar no widget abre um diálogo de lançamento.

## Build e instalação

Requer JDK 17+ (o repositório foi buildado com Temurin 21 em `~/.jdks`) e o
Android SDK apontado em `local.properties`.

```bash
JAVA_HOME=~/.jdks/jdk-21.0.11+10 ./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Depois de instalar:

1. Adicione o widget "Vale Caju" à tela inicial.
2. Conceda acesso a notificações: **Configurações → Notificações → Acesso avançado
   → Acesso a notificações → Vale Caju** (caminho varia por fabricante).

## Decisões tomadas nos pontos abertos do plano (§8)

| Ponto aberto | Decisão nesta versão |
|---|---|
| Texto exato das notificações do Caju | Parser escrito com palavras-chave prováveis (`compra`, `pagamento`, `você usou`…) e regex de valor `R$ N,NN`. **Ajustar com 5–10 amostras reais** — ver `NotificationParser.kt`. Confirmar também o pacote do app em `CajuNotificationListenerService.PACOTES_CAJU` (`adb shell pm list packages | grep -i caju`). |
| Feriados municipais na v1 | Fora da v1: lista começa vazia (`domain/Feriados.kt`), preencher quando quiser. |
| Fim de semana no eixo X | Barra "flat" repetindo o saldo anterior (dias corridos, sem buracos no gráfico). |
| Retenção histórica | Meses passados ficam no banco (nada é apagado na virada de mês). |

## Estrutura

```
app/src/main/java/com/tartari/cajuwidget/
  domain/    SaldoCalculator, Feriados        (lógica pura, testada)
  data/      Gasto, GastoDao, AppDatabase, GastoRepository (Room)
  widget/    SaldoWidgetProvider, ChartRenderer, WidgetUpdateScheduler
  notification/ CajuNotificationListenerService, NotificationParser
  ui/        LancamentoManualActivity
```

## Testes

```bash
JAVA_HOME=~/.jdks/jdk-21.0.11+10 ./gradlew :app:testDebugUnitTest
```

Cobrem a fórmula do saldo (dias úteis, fim de semana flat, feriado, saldo
negativo), o parser de notificação e o parse do valor manual.

## Atualização do widget

- Imediata ao gravar gasto (automático ou manual), via `WidgetUpdateScheduler`.
- Na virada do dia (alarme inexato à meia-noite) para o crédito diário aparecer.
- A cada 30 min pelo ciclo padrão do Android (`updatePeriodMillis`), como rede
  de segurança.
